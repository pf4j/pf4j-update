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

import ro.fortsoft.pf4j.PluginException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Interface to download a file
 */
public interface FileDownloader {
    /**
     * Downloads a file to destination. The implementation should download to a temporary folder
     * @param fileUrl the file to download
     * @return Path of downloaded file, typically in a temporary folder
     * @throws IOException if there was an IO problem during download
     * @throws PluginException if file could be downloaded but there were other problems
     */
    public Path downloadFile(URL fileUrl) throws PluginException, IOException;
}
