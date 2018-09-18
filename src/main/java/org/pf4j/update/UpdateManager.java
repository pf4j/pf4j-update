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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.pf4j.PluginException;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.VersionManager;
import org.pf4j.update.PluginInfo.PluginRelease;
import org.pf4j.update.verifier.CompoundVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Decebal Suiu
 */
public class UpdateManager {

    private static final Logger log = LoggerFactory.getLogger(UpdateManager.class);

    private PluginManager pluginManager;
    private VersionManager versionManager;
    private String systemVersion;
    private Path repositoriesJson;

    // cache last plugin release per plugin id (the key)
    private Map<String, PluginRelease> lastPluginRelease = new HashMap<>();

    protected List<UpdateRepository> repositories;

    public UpdateManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;

        versionManager = pluginManager.getVersionManager();
        systemVersion = pluginManager.getSystemVersion();
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
     * Return a list of plugins that are newer versions of already installed plugins.
     *
     * @return list of plugins that have updates
     */
    public List<PluginInfo> getUpdates() {
        List<PluginInfo> updates = new ArrayList<>();
        for (PluginWrapper installed : pluginManager.getPlugins()) {
            String pluginId = installed.getPluginId();
            if (hasPluginUpdate(pluginId)) {
                updates.add(getPluginsMap().get(pluginId));
            }
        }

        return updates;
    }

    /**
     * Checks if Update Repositories has newer versions of some of the installed plugins.
     *
     * @return true if updates exist
     */
    public boolean hasUpdates() {
        return getUpdates().size() > 0;
    }

    /**
     * Get the list of plugins from all repos.
     *
     * @return List of plugin info
     */
    public List<PluginInfo> getPlugins() {
        List<PluginInfo> list = new ArrayList<>(getPluginsMap().values());
        Collections.sort(list);

        return list;
    }

    /**
     * Get a map of all plugins from all repos where key is plugin id.
     *
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
     * Replace all repositories.
     *
     * @param repositories list of new repositories
     */
    public void setRepositories(List<UpdateRepository> repositories) {
        this.repositories = repositories;
        refresh();
    }

    /**
     * Add one {@link DefaultUpdateRepository).
     *
     * @param id of repo
     * @param url of repo
     */
    public void addRepository(String id, URL url) {
        for (UpdateRepository ur : repositories) {
            if (ur.getId().equals(id)) {
                throw new RuntimeException("Repository with id " + id + " already exists");
            }
        }
        repositories.add(new DefaultUpdateRepository(id, url));
    }

    /**
     * Add a repo that was created by client.
     *
     * @param newRepo the new UpdateRepository to add to the list
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
     * Remove a repository by id.
     *
     * @param id of repository to remove
     */
    public void removeRepository(String id) {
      for (UpdateRepository repo : getRepositories()) {
        if (id.equals(repo.getId())) {
          repositories.remove(repo);
          break;
        }
      }
      log.warn("Repository with id " + id + " not found, doing nothing");
    }

    /**
     * Refreshes all repositories, so they are forced to refresh list of plugins.
     */
    public synchronized void refresh() {
        if (repositoriesJson != null) {
            initRepositoriesFromJson();
        }
        for (UpdateRepository updateRepository : repositories) {
            updateRepository.refresh();
        }
        lastPluginRelease.clear();
    }

    /**
     * Installs a plugin by id and version.
     *
     * @param id the id of plugin to install
     * @param version the version of plugin to install, on SemVer format, or null for latest
     * @return true if installation successful and plugin started
     * @exception PluginException if plugin does not exist in repos or problems during
     */
    public synchronized boolean installPlugin(String id, String version) throws PluginException {
        // Download to temporary location
        Path downloaded = downloadPlugin(id, version);

        Path pluginsRoot = pluginManager.getPluginsRoot();
        Path file = pluginsRoot.resolve(downloaded.getFileName());
        try {
            Files.move(downloaded, file);
        } catch (IOException e) {
            throw new PluginException(e, "Failed to write file '{}' to plugins folder", file);
        }

        String pluginId = pluginManager.loadPlugin(file);
        PluginState state = pluginManager.startPlugin(pluginId);

        return PluginState.STARTED.equals(state);
    }

    /**
     * Downloads a plugin with given coordinates, runs all {@link FileVerifier}s
     * and returns a path to the downloaded file.
     *
     * @param id of plugin
     * @param version of plugin or null to download latest
     * @return Path to file which will reside in a temporary folder in the system default temp area
     * @throws PluginException if download failed
     */
    protected Path downloadPlugin(String id, String version) throws PluginException {
        try {
            PluginRelease release = findReleaseForPlugin(id, version);
            Path downloaded = getFileDownloader(id).downloadFile(new URL(release.url));
            getFileVerifier(id).verify(new FileVerifier.Context(id, release), downloaded);
            return downloaded;
        } catch (IOException e) {
            throw new PluginException(e, "Error during download of plugin {}", id);
        }
    }

