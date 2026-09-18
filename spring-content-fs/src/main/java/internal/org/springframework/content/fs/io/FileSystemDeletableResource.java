package internal.org.springframework.content.fs.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.IdentifiableResource;
import org.springframework.content.commons.utils.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.String.format;

public class FileSystemDeletableResource implements WritableResource, DeletableResource, IdentifiableResource {

    private static final Log logger = LogFactory.getLog(FileSystemDeletableResource.class);

    private final FileSystemResource resource;
    private Serializable id;
    private final FileService fileService;
    
    public FileSystemDeletableResource(FileSystemResource resource, FileService fileService) {
        this.resource = resource;
        this.fileService = fileService;
    }

    @Override
    public void delete() {

        File parent = null;
        try {
            parent = resource.getFile().getParentFile();
            FileUtils.forceDelete(this.getFile());
        } catch (IOException e) {
            logger.warn(format("Unable to get file for resource %s", resource));
        }

        if (parent != null) {
            try {
                fileService.rmdirs(parent);
            } catch (IOException e) {
                logger.warn(format("Removing orphaned directories starting at %s, left by removal of resource %s", parent.getAbsolutePath(), resource));
            }
        }
    }

    @Override
    public boolean isOpen() {
        return resource.isOpen();
    }

    public final String getPath() {
        return resource.getPath();
    }

    @Override
    public boolean exists() {
        return resource.exists();
    }

    @Override
    public boolean isReadable() {
        return resource.isReadable();
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        return resource.getInputStream();
    }

    @Override
    public boolean isWritable() {
        return resource.isWritable();
    }

    @Override
    public long lastModified() throws IOException {
        return resource.lastModified();
    }

    @Override
    @NonNull
    public OutputStream getOutputStream() throws IOException {
        if (!exists()) {
            Files.createDirectories(Paths.get(this.getFile().getParent()));
            Files.createFile(this.getFile().toPath());
        }
        return resource.getOutputStream();
    }

    @Override
    @NonNull
    public URL getURL() throws IOException {
        return resource.getURL();
    }

    @Override
    @NonNull
    public URI getURI() throws IOException {
        return resource.getURI();
    }

    @Override
    @NonNull
    public File getFile() {
        return resource.getFile();
    }

    @Override
    public long contentLength() throws IOException {
        return resource.contentLength();
    }

    @Override
    @NonNull
    public Resource createRelative(@NonNull String relativePath) {
        return resource.createRelative(relativePath);
    }

    @Override
    public String toString() {
        return resource.toString();
    }

    @Override
    public String getFilename() {
        return resource.getFilename();
    }

    @Override
    @NonNull
    public String getDescription() {
        return resource.getDescription();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                (obj instanceof FileSystemDeletableResource other && resource.equals(other.resource));
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public Serializable getId() {
        return id;
    }

    @Override
    public void setId(Serializable id) {
        this.id = id;
    }
}