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

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.update.PluginInfo.PluginRelease;
import ro.fortsoft.pf4j.update.util.LenientDateTypeAdapter;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Decebal Suiu
 */
public class DefaultUpdateRepository implements UpdateRepository {

    private static final Logger log = LoggerFactory.getLogger(DefaultUpdateRepository.class);

    private String pluginsJsonFileName = "plugins.json";

    private String id;
    private URL url;
    private Map<String, PluginInfo> plugins;

    public DefaultUpdateRepository(String id, URL url) {
        this.id = id;
        this.url = url;
    }

    public DefaultUpdateRepository(String id, URL url, String pluginsJsonFileName) {
        this(id, url);
        this.pluginsJsonFileName = pluginsJsonFileName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Map<String, PluginInfo> getPlugins() {
        if (plugins == null) {
            initPlugins();
        }

        return plugins;
    }

    @Override
    public PluginInfo getPlugin(String id) {
        return getPlugins().get(id);
    }

    private void initPlugins() {
        Reader pluginsJsonReader;
        try {
            URL pluginsUrl = new URL(getUrl(), pluginsJsonFileName);
            log.debug("Read plugins of '{}' repository from '{}'", id, pluginsUrl);
            pluginsJsonReader = new InputStreamReader(pluginsUrl.openStream());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            plugins = Collections.emptyMap();
            return;
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new LenientDateTypeAdapter()).create();
        PluginInfo[] items = gson.fromJson(pluginsJsonReader, PluginInfo[].class);
        plugins = new HashMap<>(items.length);
        for (PluginInfo p : items) {
            for (PluginRelease r : p.releases) {
                try {
                    r.url = new URL(getUrl(), r.url).toString();
                    if (r.date.getTime() == 0) {
                        log.warn("Illegal release date when parsing {}@{}, setting to epoch", p.id, r.version);
                    }
                } catch (MalformedURLException e) {
                    log.warn("Skipping release {} of plugin {} due to failure to build valid absolute URL. Url was {}{}", r.version, p.id, getUrl(), r.url);
                }
            }
            p.setRepositoryId(getId());
            plugins.put(p.id, p);
        }
        log.debug("Found {} plugins in repository '{}'", plugins.size(), id);
    }

    /**
     * Causes plugins.json to be read again to look for new updates from repos
     */
    @Override
    public void refresh() {
        plugins = null;
    }

    @Override
    public FileDownloader getFileDownloader() {
        return new SimpleFileDownloader();
    }

    public String getPluginsJsonFileName() {
        return pluginsJsonFileName;
    }

    /**
     * Choose another file name than plugins.json
     * @param pluginsJsonFileName the name (relative) of plugins.json file
     */
    public void setPluginsJsonFileName(String pluginsJsonFileName) {
        this.pluginsJsonFileName = pluginsJsonFileName;
    }

}