    /**
     * Finds the {@link FileDownloader} to use for this repository.
     *
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
     * Gets a file verifier to use for this plugin. First tries to use custom verifier
     * configured for the repository, then fallback to the default CompoundVerifier
     *
     * @param pluginId the plugin we wish to download
     * @return FileVerifier instance
     */
    protected FileVerifier getFileVerifier(String pluginId) {
        for (UpdateRepository ur : repositories) {
            if (ur.getPlugin(pluginId) != null && ur.getFileVerfier() != null) {
                return ur.getFileVerfier();
            }
        }

        return new CompoundVerifier();
    }

    /**
     * Resolves Release from id and version.
     *
     * @param id of plugin
     * @param version of plugin or null to locate latest version
     * @return PluginRelease for downloading
     * @throws PluginException if id or version does not exist
     */
    protected PluginRelease findReleaseForPlugin(String id, String version) throws PluginException {
        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            log.info("Plugin with id {} does not exist in any repository", id);
            throw new PluginException("Plugin with id {} not found in any repository", id);
        }

        if (version == null) {
            return getLastPluginRelease(id);
        }

        for (PluginRelease release : pluginInfo.releases) {
            if (versionManager.compareVersions(version, release.version) == 0 && release.url != null) {
                return release;
            }
        }

        throw new PluginException("Plugin {} with version @{} does not exist in the repository", id, version);
    }

    /**
     * Updates a plugin id to given version or to latest version if {@code version == null}.
     *
     * @param id the id of plugin to update
     * @param version the version to update to, on SemVer format, or null for latest
     * @return true if update successful
     * @exception PluginException in case the given version is not available, plugin id not already installed etc
    */
    public boolean updatePlugin(String id, String version) throws PluginException {
        if (pluginManager.getPlugin(id) == null) {
            throw new PluginException("Plugin {} cannot be updated since it is not installed", id);
        }

        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            throw new PluginException("Plugin {} does not exist in any repository", id);
        }

        if (!hasPluginUpdate(id)) {
            log.warn("Plugin {} does not have an update available which is compatible with system version", id, systemVersion);
            return false;
        }

        // Download to temp folder
        Path downloaded = downloadPlugin(id, version);

        if (!pluginManager.deletePlugin(id)) {
            return false;
        }

        Path pluginsRoot = pluginManager.getPluginsRoot();
        Path file = pluginsRoot.resolve(downloaded.getFileName());
        try {
            Files.move(downloaded, file);
        } catch (IOException e) {
            throw new PluginException("Failed to write plugin file {} to plugin folder", file);
        }

        String newPluginId = pluginManager.loadPlugin(file);
        PluginState state = pluginManager.startPlugin(newPluginId);

        return PluginState.STARTED.equals(state);
    }

    public boolean uninstallPlugin(String id) {
        return pluginManager.deletePlugin(id);
    }

    /**
     * Returns the last release version of this plugin for given system version, regardless of release date.
     *
     * @return PluginRelease which has the highest version number
     */
    public PluginRelease getLastPluginRelease(String id) {
        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            return null;
        }

        if (!lastPluginRelease.containsKey(id)) {
            for (PluginRelease release : pluginInfo.releases) {
                if (systemVersion.equals("0.0.0") || versionManager.checkVersionConstraint(systemVersion, release.requires)) {
                    if (lastPluginRelease.get(id) == null) {
                        lastPluginRelease.put(id, release);
                    } else if (versionManager.compareVersions(release.version, lastPluginRelease.get(id).version) > 0) {
                        lastPluginRelease.put(id, release);
                    }
                }
            }
        }

        return lastPluginRelease.get(id);
    }

    /**
     * Finds whether the newer version of the plugin.
     *
     * @return true if there is a newer version available which is compatible with system
     */
    public boolean hasPluginUpdate(String id) {
        PluginInfo pluginInfo = getPluginsMap().get(id);
        if (pluginInfo == null) {
            return false;
        }

        String installedVersion = pluginManager.getPlugin(id).getDescriptor().getVersion();
        PluginRelease last = getLastPluginRelease(id);

        return last != null && versionManager.compareVersions(last.version, installedVersion) > 0;
    }

    protected synchronized void initRepositoriesFromJson() {
        log.debug("Read repositories from '{}'", repositoriesJson);
        try (FileReader reader = new FileReader(repositoriesJson.toFile())) {
            Gson gson = new GsonBuilder().create();
            UpdateRepository[] items = gson.fromJson(reader, DefaultUpdateRepository[].class);
            repositories = Arrays.asList(items);
        } catch (IOException e) {
            e.printStackTrace();
            repositories = Collections.emptyList();
        }
    }

}
