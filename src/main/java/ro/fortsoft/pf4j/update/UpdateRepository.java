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

import java.net.URL;
import java.util.Map;

/**
 * Contract for update repositories
 */
public interface UpdateRepository {

    /**
     * @return the ID of this repo. This must be unique
     */
    String getId();

    /**
     * @return the URL of this repo as a String
     */
    URL getUrl();

    /**
     * Get all plugin descriptors for this repo
     * @return Map of PluginId and PluginInfo
     */
    Map<String, PluginInfo> getPlugins();

    /**
     * Get a particular plugin from this repo
     * @param id the id of the plugin
     * @return the PluginInfo
     */
    PluginInfo getPlugin(String id);

    /**
     * Flushes cached info to force re-fetching repository state on next get
     */
    void refresh();

    /**
     * Each repository has the option of overriding the download process.
     * They can e.g. do checksumming, signature verifications etc.
     * To use the default downloader, return null
     * @return the FileDownloader to use for this repository or null if you do not wish to override
     */
    FileDownloader getFileDownloader();

}
