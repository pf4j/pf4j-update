/*
 * Copyright 2014 Decebal Suiu
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Decebal Suiu
 */
public class UpdateRepository {

    private static final Logger log = LoggerFactory.getLogger(UpdateRepository.class);

    private static final String pluginsJson = "plugins.json";

    private String id;
    private String url;
    private List<PluginInfo> plugins;

    public UpdateRepository(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public List<PluginInfo> getPlugins() {
        if (plugins == null) {
            initPlugins();
        }

        return plugins;
    }

    public PluginInfo getPlugin(String id) {
        List<PluginInfo> plugins = getPlugins();
        for (PluginInfo plugin : plugins) {
            if (plugin.id.equals(id)) {
                return plugin;
            }
        }

        return null;
    }

    private void initPlugins() {
        FileReader reader;
        try {
            String pluginsUrl = url + pluginsJson;
            log.debug("Read plugins of '{}' repository from '{}'", id, pluginsUrl);
            File pluginsFile = new FileDownloader().downloadFile(pluginsUrl);
            reader = new FileReader(pluginsFile);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            plugins = Collections.emptyList();
            return;
        }

        Gson gson = new GsonBuilder().create();
        PluginInfo[] items = gson.fromJson(reader, PluginInfo[].class);
        plugins = Arrays.asList(items);
        log.debug("Found {} plugins in repository '{}'", plugins.size(), id);

        // for each release makes the url absolute
        for (PluginInfo plugin : plugins) {
            for (PluginRelease release : plugin.releases) {
                release.url = url + release.url; // add repository's url as prefix to release' url
            }
        }
    }

    public static class PluginInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        public String id;
        public String name;
        public String description;
        public String provider;
        public String projectUrl;
        public List<PluginRelease> releases;

        private boolean readLastRelease;
        private PluginRelease lastRelease;

        public PluginRelease getLastRelease(Version systemVersion) {
            if (!readLastRelease) {
                Date date = new Date(0);
                for (PluginRelease release : releases) {
                    Version requires = Version.forIntegers(0, 0, 0);
                    if ((release.requires != null) && !release.requires.isEmpty()) {
                        requires = Version.valueOf(release.requires);
                    }

                    if (systemVersion.equals(Version.forIntegers(0, 0, 0)) || systemVersion.greaterThanOrEqualTo(requires)) {
                        if (release.date.after(date)) {
                            lastRelease = release;
                            date = release.date;
                        }
                    }
                }

                readLastRelease = true;
            }

            return lastRelease;
        }

        public boolean hasUpdate(Version systemVersion, Version installedVersion) {
            return Version.valueOf(getLastRelease(systemVersion).version).greaterThan(installedVersion);
        }

    }

    public static class PluginRelease implements Serializable, Comparable<PluginRelease> {

        private static final long serialVersionUID = 1L;

        public String version;
        public Date date;
        public String requires;
        public String url;

        @Override
        public int compareTo(PluginRelease o) {
            return Version.valueOf(version).compareTo(Version.valueOf(o.version));
        }

    }

}
