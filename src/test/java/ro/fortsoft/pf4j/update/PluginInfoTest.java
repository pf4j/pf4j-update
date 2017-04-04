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
import org.junit.Before;
import org.junit.Test;
import ro.fortsoft.pf4j.update.UpdateRepository.PluginInfo;
import ro.fortsoft.pf4j.update.UpdateRepository.PluginRelease;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test the pluginInfo
 */
public class PluginInfoTest {
    private PluginInfo pi1;

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
    }

    @Test
    public void getLastRelease() throws Exception {
        assertEquals("1.0.0", pi1.getLastRelease(Version.forIntegers(2, 0, 1)).version);
        assertEquals("1.0.0", pi1.getLastRelease(Version.forIntegers(2, 5)).version);
        assertEquals("1.1.0", pi1.getLastRelease(Version.forIntegers(2, 1, 8)).version);
        assertEquals("1.2.0", pi1.getLastRelease(Version.forIntegers(3)).version);
        assertEquals("1.2.0", pi1.getLastRelease(Version.forIntegers(0, 0, 0)).version);
        assertEquals(null, pi1.getLastRelease(Version.forIntegers(4)));
    }

    @Test
    public void hasUpdate() throws Exception {
        assertTrue(pi1.hasUpdate(Version.forIntegers(3), Version.valueOf("1.1.0")));
        assertFalse(pi1.hasUpdate(Version.forIntegers(2, 3), Version.valueOf("1.1.0")));

        // There are no versions certified for System version 5, so no updates either.
        // In this case, the installed plugin will be disabled by pf4j
        assertFalse(pi1.hasUpdate(Version.forIntegers(5), Version.valueOf("1.0.0")));
    }

}