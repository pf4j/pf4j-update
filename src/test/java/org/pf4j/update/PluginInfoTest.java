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

import org.junit.Before;
import org.junit.Test;
import org.pf4j.update.PluginInfo.PluginRelease;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Test the pluginInfo
 */
public class PluginInfoTest {

    private PluginInfo pi1;
    private PluginInfo pi2;

    @Before
    public void setup() {
        pi1 = new PluginInfo();
        pi1.id = "pi1";
        PluginRelease pi1r1 = new PluginRelease();
        pi1r1.version = "1.0.0";
        pi1r1.requires = "2";
        PluginRelease pi1r2 = new PluginRelease();
        pi1r2.version = "1.1.0";
        pi1r2.requires = "~2.1";
        PluginRelease pi1r3 = new PluginRelease();
        pi1r3.version = "1.2.0";
        pi1r3.requires = ">2.5 & < 4";
        pi1.releases = Arrays.asList(pi1r1, pi1r2, pi1r3);
        pi2 = new PluginInfo();
        PluginRelease pi2r1 = new PluginRelease();
        pi2r1.version = "1.0.0";
        pi2.id = "aaa";
        pi2.releases = Collections.singletonList(pi2r1);
    }

    @Test
    public void comparePluginInfo() {
        assertTrue(pi1.compareTo(pi2) > 0);
    }

}