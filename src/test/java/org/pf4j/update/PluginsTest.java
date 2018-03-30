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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Decebal Suiu
 */
public class PluginsTest {

    private static final String pluginsJson = "plugins.json";

    public static void main(String[] args) {
        FileWriter writer;
        try {
            writer = new FileWriter("downloads/" + pluginsJson);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<PluginInfo> plugins = new ArrayList<>();

        // plugin 1
        PluginInfo p1 = new PluginInfo();
        p1.id = "welcome-plugin";
        p1.description = "Welcome plugin";
        plugins.add(p1);
        // releases for plugin 1
        PluginInfo.PluginRelease p1r1 = new PluginInfo.PluginRelease();
        p1r1.version = "0.9.0";
        p1r1.date = new Date();
        p1r1.url = "pf4j-demo-plugin1/0.9.0/pf4j-demo-plugin1-0.9.0-SNAPSHOT.zip";
        p1.releases = Collections.singletonList(p1r1);

        // plugin 2
        PluginInfo p2 = new PluginInfo();
        p2.id = "hello-plugin";
        p2.description = "Hello plugin";
        plugins.add(p2);
        // releases for plugin 2
        PluginInfo.PluginRelease p2r1 = new PluginInfo.PluginRelease();
        p2r1.version = "0.9.0";
        p2r1.date = new Date();
        p2r1.url = "pf4j-demo-plugin2/0.9.0/pf4j-demo-plugin2-0.9.0-SNAPSHOT.zip";
        p2.releases = Collections.singletonList(p2r1);

        String json = gson.toJson(plugins);
        try {
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
