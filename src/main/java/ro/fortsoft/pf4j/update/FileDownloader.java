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
