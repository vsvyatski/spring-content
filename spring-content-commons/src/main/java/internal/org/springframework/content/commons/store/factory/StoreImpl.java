package internal.org.springframework.content.commons.store.factory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

public class StoreImpl implements ContentStore<Object, Serializable> {

    private static final Log logger = LogFactory.getLog(StoreImpl.class);

    private final Object delegate;
    private final ApplicationEventPublisher publisher;
    private final Path copyContentRootPath;

    public StoreImpl(Class<?> storeInterface, Object delegate, ApplicationEventPublisher publisher, Path copyContentRootPath) {
        this.delegate = delegate;
        this.publisher = publisher;
        this.copyContentRootPath = copyContentRootPath;
    }

    @Override
    public Object setContent(Object entity, InputStream content) {
        return this.internalSetContent(entity, null, content,
                (actualContent) -> ((ContentStore) delegate).setContent(entity, actualContent));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content) {
        return this.internalSetContent(entity, propertyPath, content,
                (actualContent) -> ((ContentStore) delegate).setContent(entity, propertyPath, actualContent));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        return this.internalSetContent(entity, propertyPath, content,
                (actualContent) -> ((ContentStore) delegate).setContent(entity, propertyPath, actualContent, contentLen));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        return this.internalSetContent(entity, propertyPath, content,
                (actualContent) -> ((ContentStore) delegate).setContent(entity, propertyPath, actualContent, params));
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, InputStream content, Function<InputStream, Object> invocation) {
        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);

            BeforeSetContentEvent before = new BeforeSetContentEvent(property, propertyPath, castToContentStore(delegate), contentCopyStream);
            publisher.publishEvent(before);

            if (before.getInputStream() != null && !before.getInputStream().equals(contentCopyStream)) {
                content = before.getInputStream();
            } else if (contentCopyStream.isDirty()) {
                var buffer = new byte[4096];
                while (contentCopyStream.read(buffer) != -1) {
                }
                content = new FileInputStream(contentCopy);
            }

            result = invocation.apply(content);

            AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, castToContentStore(delegate));
            after.setResult(result);
            publisher.publishEvent(after);
        } catch (IOException ioException) {
            ioException.printStackTrace();
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
    public Object setContent(Object entity, Resource resourceContent) {
        return this.internalSetContent(entity, null, resourceContent,
                () -> ((ContentStore) delegate).setContent(entity, resourceContent));
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, Resource resourceContent) {
        return this.internalSetContent(entity, propertyPath, resourceContent,
                () -> ((ContentStore) delegate).setContent(entity, propertyPath, resourceContent));
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, Resource resourceContent, Supplier invocation) {
        BeforeSetContentEvent before = new BeforeSetContentEvent(property, propertyPath, castToContentStore(delegate), resourceContent);
        publisher.publishEvent(before);

        Object result = invocation.get();

        AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, castToContentStore(delegate));
        after.setResult(result);
        publisher.publishEvent(after);
        return result;
    }

    @Override
    public Object unsetContent(Object entity) {
        return this.internalUnsetContent(entity, null, () -> ((ContentStore) delegate).unsetContent(entity));
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath) {
        return this.internalUnsetContent(entity, propertyPath, () -> ((ContentStore) delegate).unsetContent(entity, propertyPath));
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
        return this.internalUnsetContent(entity, propertyPath, () -> ((ContentStore) delegate).unsetContent(entity, propertyPath, params));
    }

    public Object internalUnsetContent(Object entity, PropertyPath propertyPath, Supplier invocation) {
        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(entity, propertyPath, castToContentStore(delegate));
        publisher.publishEvent(before);

        Object result = invocation.get();

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(entity, propertyPath, castToContentStore(delegate));
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public InputStream getContent(Object entity) throws IOException {
        return this.internalGetContent(entity, null, () -> ((ContentStore) delegate).getContent(entity));
    }

    @Override
    public InputStream getContent(Object entity, PropertyPath propertyPath) throws IOException {
        return this.internalGetContent(entity, propertyPath, () -> ((ContentStore) delegate).getContent(entity, propertyPath));
    }

    public InputStream internalGetContent(Object entity, PropertyPath propertyPath, ThrowingSupplier<InputStream> invocation)
            throws IOException {

        BeforeGetContentEvent before = new BeforeGetContentEvent(entity, propertyPath, castToContentStore(this.delegate));
        publisher.publishEvent(before);

        InputStream result;
        try {
            result = invocation.getWithException();
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception anyOther) {
            throw new IOException("Error reading content.", anyOther);
        }

        AfterGetContentEvent after = new AfterGetContentEvent(entity, propertyPath, castToContentStore(this.delegate));
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (InputStream) after.getResult();
        }

        return result;
    }

    @Override
    public Resource getResource(Object entity) {
        return this.internalGetResource(entity, null, () -> ((AssociativeStore) delegate).getResource(entity));
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath) {
        return this.internalGetResource(entity, propertyPath, () -> ((AssociativeStore) delegate).getResource(entity, propertyPath));
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
        return this.internalGetResource(entity, propertyPath, () -> ((AssociativeStore) delegate).getResource(entity, propertyPath, params));
    }

    public Resource internalGetResource(Object entity, PropertyPath propertyPath, Supplier<Resource> invocation) {
        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, castToContentStore(delegate));
        publisher.publishEvent(before);

        Resource result = invocation.get();

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, castToContentStore(delegate));
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (Resource) after.getResult();
        }

        return result;
    }

    @Override
    public Resource getResource(Serializable id) {
        Store<Serializable> storeDelegate = (Store<Serializable>) delegate;
        BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, storeDelegate);
        publisher.publishEvent(before);

        Resource result = storeDelegate.getResource(id);

        AfterGetResourceEvent after = new AfterGetResourceEvent(id, storeDelegate);
        after.setResult(result);
        publisher.publishEvent(after);
        if (after.getResult() != null) {
            result = (Resource) after.getResult();
        }

        return result;
    }

    @Override
    public void associate(Object entity, Serializable id) {
        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, castToContentStore(delegate));
        publisher.publishEvent(before);

        ((AssociativeStore) delegate).associate(entity, id);

        AfterAssociateEvent after = new AfterAssociateEvent(entity, castToContentStore(delegate));
        publisher.publishEvent(after);
    }

    @Override
    public void associate(Object entity, PropertyPath propertyPath, Serializable id) {
        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, castToContentStore(delegate));
        publisher.publishEvent(before);

        ((AssociativeStore) delegate).associate(entity, propertyPath, id);

        AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, castToContentStore(delegate));
        publisher.publishEvent(after);
    }

    @Override
    public void unassociate(Object entity) {
        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, castToContentStore(delegate));
        publisher.publishEvent(before);

        ((AssociativeStore) delegate).unassociate(entity);

        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, castToContentStore(delegate));
        publisher.publishEvent(after);
    }

    @Override
    public void unassociate(Object entity, PropertyPath propertyPath) {
        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, castToContentStore(delegate));
        publisher.publishEvent(before);

        ((AssociativeStore) delegate).unassociate(entity, propertyPath);

        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, castToContentStore(delegate));
        publisher.publishEvent(after);
    }

    private <SID extends Serializable> ContentStore<Object, SID> castToContentStore(Object delegate) {
        if (!(delegate instanceof ContentStore)) {
            return null;
        }
        return (ContentStore) delegate;
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
