package org.springframework.content.commons.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

public class FileRemover implements InputStreamObserver, OutputStreamObserver {

    private static final Log LOGGER = LogFactory.getLog(FileRemover.class);

    private final File file;

    public FileRemover(File file) {
        this.file = file;
    }

    @Override
    public void closed() {
        if (!this.file.delete()) {
            LOGGER.debug("Failed to delete file: " + this.file);
        }
    }
}
