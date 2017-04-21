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

import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PluginInfo describing a plugin from a repo
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
     * Finds whether the  newer version of the plugin
     * @param systemVersion version of host system where plugin will be installed
     * @param installedVersion version that is already installed
     * @return true if there is a newer version available which is compatible with system
     */
    public boolean hasUpdate(Version systemVersion, Version installedVersion) {
        PluginRelease last = getLastRelease(systemVersion);
        return last != null && Version.valueOf(last.version).greaterThan(installedVersion);
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
            try {
                return ExpressionParser.newInstance().parse(requires == null ? "*" : requires);
            } catch (Exception e) {
                log.warn("Failed to parse 'requires' expression {} for plugin {}. Allowing all versions", requires, url, e);
                return ExpressionParser.newInstance().parse("*");
            }
        }

    }

}
