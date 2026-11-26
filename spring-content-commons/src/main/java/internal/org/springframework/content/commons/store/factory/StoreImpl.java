package internal.org.springframework.content.commons.store.factory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.commons.store.events.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;

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
@NullMarked
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
    public Object setContent(Object entity, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentInputStream(content);
        return this.internalSetContent(entity, null, content,
            (actualContent) -> delegate.setContent(entity, actualContent));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentInputStream(content);
        return this.internalSetContent(entity, propertyPath, content,
            (actualContent) -> delegate.setContent(entity, propertyPath, actualContent));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentInputStream(content);
        return this.internalSetContent(entity, propertyPath, content,
            (actualContent) -> delegate.setContent(entity, propertyPath, actualContent, contentLen));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireSetContentParams(params);
        StoreEntityValidation.requireContentInputStream(content);
        return this.internalSetContent(entity, propertyPath, content,
            (actualContent) -> delegate.setContent(entity, propertyPath, actualContent, params));
    }

    public Object internalSetContent(Object property, @Nullable PropertyPath propertyPath, InputStream content,
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

        return result != null ? result : property;
    }

    @Override
    public Object setContent(Object entity, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentResource(resourceContent);
        return this.internalSetContent(entity, null, resourceContent,
            () -> delegate.setContent(entity, resourceContent));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, Resource resourceContent) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentResource(resourceContent);
        return this.internalSetContent(entity, propertyPath, resourceContent,
            () -> delegate.setContent(entity, propertyPath, resourceContent));
    }

    public Object internalSetContent(Object property, @Nullable PropertyPath propertyPath, Resource resourceContent,
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
    public Object unsetContent(Object entity) {
        StoreEntityValidation.requireEntity(entity);
        return this.internalUnsetContent(entity, null, () -> delegate.unsetContent(entity));
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalUnsetContent(entity, propertyPath, () -> delegate.unsetContent(entity, propertyPath));
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireUnsetContentParams(params);
        return this.internalUnsetContent(entity, propertyPath,
            () -> delegate.unsetContent(entity, propertyPath, params));
    }

    public Object internalUnsetContent(Object entity, @Nullable PropertyPath propertyPath, Supplier<Object> invocation) {
        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        Object result = invocation.get();

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public @Nullable InputStream getContent(Object entity) throws IOException {
        StoreEntityValidation.requireEntity(entity);
        return this.internalGetContent(entity, null, () -> delegate.getContent(entity));
    }

    @Override
    public @Nullable InputStream getContent(Object entity, PropertyPath propertyPath) throws IOException {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalGetContent(entity, propertyPath, () -> delegate.getContent(entity, propertyPath));
    }

    private @Nullable InputStream internalGetContent(Object entity, @Nullable PropertyPath propertyPath,
                                                     ContentFetcher fetcher) throws IOException {

        BeforeGetContentEvent before = new BeforeGetContentEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        InputStream result = fetcher.fetch();

        AfterGetContentEvent after = new AfterGetContentEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (InputStream) after.getResult();
        }

        return result;
    }

    @Override
    public @Nullable Resource getResource(Object entity) {
        StoreEntityValidation.requireEntity(entity);
        return this.internalGetResource(entity, null, () -> delegate.getResource(entity));
    }

    @Override
    public @Nullable Resource getResource(Object entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return this.internalGetResource(entity, propertyPath, () -> delegate.getResource(entity, propertyPath));
    }

    @Override
    public @Nullable Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireGetResourceParams(params);
        return this.internalGetResource(entity, propertyPath,
            () -> delegate.getResource(entity, propertyPath, params));
    }

    private @Nullable Resource internalGetResource(Object entity, @Nullable PropertyPath propertyPath,
                                                   ResourceFetcher fetcher) {
        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        Resource result = fetcher.fetch();

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (Resource) after.getResult();
        }

        return result;
    }

    @Override
    public @Nullable Resource getResource(Serializable id) {
        StoreEntityValidation.requireContentId(id);
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
    public void associate(Object entity, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requireContentId(id);
        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, delegate);
        publisher.publishEvent(before);

        delegate.associate(entity, id);

        AfterAssociateEvent after = new AfterAssociateEvent(entity, delegate);
        publisher.publishEvent(after);
    }

    @Override
    public void associate(Object entity, PropertyPath propertyPath, Serializable id) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        StoreEntityValidation.requireContentId(id);
        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        delegate.associate(entity, propertyPath, id);

        AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(after);
    }

    @Override
    public void unassociate(Object entity) {
        StoreEntityValidation.requireEntity(entity);
        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, delegate);
        publisher.publishEvent(before);

        delegate.unassociate(entity);

        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, delegate);
        publisher.publishEvent(after);
    }

    @Override
    public void unassociate(Object entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        delegate.unassociate(entity, propertyPath);

        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(after);
    }

    @FunctionalInterface
    private interface ContentFetcher {
        @Nullable
        InputStream fetch() throws IOException;
    }

    @FunctionalInterface
    private interface ResourceFetcher {
        @Nullable
        Resource fetch();
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
