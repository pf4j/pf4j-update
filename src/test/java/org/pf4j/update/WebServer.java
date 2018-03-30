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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

/**
 * @author Decebal Suiu
 */
public class WebServer {

    private static final int DEFAULT_PORT = 8081;
    private static final String DEFAULT_RESOURCE_BASE = "./downloads/";

    private int port = DEFAULT_PORT;
    private String resourceBase = DEFAULT_RESOURCE_BASE;

    public int getPort() {
        return port;
    }

    public WebServer setPort(int port) {
        this.port = port;

        return this;
    }

    public String getResourceBase() {
        return resourceBase;
    }

    public WebServer setResourceBase(String resourceBase) {
        this.resourceBase = resourceBase;

        return this;
    }

    public void start() throws Exception {
        Server server = new Server();

        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        server.addConnector(connector);
        server.setStopAtShutdown(true);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(resourceBase);
        resourceHandler.setDirectoriesListed(true);

//        server.setHandler(contextHandler);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
        server.setHandler(handlers);

        server.start();
//        server.join();
    }

    public static void main(String[] args) {
        try {
            new WebServer().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
