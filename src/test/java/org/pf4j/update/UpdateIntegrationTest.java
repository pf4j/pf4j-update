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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pf4j.update.util.TestApplication;
import org.pf4j.update.util.TestPluginsFixture;

import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UpdateIntegrationTest {

    private WebServer webServer;

    @Before
    public void setup() throws Exception {
        TestPluginsFixture.setup();

        FileWriter writer = new FileWriter("repositories.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<UpdateRepository> repositories = new ArrayList<UpdateRepository>();
        repositories.add(new DefaultUpdateRepository("localhost", new URL("http://localhost:8081/")));

        String json = gson.toJson(repositories);
        writer.write(json);
        writer.close();

        webServer = new WebServer();
        webServer.start();
    }

    @After
    public void tearDown() {
        webServer.shutdown();
    }

    @Test
    public void assertUpdateCreatesPlugins() {
        TestApplication subject = new TestApplication();
        subject.start();

        assertEquals("Expect no plugins loaded on start", 0, subject.getPluginManager().getPlugins().size());

        subject.update();

        assertEquals("Expect two plugins loaded on update", 2, subject.getPluginManager().getPlugins().size());
    }
}
