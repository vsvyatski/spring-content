package internal.org.springframework.content.commons.store.factory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.store.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.util.function.ThrowingSupplier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Event-publishing {@link ContentStore} wrapper around a concrete store implementation.
 *
 * @author Vladimir Svyatski
 */
public class StoreImpl implements ContentStore<Object, Serializable> {

    private static final Log logger = LogFactory.getLog(StoreImpl.class);

    private final ContentStore<Object, Serializable> delegate;
    private final ApplicationEventPublisher publisher;
    private final Path copyContentRootPath;

    @SuppressWarnings("unchecked")
    public StoreImpl(Object delegate, ApplicationEventPublisher publisher, Path copyContentRootPath) {
        if (!(delegate instanceof ContentStore<?, ?>)) {
            throw new IllegalArgumentException("delegate must implement ContentStore");
        }
        this.delegate = (ContentStore<Object, Serializable>) delegate;
        this.publisher = publisher;
        this.copyContentRootPath = copyContentRootPath;
    }

    @Override
    public Object setContent(@NonNull Object entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        return this.internalSetContent(entity, null, content,
            (actualContent) -> delegate.setContent(entity, actualContent));
    }

    @Override
    public Object setContent(@NonNull Object entity, @NonNull PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalSetContent(entity, propertyPath, content,
            (actualContent) -> delegate.setContent(entity, propertyPath, actualContent));
    }

