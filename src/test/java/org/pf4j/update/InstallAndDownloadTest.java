/*
 * Copyright (C) 2012-present the original author or authors.
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
package org.pf4j.update;

import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.pf4j.PluginException;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.pf4j.TestPluginDescriptor;
import org.pf4j.VersionManager;
import org.pf4j.update.util.NopPlugin;
import org.pf4j.update.util.PropertiesPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Test downloads etc
 */
public class InstallAndDownloadTest {
    private static final Logger log = LoggerFactory.getLogger(InstallAndDownloadTest.class);

    private Path downloadRepoDir;
    private Path pluginFolderDir;

    private MockZipPlugin p1;
    private MockZipPlugin p2;
    private MockZipPlugin p3;
    private MockZipPlugin p4;
    private MockZipPlugin p5;

    private PluginManager pluginManager;
    private UpdateManager updateManager;
    private VersionManager versionManager;

    private String systemVersion;
    private URL repoUrl;
    private List<PluginWrapper> installed;

    @Before
    public void setup() throws IOException {
        downloadRepoDir = Files.createTempDirectory("pf4j-repo");
        downloadRepoDir.toFile().deleteOnExit();

        pluginFolderDir = Files.createTempDirectory("pf4j-plugins");

        p1 = new MockZipPlugin("myPlugin", "1.2.3", "my-plugin-1.2.3", "my-plugin-1.2.3.zip", "Mar 22, 2017 9:00:35 PM");
        p2 = new MockZipPlugin("myPlugin", "2.0.0", "my-plugin-2.0.0", "my-plugin-2.0.0.ZIP");
        p3 = new MockZipPlugin("other", "3.0.0", "other-3.0.0", "other-3.0.0.Zip");
        p4 = new MockZipPlugin("other", "3.0.1", "other-3.0.1", "other-3.0.1.Zip", "2017-01-31T12:34:56Z");
        p5 = new MockZipPlugin("wrongDate", "4.0.1", "wrong-4.0.1", "wrong-4.0.1.Zip", "wrong");

        pluginManager = new PropertiesPluginManager(pluginFolderDir);
        systemVersion = "1.8.0";
        pluginManager.setSystemVersion(systemVersion); // Only p2 and p3 are valid

        versionManager = pluginManager.getVersionManager();

        repoUrl = new URL("file:" + downloadRepoDir.toAbsolutePath().toString() + "/");
        UpdateRepository local = new DefaultUpdateRepository("local", repoUrl);
        p1.create();
        p2.create();
        p3.create();
        p5.create();

        Path pluginsJson = downloadRepoDir.resolve("plugins.json");
        BufferedWriter writer = Files.newBufferedWriter(pluginsJson, Charset.defaultCharset(), StandardOpenOption.CREATE_NEW);
        String jsonForPlugins = getJsonForPlugins(p1, p2, p3, p4, p5);
        writer.write(jsonForPlugins);
        writer.close();

        updateManager = new UpdateManager(pluginManager, Collections.singletonList(local));
    }

    @Test
    public void findRightVersions() {
        assertEquals(1, updateManager.repositories.size());
        assertEquals(3, updateManager.getPlugins().size());
        assertEquals("2.0.0", updateManager.getPluginsMap().get("myPlugin").getLastRelease(systemVersion, versionManager).version);
        assertEquals("3.0.1", updateManager.getPluginsMap().get("other").getLastRelease(systemVersion, versionManager).version);
    }

    @Test
    public void tolerantDateParsing() throws Exception {
        assertEquals(dateFor("2016-12-31"), updateManager.getPluginsMap().get("myPlugin").getLastRelease(systemVersion, versionManager).date);
        assertTrue(updateManager.getPluginsMap().get("other").getLastRelease(systemVersion, versionManager).date.after(dateFor("2017-01-31")));
        assertTrue(updateManager.getPluginsMap().get("other").getLastRelease(systemVersion, versionManager).date.before(dateFor("2017-02-01")));
        assertEquals(dateFor("1970-01-01"), updateManager.getPluginsMap().get("wrongDate").getLastRelease(systemVersion, versionManager).date);
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
    public void updateVersionNotExist() throws Exception {
        assertTrue(updateManager.installPlugin("myPlugin", "1.2.3"));
        updateManager.updatePlugin("myPlugin", "9.9.9");
    }

    @Test
    public void noUpdateAvailable() throws Exception {
        assertTrue(updateManager.installPlugin("myPlugin", null)); // Install latest
        assertFalse(updateManager.updatePlugin("myPlugin", null)); // Update to latest
    }

    @Test(expected = IllegalArgumentException.class)
    public void uninstallNonExisting() {
        updateManager.uninstallPlugin("other");
    }

    @Test
    public void uninstall() throws Exception {
        updateManager.installPlugin("other", "3.0.0");
        assertTrue(updateManager.uninstallPlugin("other"));
    }

    @Test
    public void repositoryIdIsFilled() {
        for (PluginInfo info : updateManager.getAvailablePlugins()) {
            assertEquals("local", info.getRepositoryId());
        }
    }

    // ****************** MOCK *********************

    private Date dateFor(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.parse(date);
    }

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
        public String dateStr;

        public MockZipPlugin(String id, String version, String filename, String zipname, String dateStr) {
            this.id = id;
            this.version = version;
            this.filenameUnzipped = filename;
            this.zipname = zipname;
            this.dateStr = dateStr;

            updateRepoZipFile = downloadRepoDir.resolve(zipname).toAbsolutePath();
            pluginRepoUnzippedFolder = pluginFolderDir.resolve(filename);
            propsFile = downloadRepoDir.resolve("my.properties");
            fileURI = URI.create("jar:file:"+ updateRepoZipFile.toString());

            descriptor = new TestPluginDescriptor();
            descriptor.setPluginId(id);
            descriptor.setPluginVersion(version);
        }

        public MockZipPlugin(String id, String version, String filename, String zipname) {
            this(id, version, filename, zipname, "2016-12-31");
        }

        public void create() throws IOException {
            try (FileSystem zipfs = FileSystems.newFileSystem(fileURI, Collections.singletonMap("create", "true"))) {
                Path propsInZip = zipfs.getPath("/" + propsFile.getFileName().toString());
                BufferedWriter br = new BufferedWriter(new FileWriter(propsFile.toString()));
                br.write("plugin.id=" + id);
                br.newLine();
                br.write("plugin.version=" + version);
                br.newLine();
                br.write("plugin.class=" + NopPlugin.class.getName());
                br.close();
                Files.move(propsFile, propsInZip);
                zipfs.close();
            }
        }

        public String getSha512() {
            try {
                String checksum = DigestUtils.sha512Hex(Files.newInputStream(updateRepoZipFile));
                log.debug("Generated SHA sum for file: {} ", checksum);
                return checksum;
            } catch (IOException e) {
                return null;
            }
        }
    }

    private String getJsonForPlugins(MockZipPlugin... plugins) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Map<String, List<MockZipPlugin>> pluginMap = new HashMap<>();
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

        List list = new ArrayList<>();
        for (List<MockZipPlugin> l : pluginMap.values()) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", l.get(0).id);
            List<Object> releases = new ArrayList<>();
            for (MockZipPlugin p : l) {
                Map<String, String> releaseInfo = new HashMap<>();
                releaseInfo.put("version", p.version);
                releaseInfo.put("url", p.zipname);
                releaseInfo.put("date", p.dateStr);
                releaseInfo.put("sha512sum", p.getSha512());
                releases.add(releaseInfo);
            }
            info.put("releases", releases);
            list.add(info);
        }

        return gsonBuilder.create().toJson(list);
    }

}