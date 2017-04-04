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
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.*;

/**
 * @author Decebal Suiu
 */
public class UpdateRepository {

    private static final Logger log = LoggerFactory.getLogger(UpdateRepository.class);

    private static final String DEFAULT_PLUGINS_JSON = "plugins.json";

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
        Reader pluginsJsonReader;
        try {
            URL pluginsUrl = new URL(new URL(url), DEFAULT_PLUGINS_JSON);
            log.debug("Read plugins of '{}' repository from '{}'", id, pluginsUrl);
            pluginsJsonReader = new InputStreamReader(pluginsUrl.openStream());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            plugins = Collections.emptyList();
            return;
        }

        Gson gson = new GsonBuilder().create();
        PluginInfo[] items = gson.fromJson(pluginsJsonReader, PluginInfo[].class);
        plugins = Arrays.asList(items);
        log.debug("Found {} plugins in repository '{}'", plugins.size(), id);

        // for each release makes the url absolute
        for (PluginInfo plugin : plugins) {
            for (PluginRelease release : plugin.releases) {
                release.url = url + release.url; // add repository's url as prefix to release' url
            }
        }
    }

    /**
     * Causes plugins.json to be read again to look for new updates from repos
     */
    public void refresh() {
        plugins = null;
    }

    public static class PluginInfo implements Serializable {

        public String id;
        public String name;
        public String description;
        public String provider;
        public String projectUrl;
        public List<PluginRelease> releases;

        // Cache lastRelease per system version
        private Map<Version, PluginRelease> lastRelease = new HashMap<>();

        /**
         * Returns the last release version of this plugin for given system version, regardless of release date
         * @param systemVersion version of host system where plugin will be installed
         * @return PluginRelease which has the highest version number
         */
        public PluginRelease getLastRelease(Version systemVersion) {
            if (!lastRelease.containsKey(systemVersion)) {
                for (PluginRelease release : releases) {
                    Expression requires = release.getRequiresExpression();

                    if (systemVersion.equals(Version.forIntegers(0, 0, 0)) || systemVersion.satisfies(requires)) {
                        if (lastRelease.get(systemVersion) == null) {
                            lastRelease.put(systemVersion, release);
                        } else if (release.compareTo(lastRelease.get(systemVersion)) > 0) {
                            lastRelease.put(systemVersion, release);
                        }
                    }
                }
            }

            return lastRelease.get(systemVersion);
        }

        /**
         * Finds whether the repo has a newer version of the plugin
         * @param systemVersion version of host system where plugin will be installed
         * @param installedVersion version that is already installed
         * @return true if there is a newer version available which is compatible with system
         */
        public boolean hasUpdate(Version systemVersion, Version installedVersion) {
            PluginRelease last = getLastRelease(systemVersion);
            return last != null && Version.valueOf(last.version).greaterThan(installedVersion);
        }

    }

    public static class PluginRelease implements Serializable, Comparable<PluginRelease> {

        public String version;
        public Date date;
        public String requires;
        public String url;

        @Override
        public int compareTo(PluginRelease o) {
            return Version.valueOf(version).compareTo(Version.valueOf(o.version));
        }

        /**
         * Get the required version as a SemVer Expression
         * @return Expression object that can be compared to a Version. If requires is empty, a wildcard version is returned
         */
        public Expression getRequiresExpression() {
            return ExpressionParser.newInstance().parse(requires == null ? "*" : requires);
        }
    }

}
