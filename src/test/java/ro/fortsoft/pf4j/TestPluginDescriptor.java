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
package ro.fortsoft.pf4j;

import com.github.zafarkhaja.semver.Version;

/**
 * PluginDescriptor for testing, with public setters
 */
public class TestPluginDescriptor extends PluginDescriptor {
    @Override
    public void setPluginId(String pluginId) {
        super.setPluginId(pluginId);
    }

    @Override
    public void setPluginDescription(String pluginDescription) {
        super.setPluginDescription(pluginDescription);
    }

    @Override
    public void setPluginClass(String pluginClassName) {
        super.setPluginClass(pluginClassName);
    }

    @Override
    public void setPluginVersion(Version version) {
        super.setPluginVersion(version);
    }

    @Override
    public void setProvider(String provider) {
        super.setProvider(provider);
    }

    @Override
    public void setRequires(String requires) {
        super.setRequires(requires);
    }

    @Override
    public void setDependencies(String dependencies) {
        super.setDependencies(dependencies);
    }

    @Override
    public void setLicense(String license) {
        super.setLicense(license);
    }
}
