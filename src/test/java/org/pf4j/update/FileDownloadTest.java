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
import org.pf4j.PluginException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.*;

/**
 * Tests file download
 */
public class FileDownloadTest {

    private SimpleFileDownloader downloader;
    private WebServer webserver;
    private Path updateRepoDir;
    private Path repoFile;
    private Path emptyFile;

    @Before
    public void setup() throws IOException {
        downloader = new SimpleFileDownloader();
        webserver = new WebServer();
        updateRepoDir = Files.createTempDirectory("repo");
        updateRepoDir.toFile().deleteOnExit();
        repoFile = Files.createFile(updateRepoDir.resolve("myfile"));
        BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(repoFile, Charset.forName("utf-8"), StandardOpenOption.APPEND));
        writer.write("test");
        writer.close();
        emptyFile = Files.createFile(updateRepoDir.resolve("emptyFile"));
    }

    @Test
    public void downloadLocal() throws Exception {
        assertTrue(Files.exists(repoFile));
        Path downloaded = downloader.downloadFile(repoFile.toUri().toURL());
        assertTrue(Files.exists(downloaded));
        // File still remains in original location
        assertTrue(Files.exists(repoFile));
        // File attributes are copied
        assertEquals(repoFile.toFile().lastModified(), downloaded.toFile().lastModified());
        Files.delete(downloaded);
        assertTrue(Files.exists(repoFile));
    }

    @Test
    public void downloadHttp() throws Exception {
        webserver.setPort(55000);
        webserver.setResourceBase(updateRepoDir.toAbsolutePath().toString());
        webserver.start();

        URL downloadUrl = new URL("http://localhost:55000/myfile");
        Path downloaded = downloader.downloadFile(downloadUrl);
        assertTrue(Files.exists(downloaded));
        assertEquals(4, Files.size(downloaded));
        // File attributes are copied
        assertEquals(downloadUrl.openConnection().getLastModified(), downloaded.toFile().lastModified());
    }

    @Test(expected = PluginException.class)
    public void unsupportedProtocol() throws Exception {
        downloader.downloadFile(new URL("jar:file:!/myfile.jar"));
    }
}