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
import ro.fortsoft.pf4j.*;
import ro.fortsoft.pf4j.update.PluginInfo.PluginRelease;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
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
        for (PluginInfo plugin : getPlugins()) {
            if (pluginManager.getPlugin(plugin.id) == null) {
                availablePlugins.add(plugin);
            }
        }

        return availablePlugins;
    }

    public boolean hasAvailablePlugins() {
        for (PluginInfo plugin : getPlugins()) {
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
        List<PluginInfo> list = new ArrayList<>(getPluginsMap().values());
        Collections.sort(list);
        return list;
    }

    /**
     * Get a map of all plugins from all repos where key is plugin id
     * @return List of plugin info
     */
    public Map<String, PluginInfo> getPluginsMap() {
        Map<String, PluginInfo> pluginsMap = new HashMap<>();
        for (UpdateRepository repository : getRepositories()) {
            pluginsMap.putAll(repository.getPlugins());
        }

        return pluginsMap;
    }

    public List<UpdateRepository> getRepositories() {
        if (repositories == null && repositoriesJson != null) {
            refresh();
        }

        return repositories;
    }


    /**
     * Replace all repositories
     * @param repositories list of new repositories
     */
    public void setRepositories(List<UpdateRepository> repositories) {
        this.repositories = repositories;
        refresh();
    }

    /**
     * Add one DefaultUpdateRepository
     * @param id of repo
     * @param url of repo
     */
    public void addRepository(String id, String url) {
        for (UpdateRepository ur : repositories) {
            if (ur.getId().equals(id)) {
                throw new RuntimeException("Repository with id " + id + " already exists");
            }
        }
        repositories.add(new DefaultUpdateRepository(id, url));
    }

    /**
     * Add a repo that was created by client
     * @param newRepo
     */
    public void addRepository(UpdateRepository newRepo) {
        for (UpdateRepository ur : repositories) {
            if (ur.getId().equals(newRepo.getId())) {
                throw new RuntimeException("Repository with id " + newRepo.getId() + " already exists");
            }
        }
        newRepo.refresh();
        repositories.add(newRepo);
    }

    /**
     * Remove a repository by id
     * @param id of repository to remove
     */
    public void removeRepository(String id) {
      for (UpdateRepository repo : getRepositories()) {
        if (id == repo.getId()) {
          repositories.remove(repo);
          break;
        }
      }
      log.warn("Repository with id " + id + " not found, doing nothing");
    }

    /**
     * Refreshes all repositories, so they are forced to refresh list of plugins
     */
    public synchronized void refresh() {
        if (repositoriesJson != null) {
            initRepositoriesFromJson();
        }
        for (UpdateRepository updateRepository : repositories) {
            updateRepository.refresh();
        }
    }

    /**
     * Installs a plugin given only its URL
     * @param url the url of the plugin to install
     * @return true if plugin was installed
     * @deprecated - use the installPlugin(id, version) instead
     */
    public boolean installPlugin(String url) {
        FileDownloader downloader = new SimpleFileDownloader();
        try {
            Path downloaded = downloader.downloadFile(new URL(url));

            Path pluginsRoot = pluginManager.getPluginsRoot();
            Path file = pluginsRoot.resolve(downloaded.getFileName());
            Files.move(downloaded, file);

            String newPluginId = pluginManager.loadPlugin(file);
            PluginState state = pluginManager.startPlugin(newPluginId);

            return PluginState.STARTED.equals(state);
        } catch (Exception e) {
            log.error("Plugin at " + url + " not installed.", e);
        }
        return false;
    }

    /**
     * Installs a plugin by URL
     * @param id the id of plugin to install
     * @param version the version of plugin to install, on SemVer format
     * @return true if installation successful and plugin started
     */
    public synchronized boolean installPlugin(String id, String version) {
        Path downloaded = null;
        try {
            // Download to temporary location
            downloaded = downloadPlugin(id, version);

            Path pluginsRoot = pluginManager.getPluginsRoot();
            Path file = pluginsRoot.resolve(downloaded.getFileName());
            Files.move(downloaded, file);

            String pluginId = pluginManager.loadPlugin(file);
            PluginState state = pluginManager.startPlugin(pluginId);

            return PluginState.STARTED.equals(state);
        } catch (Exception e) {
            log.error("Plugin " + id + "@" + version + " not installed.", e);
        }
        return false;
    }

    /**
     * Downloads a plugin with given coordinates and returns a path to the file
     * @param id of plugin
     * @param version of plugin
     * @return Path to file which will reside in a temporary folder in the system default temp area
     * @throws PluginException if download failed
     */
    protected Path downloadPlugin(String id, String version) throws PluginException {
        try {
            URL url = findUrlForPlugin(id, version);
            return getFileDownloader(id).downloadFile(url);
        } catch (IOException e) {
            throw new PluginException("Error during download of plugin " + id, e);
        }
    }

    /**
     * Finds the FileDownloader to use for this repository
     * @param pluginId the plugin we wish to download
     * @return FileDownloader instance
     */
    protected FileDownloader getFileDownloader(String pluginId) {
        for (UpdateRepository ur : repositories) {
            if (ur.getPlugin(pluginId) != null && ur.getFileDownloader() != null) {
                return ur.getFileDownloader();
            }
        }
        return new SimpleFileDownloader();
    }

    /**
     * Resolves url from id and version
     * @param id of plugin
     * @param version of plugin
     * @return URL for downloading
     * @throws PluginException if id or version does not exist
     */
    protected URL findUrlForPlugin(String id, String version) throws PluginException {
        PluginInfo plugin = getPluginsMap().get(id);
        if (plugin == null) {
            log.info("Plugin with id {} does not exist in any repository", id);
            throw new PluginException("Plugin with id " + id + " not found in any repository");
        }
        for (PluginRelease release : plugin.releases) {
            if (Version.valueOf(version).equals(Version.valueOf(release.version)) && release.url != null) {
                try {
                    return new URL(release.url);
                } catch (MalformedURLException e) {
                    throw new PluginException("Release URL " + release.url + " is not a valid URL", e);
                }
            }
        }
        throw new PluginException("Plugin " + id + " with version @" + version + " does not exist in the repository");
    }

    public boolean updatePlugin(String id, String url) {
        try {
            // Download to temp folder
            Path downloaded = getFileDownloader(id).downloadFile(new URL(url));

            if (!pluginManager.deletePlugin(id)) {
                return false;
            }

            Path pluginsRoot = pluginManager.getPluginsRoot();
            Path file = pluginsRoot.resolve(downloaded.getFileName());
            Files.move(downloaded, file);

            String newPluginId = pluginManager.loadPlugin(file);
            PluginState state = pluginManager.startPlugin(newPluginId);

            return PluginState.STARTED.equals(state);

        } catch (Exception e) {
            log.error("Error while updating plugin " + id, e);
        }
        return false;
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
        UpdateRepository[] items = gson.fromJson(reader, DefaultUpdateRepository[].class);

        repositories = Arrays.asList(items);
    }

    private Version getSystemVersion() {
        return pluginManager.getSystemVersion();
    }

}
