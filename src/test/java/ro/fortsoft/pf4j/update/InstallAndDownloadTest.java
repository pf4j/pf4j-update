/*
 * Copyright 2017 Decebal Suiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.fortsoft.pf4j.update;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import ro.fortsoft.pf4j.PluginException;
import ro.fortsoft.pf4j.PluginManager;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.update.util.PropertiesPluginManager;
import ro.fortsoft.pf4j.TestPluginDescriptor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test downloads etc
 */
public class InstallAndDownloadTest {
    private Path downloadRepoDir;
    private Path pluginFolderDir;
    private MockZipPlugin p1;
    private MockZipPlugin p2;
    private MockZipPlugin p3;
    private MockZipPlugin p4;
    private PluginManager pluginManager;
    private UpdateManager updateManager;
    private Version systemVersion;
    private URL repoUrl;
    private List<PluginWrapper> installed;

    @Before
    public void setup() throws IOException {
        downloadRepoDir = Files.createTempDirectory("pf4j-repo");
        downloadRepoDir.toFile().deleteOnExit();
        pluginFolderDir = Files.createTempDirectory("pf4j-plugins");
        p1 = new MockZipPlugin("myPlugin", "1.2.3", "my-plugin-1.2.3", "my-plugin-1.2.3.zip");
        p2 = new MockZipPlugin("myPlugin", "2.0.0", "my-plugin-2.0.0", "my-plugin-2.0.0.ZIP");
        p3 = new MockZipPlugin("other", "3.0.0", "other-3.0.0", "other-3.0.0.Zip");
        p4 = new MockZipPlugin("other", "3.0.1", "other-3.0.1", "other-3.0.1.Zip");
        pluginManager = new PropertiesPluginManager(pluginFolderDir);
        systemVersion = Version.forIntegers(1,8);
        pluginManager.setSystemVersion(systemVersion); // Only p2 and p3 are valid
        Path pluginsjson = downloadRepoDir.resolve("plugins.json");
        BufferedWriter writer = Files.newBufferedWriter(pluginsjson, Charset.defaultCharset(), StandardOpenOption.CREATE_NEW);
        String jsonForPlugins = getJsonForPlugins(p1,p2,p3,p4);
        writer.write(jsonForPlugins);
        writer.close();
        repoUrl = new URL("file:" + downloadRepoDir.toAbsolutePath().toString() + "/");
        UpdateRepository local = new DefaultUpdateRepository("local", repoUrl);
        p1.create();
        p2.create();
        p3.create();
        updateManager = new UpdateManager(pluginManager, Arrays.asList(local));
    }

    @Test
    public void findRightVersions() throws Exception {
        assertEquals(1, updateManager.repositories.size());
        assertEquals(2, updateManager.getPlugins().size());
        assertEquals("2.0.0", updateManager.getPlugins().get(0).getLastRelease(systemVersion).version);
        assertEquals("3.0.1", updateManager.getPlugins().get(1).getLastRelease(systemVersion).version);
    }

    @Test
    public void install() throws Exception {
        assertFalse(Files.exists(pluginFolderDir.resolve(p3.zipname)));
        assertTrue(updateManager.installPlugin("other", "3.0.0"));
        assertTrue(Files.exists(pluginFolderDir.resolve(p3.zipname)));
    }

    @Test
    public void installOldVersion() throws Exception {
        assertTrue(updateManager.installPlugin("myPlugin", "1.2.3"));
    }

    @Test
    public void update() throws Exception {
        assertTrue(updateManager.installPlugin("myPlugin", "1.2.3"));
        assertTrue(updateManager.hasUpdates());
        assertEquals(1, updateManager.getUpdates().size());
        assertTrue(updateManager.updatePlugin("myPlugin", null)); // latest release
        assertTrue(Files.exists(pluginFolderDir.resolve(p2.zipname)));
        assertTrue(Files.exists(pluginFolderDir.resolve(p2.pluginRepoUnzippedFolder)));
        assertFalse(Files.exists(pluginFolderDir.resolve(p1.zipname)));
        assertFalse(Files.exists(pluginFolderDir.resolve(p1.pluginRepoUnzippedFolder)));
    }

