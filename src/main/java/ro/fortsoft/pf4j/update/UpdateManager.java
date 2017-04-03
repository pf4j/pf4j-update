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
import ro.fortsoft.pf4j.PluginManager;
import ro.fortsoft.pf4j.PluginState;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.update.UpdateRepository.PluginInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Decebal Suiu
 */
public class UpdateManager {

    private static final Logger log = LoggerFactory.getLogger(UpdateManager.class);

    private Path repositoriesJson;

    protected List<UpdateRepository> repositories;

    public PluginManager pluginManager;

    public UpdateManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        repositoriesJson = Paths.get("repositories.json");
    }

    public UpdateManager(PluginManager pluginManager, Path repositoriesJson) {
        this(pluginManager);
        this.repositoriesJson = repositoriesJson;
    }

    public UpdateManager(PluginManager pluginManager, List<UpdateRepository> repos) {
        this(pluginManager);
        repositories = repos == null ? new ArrayList<UpdateRepository>() : repos;
    }

    public List<PluginInfo> getAvailablePlugins() {
        List<PluginInfo> availablePlugins = new ArrayList<>();
        List<PluginInfo> plugins = getPlugins();
        for (PluginInfo plugin : plugins) {
            if (pluginManager.getPlugin(plugin.id) == null) {
                availablePlugins.add(plugin);
            }
        }

        return availablePlugins;
    }

    public boolean hasAvailablePlugins() {
        List<PluginInfo> plugins = getPlugins();
        for (PluginInfo plugin : plugins) {
            if (pluginManager.getPlugin(plugin.id) == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return a list of plugins that are newer versions of already installed plugins
     * @return
     */
    public List<PluginInfo> getUpdates() {
        List<PluginInfo> updates = new ArrayList<>();
        Map<String, PluginInfo> pluginMap = getPluginsMap();
        for (PluginWrapper installed : pluginManager.getPlugins()) {
            PluginInfo pluginFromRepo = pluginMap.get(installed.getPluginId());
            if (pluginFromRepo != null) {
                Version installedVersion = installed.getDescriptor().getVersion();
                if (pluginFromRepo.hasUpdate(getSystemVersion(), installedVersion)) {
                    updates.add(pluginFromRepo);
                }
            }
        }

        return updates;
    }

    /**
     * Checks if Update Repositories has newer versions of some of the installed plugins
     * @return true if updates exist
     */
    public boolean hasUpdates() {
        return getUpdates().size() > 0;
    }

    /**
     * Get the list of plugins from all repos
     * @return List of plugin info
     */
    public List<PluginInfo> getPlugins() {
        List<PluginInfo> plugins = new ArrayList<>();
        List<UpdateRepository> repositories = getRepositories();
        for (UpdateRepository repository : repositories) {
            plugins.addAll(repository.getPlugins());
        }

        return plugins;
    }

    /**
     * Get a map of all plugins from all repos where key is plugin id
     * @return List of plugin info
     */
    public Map<String, PluginInfo> getPluginsMap() {
        Map<String, PluginInfo> pluginsMap = new HashMap<>();
        for (PluginInfo plugin : getPlugins()) {
            pluginsMap.put(plugin.id, plugin);
        }
        return pluginsMap;
    }

    public List<UpdateRepository> getRepositories() {
        if (repositories == null && repositoriesJson != null) {
            refresh();
        }

        return repositories;
    }

    public void setRepositories(List<UpdateRepository> repositories) {
        this.repositories = repositories;
        refresh();
    }


    public synchronized void refresh() {
        if (repositoriesJson != null) {
            initRepositoriesFromJson();
        }
        for (UpdateRepository updateRepository : repositories) {
            updateRepository.refresh();
        }
    }

    public synchronized boolean installPlugin(String url) {
        File pluginArchiveFile;
        try {
            pluginArchiveFile = new FileDownloader().downloadFile(url);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }

        String pluginId = pluginManager.loadPlugin(pluginArchiveFile.toPath());
        PluginState state = pluginManager.startPlugin(pluginId);

        return PluginState.STARTED.equals(state);
    }

    public boolean updatePlugin(String id, String url) {
        File pluginArchiveFile;
        try {
            pluginArchiveFile = new FileDownloader().downloadFile(url);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }

        if (!pluginManager.deletePlugin(id)) {
            return false;
        }

        String newPluginId = pluginManager.loadPlugin(pluginArchiveFile.toPath());
        PluginState state = pluginManager.startPlugin(newPluginId);

        return PluginState.STARTED.equals(state);
    }

    public boolean uninstallPlugin(String id) {
        return pluginManager.deletePlugin(id);
    }

    protected synchronized void initRepositoriesFromJson() {
        FileReader reader;
        try {
            log.debug("Read repositories from '{}'", repositoriesJson);
            reader = new FileReader(repositoriesJson.toFile());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            repositories = Collections.emptyList();
            return;
        }

        Gson gson = new GsonBuilder().create();
        UpdateRepository[] items = gson.fromJson(reader, UpdateRepository[].class);

        repositories = Arrays.asList(items);
    }

    private Version getSystemVersion() {
        return pluginManager.getSystemVersion();
    }

}
