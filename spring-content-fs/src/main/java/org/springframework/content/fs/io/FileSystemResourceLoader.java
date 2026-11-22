package org.springframework.content.fs.io;

import internal.org.springframework.content.fs.io.FileSystemDeletableResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.FileServiceImpl;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import static org.springframework.util.StringUtils.cleanPath;

/**
 * The implementation of the {@link ResourceLoader} that resolves plain paths as {@link DeletableResource}
 * file system resources, rather than as class path resources (the latter is the default strategy of
 * {@link DefaultResourceLoader}).
 * <p/>
 * <b>NOTE:</b> Plain paths will always be interpreted relative to the root specified during instantiation,
 * rather than relative to the current VM working directory (the latter is the default behavior of
 * {@link org.springframework.core.io.FileSystemResourceLoader FileSystemResourceLoader}, even if they start
 * with a slash).
 */
public class FileSystemResourceLoader
    extends org.springframework.core.io.FileSystemResourceLoader {

    private static final Log logger = LogFactory.getLog(FileSystemResourceLoader.class);

    private final FileSystemResource root;
    private FileService fileService = null;

    public FileSystemResourceLoader(@NonNull String root) {
        Assert.notNull(root, "root must not be null");
        logger.info(String.format("Defaulting filesystem root to %s", root));
        this.root = new FileSystemResource(suffixPath(cleanPath(root)));
        this.fileService = new FileServiceImpl();
    }

    public FileSystemResource getRootResource() {
        return root;
    }

    private String suffixPath(String path) {
        if (!path.endsWith("/")) {
            return path + "/";
        }
        return path;
    }

    @Override
    public @NonNull Resource getResource(@NonNull String location) {
        Resource resource = root.createRelative(location);
        if (resource instanceof FileSystemResource fsr) {
            resource = new FileSystemDeletableResource(fsr, fileService);
        }
        return resource;
    }
}