    @Override
    public Object setContent(@NonNull Object entity, @NonNull PropertyPath propertyPath, InputStream content,
            long contentLen) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalSetContent(entity, propertyPath, content,
            (actualContent) -> delegate.setContent(entity, propertyPath, actualContent, contentLen));
    }

    @Override
    public Object setContent(@NonNull Object entity, @NonNull PropertyPath propertyPath, InputStream content,
            @NonNull SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        return this.internalSetContent(entity, propertyPath, content,
            (actualContent) -> delegate.setContent(entity, propertyPath, actualContent, params));
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, InputStream content,
                                     Function<InputStream, Object> invocation) {
        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);

            BeforeSetContentEvent before =
                new BeforeSetContentEvent(property, propertyPath, delegate, contentCopyStream);
            publisher.publishEvent(before);

            if (before.getInputStream() != null && !before.getInputStream().equals(contentCopyStream)) {
                result = invocation.apply(before.getInputStream());
            } else if (contentCopyStream.isDirty()) {
                IOUtils.consume(contentCopyStream);
                result = invocation.apply(new ByteArrayInputStream(Files.readAllBytes(contentCopy.toPath())));
            } else {
                result = invocation.apply(content);
            }

            AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, delegate);
            after.setResult(result);
            publisher.publishEvent(after);
        } catch (IOException ioException) {
            logger.error("", ioException);
        } finally {
            if (contentCopyStream != null) {
                IOUtils.closeQuietly(contentCopyStream);
            }
            if (contentCopy != null) {
                try {
                    Files.deleteIfExists(contentCopy.toPath());
                } catch (IOException e) {
                    logger.error(String.format("Unable to delete content copy %s", contentCopy.toPath()), e);
                }
            }
        }

        return result;
    }

    @Override
    public Object setContent(@NonNull Object entity, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        return this.internalSetContent(entity, null, resourceContent,
            () -> delegate.setContent(entity, resourceContent));
    }

    @Override
    public Object setContent(@NonNull Object entity, @NonNull PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalSetContent(entity, propertyPath, resourceContent,
            () -> delegate.setContent(entity, propertyPath, resourceContent));
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, Resource resourceContent,
                                     Supplier<Object> invocation) {
        BeforeSetContentEvent before =
            new BeforeSetContentEvent(property, propertyPath, delegate, resourceContent);
        publisher.publishEvent(before);

        Object result = invocation.get();

        AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);
        return result;
    }

    @Override
    public Object unsetContent(@NonNull Object entity) {
        StoreEntityValidation.requireEntity(entity);
        return this.internalUnsetContent(entity, null, () -> delegate.unsetContent(entity));
    }

    @Override
    public Object unsetContent(@NonNull Object entity, @NonNull PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalUnsetContent(entity, propertyPath, () -> delegate.unsetContent(entity, propertyPath));
    }

    @Override
    public Object unsetContent(@NonNull Object entity, @NonNull PropertyPath propertyPath,
            @NonNull UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireUnsetContentParams(params);
        return this.internalUnsetContent(entity, propertyPath,
            () -> delegate.unsetContent(entity, propertyPath, params));
    }

    public Object internalUnsetContent(Object entity, PropertyPath propertyPath, Supplier<Object> invocation) {
        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        Object result = invocation.get();

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public InputStream getContent(@NonNull Object entity) throws IOException {
        StoreEntityValidation.requireEntity(entity);
        return this.internalGetContent(entity, null, () -> delegate.getContent(entity));
    }

    @Override
    public InputStream getContent(@NonNull Object entity, @NonNull PropertyPath propertyPath) throws IOException {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalGetContent(entity, propertyPath, () -> delegate.getContent(entity, propertyPath));
    }

    public InputStream internalGetContent(Object entity, PropertyPath propertyPath,
                                          ThrowingSupplier<InputStream> invocation) throws IOException {

        BeforeGetContentEvent before = new BeforeGetContentEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        InputStream result;
        try {
            result = invocation.getWithException();
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception anyOther) {
            throw new IOException("Error reading content.", anyOther);
        }

        AfterGetContentEvent after = new AfterGetContentEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (InputStream) after.getResult();
        }

        return result;
    }

    @Override
    public Resource getResource(@NonNull Object entity) {
        StoreEntityValidation.requireEntity(entity);
        return this.internalGetResource(entity, null, () -> delegate.getResource(entity));
    }

    @Override
    public Resource getResource(@NonNull Object entity, @NonNull PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalGetResource(entity, propertyPath, () -> delegate.getResource(entity, propertyPath));
    }

    @Override
    public Resource getResource(@NonNull Object entity, @NonNull PropertyPath propertyPath,
            @NonNull GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireGetResourceParams(params);
        return this.internalGetResource(entity, propertyPath,
            () -> delegate.getResource(entity, propertyPath, params));
    }

    public Resource internalGetResource(Object entity, PropertyPath propertyPath, Supplier<Resource> invocation) {
        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        Resource result = invocation.get();

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (Resource) after.getResult();
        }

        return result;
    }

    @Override
    public Resource getResource(Serializable id) {
        BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, delegate);
        publisher.publishEvent(before);

        Resource result = delegate.getResource(id);

        AfterGetResourceEvent after = new AfterGetResourceEvent(id, delegate);
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (Resource) after.getResult();
        }

        return result;
    }

    @Override
    public void associate(@NonNull Object entity, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, delegate);
        publisher.publishEvent(before);

        delegate.associate(entity, id);

        AfterAssociateEvent after = new AfterAssociateEvent(entity, delegate);
        publisher.publishEvent(after);
    }

    @Override
    public void associate(@NonNull Object entity, @NonNull PropertyPath propertyPath, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        delegate.associate(entity, propertyPath, id);

        AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(after);
    }

    @Override
    public void unassociate(@NonNull Object entity) {
        StoreEntityValidation.requireEntity(entity);
        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, delegate);
        publisher.publishEvent(before);

        delegate.unassociate(entity);

        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, delegate);
        publisher.publishEvent(after);
    }

    @Override
    public void unassociate(@NonNull Object entity, @NonNull PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        delegate.unassociate(entity, propertyPath);

        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(after);
    }

    static class TeeInputStream extends org.apache.commons.io.input.TeeInputStream {

        private boolean dirty;

        public TeeInputStream(InputStream input, OutputStream branch, boolean closeBranch) {
            super(input, branch, closeBranch);
        }

        public boolean isDirty() {
            return dirty;
        }

        @Override
        public int read() throws IOException {
            dirty = true;
            return super.read();
        }

        @Override
        public int read(byte[] bts, int st, int end) throws IOException {
            dirty = true;
            return super.read(bts, st, end);
        }

        @Override
        public int read(byte[] bts) throws IOException {
            dirty = true;
            return super.read(bts);
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }
}
