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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;

import java.util.List;

/**
 * @author Decebal Suiu
 */
public class UpdateTest {

    private static final Logger log = LoggerFactory.getLogger(UpdateTest.class);

    private static final String repositoriesFile = "repositories.json";

    public static void main(String[] args) {
//        createTestRepositories();
        update();
    }

    private static void update() {
        // start the web server that serves the repository's artifacts
        try {
            new WebServer().start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // create plugin manager
        PluginManager pluginManager = new DefaultPluginManager();
        Version systemVersion = pluginManager.getSystemVersion();
        pluginManager.loadPlugins();

        // create update manager
        UpdateManager updateManager = new UpdateManager(pluginManager);

        // >> keep system up-to-date <<
        boolean systemUpToDate = true;

        // check for updates
        if (updateManager.hasUpdates()) {
            List<UpdateRepository.PluginInfo> updates = updateManager.getUpdates();
            log.debug("Found {} updates", updates.size());
            for (UpdateRepository.PluginInfo plugin : updates) {
                log.debug("Found update for plugin '{}'", plugin.id);
                UpdateRepository.PluginRelease lastRelease = plugin.getLastRelease(systemVersion);
                String lastVersion = lastRelease.version;
                String installedVersion = pluginManager.getPlugin(plugin.id).getDescriptor().getVersion().toString();
                log.debug("Update plugin '{}' from version {} to version {}", plugin.id, installedVersion, lastVersion);
                boolean updated = updateManager.updatePlugin(plugin.id, lastRelease.url);
                if (updated) {
                    log.debug("Updated plugin '{}'", plugin.id);
                } else {
                    log.error("Cannot update plugin '{}'", plugin.id);
                    systemUpToDate = false;
                }
            }
        } else {
            log.debug("No updates found");
        }

        // check for available (new) plugins
        if (updateManager.hasAvailablePlugins()) {
            List<UpdateRepository.PluginInfo> availablePlugins = updateManager.getAvailablePlugins();
            log.debug("Found {} available plugins", availablePlugins.size());
            for (UpdateRepository.PluginInfo plugin : availablePlugins) {
                log.debug("Found available plugin '{}'", plugin.id);
                UpdateRepository.PluginRelease lastRelease = plugin.getLastRelease(systemVersion);
                String lastVersion = lastRelease.version;
                log.debug("Install plugin '{}' with version {}", plugin.id, lastVersion);
                boolean installed = updateManager.installPlugin(lastRelease.url);
                if (installed) {
                    log.debug("Installed plugin '{}'", plugin.id);
                } else {
                    log.error("Cannot install plugin '{}'", plugin.id);
                    systemUpToDate = false;
                }
            }
        } else {
            log.debug("No available plugins found");
        }

        if (systemUpToDate) {
            log.debug("System up-to-date");
        }
    }

}