    @Test(expected = PluginException.class)
    public void updateVersionNotexist() throws Exception {
        assertTrue(updateManager.installPlugin("myPlugin", "1.2.3"));
        updateManager.updatePlugin("myPlugin", "9.9.9");
    }

    @Test
    public void noUpdateAvailable() throws Exception {
        assertTrue(updateManager.installPlugin("myPlugin", null)); // Install latest
        assertFalse(updateManager.updatePlugin("myPlugin", null)); // Update to latest
    }

    @Test(expected = IllegalArgumentException.class)
    public void uninstallNonexisting() throws Exception {
        updateManager.uninstallPlugin("other");
    }

    @Test
    public void uninstall() throws Exception {
        updateManager.installPlugin("other", "3.0.0");
        assertTrue(updateManager.uninstallPlugin("other"));
    }

    @Test
    public void repositoryIdIsFilled() throws Exception {
        for (PluginInfo info : updateManager.getAvailablePlugins()) {
            assertEquals("local", info.getRepositoryId());
        }
    }

    // ****************** MOCK *********************

    private class MockZipPlugin {
        public final String id;
        public final String version;
        public final String filenameUnzipped;
        public final Path updateRepoZipFile;
        public final Path pluginRepoUnzippedFolder;
        private final Path propsFile;
        private final URI fileURI;
        public String zipname;
        public TestPluginDescriptor descriptor;

        public MockZipPlugin(String id, String version, String filename, String zipname) throws IOException {
            this.id = id;
            this.version = version;
            this.filenameUnzipped = filename;
            this.zipname = zipname;

            updateRepoZipFile = downloadRepoDir.resolve(zipname).toAbsolutePath();
            pluginRepoUnzippedFolder = pluginFolderDir.resolve(filename);
            propsFile = downloadRepoDir.resolve("my.properties");
            fileURI = URI.create("jar:file:"+ updateRepoZipFile.toString());

            descriptor = new TestPluginDescriptor();
            descriptor.setPluginId(id);
            descriptor.setPluginVersion(Version.valueOf(version));
        }

        public void create() throws IOException {
            try (FileSystem zipfs = FileSystems.newFileSystem(fileURI, Collections.singletonMap("create", "true"))) {
                Path propsInZip = zipfs.getPath("/" + propsFile.getFileName().toString());
                BufferedWriter br = new BufferedWriter(new FileWriter(propsFile.toString()));
                br.write("plugin.id=" + id);
                br.newLine();
                br.write("plugin.version=" + version);
                br.newLine();
                br.write("plugin.class=ro.fortsoft.pf4j.update.util.NopPlugin");
                br.close();
                Files.move(propsFile, propsInZip);
            }
        }
    }

    private String getJsonForPlugins(MockZipPlugin... plugins) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Map<String,List<MockZipPlugin>> pluginMap = new HashMap<>();
        for (MockZipPlugin p : plugins) {
            if (pluginMap.containsKey(p.id)) {
                List<MockZipPlugin> l = pluginMap.get(p.id);
                l.add(p);
            } else {
                List<MockZipPlugin> l = new ArrayList<>();
                l.add(p);
                pluginMap.put(p.id, l);
            }
        }
        ArrayList list = new ArrayList<>();
        for (List<MockZipPlugin> l : pluginMap.values()) {
            Map<String,Object> info = new HashMap<>();
            info.put("id", l.get(0).id);
            List<Object> releases = new ArrayList<>();
            for (MockZipPlugin p : l) {
                Map<String, String> releaseInfo = new HashMap<>();
                releaseInfo.put("version", p.version);
                releaseInfo.put("url", p.zipname);
                releases.add(releaseInfo);
            }
            info.put("releases", releases);
            list.add(info);
        }
        return gsonBuilder.create().toJson(list);
    }
}