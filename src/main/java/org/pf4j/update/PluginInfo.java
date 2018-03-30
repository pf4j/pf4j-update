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

import org.pf4j.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code PluginInfo} describing a plugin from a repository.
 */
public class PluginInfo implements Serializable, Comparable<PluginInfo> {

    private static final Logger log = LoggerFactory.getLogger(PluginInfo.class);

    public String id;
    public String name;
    public String description;
    public String provider;
    public String projectUrl;
    public List<PluginRelease> releases;

    // This is metadata added at parse time, not part of the published plugins.json
    private String repositoryId;

    // Cache lastRelease per system version
    transient private Map<String, PluginRelease> lastRelease = new HashMap<>();

    /**
     * Returns the last release version of this plugin for given system version, regardless of release date.
     *
     * @param systemVersion version of host system where plugin will be installed
     * @return PluginRelease which has the highest version number
     */
    public PluginRelease getLastRelease(String systemVersion, VersionManager versionManager) {
        if (!lastRelease.containsKey(systemVersion)) {
            for (PluginRelease release : releases) {
                if (systemVersion.equals("0.0.0") || versionManager.checkVersionConstraint(systemVersion, release.requires)) {
                    if (lastRelease.get(systemVersion) == null) {
                        lastRelease.put(systemVersion, release);
                    } else if (versionManager.compareVersions(release.version, lastRelease.get(systemVersion).version) > 0) {
                        lastRelease.put(systemVersion, release);
                    }
                }
            }
        }

        return lastRelease.get(systemVersion);
    }

    /**
     * Finds whether the newer version of the plugin.
     *
     * @param systemVersion version of host system where plugin will be installed
     * @param installedVersion version that is already installed
     * @param versionManager version manager
     * @return true if there is a newer version available which is compatible with system
     */
    public boolean hasUpdate(String systemVersion, String installedVersion, VersionManager versionManager) {
        PluginRelease last = getLastRelease(systemVersion, versionManager);
        return last != null && versionManager.compareVersions(last.version, installedVersion) > 0;

    }

    @Override
    public int compareTo(PluginInfo o) {
        return id.compareTo(o.id);
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public String toString() {
        return "PluginInfo{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", provider='" + provider + '\'' +
            ", projectUrl='" + projectUrl + '\'' +
            ", releases=" + releases +
            ", repositoryId='" + repositoryId + '\'' +
            '}';
    }

    /**
     * A concrete release
     */
    public static class PluginRelease implements Serializable {

        public String version;
        public Date date;
        public String requires;
        public String url;
        /**
         * Optional sha512 digest checksum. Can be one of
         * <ul>
         *   <li>&lt;sha512 sum string&gt;</li>
         *   <li>URL to an external sha512 file</li>
         *   <li>".sha512" as a shortcut for saying download a &lt;filename&gt;.sha512 file next to the zip/jar file</li>
         * </ul>
         */
        public String sha512sum;

        @Override
        public String toString() {
            return "PluginRelease{" +
                "version='" + version + '\'' +
                ", date=" + date +
                ", requires='" + requires + '\'' +
                ", url='" + url + '\'' +
                ", sha512sum='" + sha512sum + '\'' +
                '}';
        }
    }

}
