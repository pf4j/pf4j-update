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
package org.pf4j;

/**
 * PluginDescriptor for testing, with public setters.
 */
public class TestPluginDescriptor extends DefaultPluginDescriptor {

    @Override
    public DefaultPluginDescriptor setPluginId(String pluginId) {
        return super.setPluginId(pluginId);
    }

    @Override
    public PluginDescriptor setPluginDescription(String pluginDescription) {
        return super.setPluginDescription(pluginDescription);
    }

    @Override
    public PluginDescriptor setPluginClass(String pluginClassName) {
        return super.setPluginClass(pluginClassName);
    }

    @Override
    public DefaultPluginDescriptor setPluginVersion(String version) {
        return super.setPluginVersion(version);
    }

    @Override
    public PluginDescriptor setProvider(String provider) {
        return super.setProvider(provider);
    }

    @Override
    public PluginDescriptor setRequires(String requires) {
        return super.setRequires(requires);
    }

    @Override
    public PluginDescriptor setDependencies(String dependencies) {
        return super.setDependencies(dependencies);
    }

}
