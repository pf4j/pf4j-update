/*
 * Copyright 2018 Decebal Suiu
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

import org.pf4j.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

public class ZipPluginManager extends DefaultPluginManager {
  @Override
  protected PluginDescriptorFinder createPluginDescriptorFinder() {
    CompoundPluginDescriptorFinder descriptorFinder = (CompoundPluginDescriptorFinder) super.createPluginDescriptorFinder();
    descriptorFinder.add(new ZipPluginDescriptorFinder());
    return descriptorFinder;
  }

  private class ZipPluginDescriptorFinder implements PluginDescriptorFinder {
    @Override
    public boolean isApplicable(Path pluginPath) {
      return pluginPath.getFileName().toString().toUpperCase().endsWith(".ZIP");
    }

    @Override
    public PluginDescriptor find(Path pluginPath) throws PluginException {
      try {
        ZipFile zipFile = new ZipFile(pluginPath.toFile());
//        while (zipFile.getEntry().entries().hasMoreElements()) {
//          ZipEntry entry = zipFile.entries().nextElement();
//          entry.
          return new ZipPluginDescriptorFinder().find(pluginPath);
//        }
      } catch (IOException e) {
        throw new PluginException(e);
      }
    }
  }
}
