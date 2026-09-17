package internal.org.springframework.content.commons.store.factory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.Store;
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

public class StoreImpl implements org.springframework.content.commons.repository.ContentStore<Object, Serializable>, ContentStore<Object, Serializable> {

    private static final Log logger = LogFactory.getLog(StoreImpl.class);

    private final Class<? extends Store> storeInterface;
    private final Store<Serializable> delegate;
    private final ApplicationEventPublisher publisher;
    private final Path copyContentRootPath;

    public StoreImpl(Class<? extends Store> storeInterface, Store<Serializable> delegate, ApplicationEventPublisher publisher, Path copyContentRootPath) {
        this.storeInterface = storeInterface;
        this.delegate = delegate;
        this.publisher = publisher;
        this.copyContentRootPath = copyContentRootPath;
    }

    @Override
    public Object setContent(Object entity, InputStream content) {
        return this.internalSetContent(entity, null, content, (actualContent) -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) (delegate)).setContent(entity, actualContent);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) (delegate)).setContent(entity, actualContent);
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content) {
        return this.internalSetContent(entity, propertyPath, content, (actualContent) -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) (delegate)).setContent(entity, propertyPath, actualContent);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) (delegate)).setContent(entity, propertyPath, actualContent);
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        return this.internalSetContent(entity, propertyPath, content, (actualContent) -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) (delegate)).setContent(entity, propertyPath, actualContent, contentLen);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) (delegate)).setContent(entity, propertyPath, actualContent, contentLen);
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.repository.SetContentParams params) {
        return this.internalSetContent(entity, propertyPath, content, (actualContent) -> {
            return ((org.springframework.content.commons.repository.ContentStore) (delegate)).setContent(entity, propertyPath, actualContent, params);
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        return this.internalSetContent(entity, propertyPath, content, (actualContent) -> {
            return ((ContentStore) delegate).setContent(entity, propertyPath, actualContent, params);
        });
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, InputStream content, Function<InputStream, Object> invocation) {
        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);

            org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = null;
            BeforeSetContentEvent before = null;

            if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
                oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, propertyPath, delegate, contentCopyStream);
                publisher.publishEvent(oldBefore);
            }

            if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
                before = new BeforeSetContentEvent(property, propertyPath, castToContentStore(delegate), contentCopyStream);
                publisher.publishEvent(before);
            }

            // input stream was processed and replaced
            if (oldBefore != null && oldBefore.getInputStream() != null &&
                    !oldBefore.getInputStream().equals(contentCopyStream)) {
                content = oldBefore.getInputStream();
            } else if (before != null && before.getInputStream() != null &&
                    !before.getInputStream().equals(contentCopyStream)) {
                content = before.getInputStream();
            }
            // content was processed but not replaced
            else if (contentCopyStream.isDirty()) {
                var buffer = new byte[4096];
                while (contentCopyStream.read(buffer) != -1) {
                }
                content = new FileInputStream(contentCopy);
            }

            result = invocation.apply(content);

            if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
                org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, propertyPath, delegate);
                oldAfter.setResult(result);
                publisher.publishEvent(oldAfter);
            }

            if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
                AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, castToContentStore(delegate));
                after.setResult(result);
                publisher.publishEvent(after);
            }
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
        return this.internalSetContent(entity, null, resourceContent, () -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) (delegate)).setContent(entity, resourceContent);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) (delegate)).setContent(entity, resourceContent);
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, Resource resourceContent) {
        return this.internalSetContent(entity, propertyPath, resourceContent, () -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) (delegate)).setContent(entity, propertyPath, resourceContent);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) (delegate)).setContent(entity, propertyPath, resourceContent);
            }
        });
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, Resource resourceContent, Supplier invocation) {
        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, propertyPath, delegate, resourceContent);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeSetContentEvent before = new BeforeSetContentEvent(property, propertyPath, castToContentStore(delegate), resourceContent);
            publisher.publishEvent(before);
        }

        Object result = invocation.get();

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, propertyPath, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, castToContentStore(delegate));
            after.setResult(result);
            publisher.publishEvent(after);
        }
        return result;
    }

    @Override
    public Object unsetContent(Object entity) {
        return this.internalUnsetContent(entity, null,
                () -> {
                    Object result;
                    if (delegate instanceof ContentStore) {
                        return ((ContentStore) (delegate)).unsetContent(entity);
                    } else {
                        return ((org.springframework.content.commons.repository.ContentStore) (delegate)).unsetContent(entity);
                    }
                });
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath) {
        return this.internalUnsetContent(entity, propertyPath,
                () -> {
                    Object result;
                    if (delegate instanceof ContentStore) {
                        return ((ContentStore) (delegate)).unsetContent(entity, propertyPath);
                    } else {
                        return ((org.springframework.content.commons.repository.ContentStore) (delegate)).unsetContent(entity, propertyPath);
                    }
                });
    }

    @Override
    public Object unsetContent(
            Object entity, PropertyPath propertyPath,
            org.springframework.content.commons.repository.UnsetContentParams params
    ) {
        return this.internalUnsetContent(entity, propertyPath, () -> {
            if (delegate instanceof ContentStore) {
                int ordinal = params.disposition().ordinal();
                UnsetContentParams params1 = new UnsetContentParams(UnsetContentParams.Disposition.values()[ordinal]);
                return ((ContentStore) delegate).unsetContent(entity, propertyPath, params1);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) delegate)
                        .unsetContent(entity, propertyPath, params);
            }
        });
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
        return this.internalUnsetContent(entity, propertyPath, () -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) delegate).unsetContent(entity, propertyPath, params);
            } else {
                int ordinal = params.disposition().ordinal();
                org.springframework.content.commons.repository.UnsetContentParams params1 = new org.springframework.content.commons.repository.UnsetContentParams(org.springframework.content.commons.repository.UnsetContentParams.Disposition.values()[ordinal]);
                return ((org.springframework.content.commons.repository.ContentStore) delegate)
                        .unsetContent(entity, propertyPath, params1);
            }
        });
    }

    public Object internalUnsetContent(Object entity, PropertyPath propertyPath, Supplier invocation) {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeUnsetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnsetContentEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(entity, propertyPath, castToContentStore(delegate));
            publisher.publishEvent(before);
        }

        Object result = invocation.get();

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterUnsetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnsetContentEvent(entity, propertyPath, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterUnsetContentEvent after = new AfterUnsetContentEvent(entity, propertyPath, castToContentStore(delegate));
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public InputStream getContent(Object entity) throws IOException {
        return this.internalGetContent(entity, null, () -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) delegate).getContent(entity);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) delegate).getContent(entity);
            }
        });
    }

    @Override
    public InputStream getContent(Object entity, PropertyPath propertyPath) throws IOException {
        return this.internalGetContent(entity, propertyPath, () -> {
            if (delegate instanceof ContentStore) {
                return ((ContentStore) delegate).getContent(entity, propertyPath);
            } else {
                return ((org.springframework.content.commons.repository.ContentStore) delegate)
                        .getContent(entity, propertyPath);
            }
        });
    }

//    @FunctionalInterface
//    private interface StreamInvocation {
//        InputStream get() throws IOException;
//    }

    public InputStream internalGetContent(Object entity, PropertyPath propertyPath, ThrowingSupplier<InputStream> invocation)
            throws IOException {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeGetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetContentEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeGetContentEvent before = new BeforeGetContentEvent(entity, propertyPath, castToContentStore(this.delegate));
            publisher.publishEvent(before);
        }

        InputStream result;
        try {
            result = invocation.getWithException();
        } catch (IOException ioException) {
            throw ioException;
        } catch (Exception anyOther) {
            throw new IOException("Error reading content.", anyOther);
        }

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterGetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetContentEvent(entity, propertyPath, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);
            if (oldAfter.getResult() != null) {
                result = (InputStream) oldAfter.getResult();
            }
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterGetContentEvent after = new AfterGetContentEvent(entity, propertyPath, castToContentStore(this.delegate));
            after.setResult(result);
            publisher.publishEvent(after);
            if (after.getResult() != null) {
                result = (InputStream) after.getResult();
            }
        }

        return result;
    }

    @Override
    public Resource getResource(Object entity) {
        return this.internalGetResource(entity, null, () -> {
            if (delegate instanceof org.springframework.content.commons.store.Store) {
                return ((AssociativeStore) delegate).getResource(entity);
            } else {
                return ((org.springframework.content.commons.repository.AssociativeStore) delegate)
                        .getResource(entity);
            }
        });
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath) {
        return this.internalGetResource(entity, propertyPath, () -> {
            if (delegate instanceof AssociativeStore) {
                return ((AssociativeStore) delegate).getResource(entity, propertyPath);
            } else {
                return ((org.springframework.content.commons.repository.AssociativeStore) delegate)
                        .getResource(entity, propertyPath);
            }
        });
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath, org.springframework.content.commons.repository.GetResourceParams oldParams) {
        return this.internalGetResource(entity, propertyPath, () ->
                ((org.springframework.content.commons.repository.AssociativeStore) delegate)
                        .getResource(entity, propertyPath, oldParams));
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
        return this.internalGetResource(entity, propertyPath, () -> {
            return ((AssociativeStore) delegate).getResource(entity, propertyPath, params);
        });
    }

    public Resource internalGetResource(Object entity, PropertyPath propertyPath, Supplier<Resource> invocation) {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, castToContentStore(delegate));
            publisher.publishEvent(before);
        }

        Resource result = invocation.get();

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(entity, propertyPath, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);
            if (oldAfter.getResult() != null) {
                result = (Resource) oldAfter.getResult();
            }
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, castToContentStore(delegate));
            after.setResult(result);
            publisher.publishEvent(after);
            if (after.getStore() != null) {
                result = (Resource) after.getResult();
            }
        }

        return result;
    }

    @Override
    public Resource getResource(Serializable id) {

        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(id, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, contentStore);
            publisher.publishEvent(before);
        }

        Resource result = delegate.getResource(id);

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(id, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterGetResourceEvent after = new AfterGetResourceEvent(id, contentStore);
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public void associate(Object entity, Serializable id) {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeAssociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeAssociateEvent(entity, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeAssociateEvent before = new BeforeAssociateEvent(entity, castToContentStore(delegate));
            publisher.publishEvent(before);
        }

        if (delegate instanceof AssociativeStore) {
            ((AssociativeStore) (delegate)).associate(entity, id);
        } else {
            ((org.springframework.content.commons.repository.AssociativeStore) (delegate)).associate(entity, id);
        }

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterAssociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterAssociateEvent(entity, delegate);
            publisher.publishEvent(oldAfter);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterAssociateEvent after = new AfterAssociateEvent(entity, castToContentStore(delegate));
            publisher.publishEvent(after);
        }
    }

    @Override
    public void associate(Object entity, PropertyPath propertyPath, Serializable id) {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeAssociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeAssociateEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, castToContentStore(delegate));
            publisher.publishEvent(before);
        }

        if (delegate instanceof AssociativeStore) {
            ((AssociativeStore) (delegate)).associate(entity, propertyPath, id);
        } else {
            ((org.springframework.content.commons.repository.AssociativeStore) (delegate)).associate(entity, propertyPath, id);
        }

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterAssociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterAssociateEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldAfter);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, castToContentStore(delegate));
            publisher.publishEvent(after);
        }
    }

    @Override
    public void unassociate(Object entity) {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeUnassociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnassociateEvent(entity, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, castToContentStore(delegate));
            publisher.publishEvent(before);
        }

        if (delegate instanceof AssociativeStore) {
            ((AssociativeStore) (delegate)).unassociate(entity);
        } else {
            ((org.springframework.content.commons.repository.AssociativeStore) (delegate)).unassociate(entity);
        }

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterUnassociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnassociateEvent(entity, delegate);
            publisher.publishEvent(oldAfter);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterUnassociateEvent after = new AfterUnassociateEvent(entity, castToContentStore(delegate));
            publisher.publishEvent(after);
        }
    }

    @Override
    public void unassociate(Object entity, PropertyPath propertyPath) {

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.BeforeUnassociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnassociateEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldBefore);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, castToContentStore(delegate));
            publisher.publishEvent(before);
        }

        if (delegate instanceof AssociativeStore) {
            ((AssociativeStore) (delegate)).unassociate(entity, propertyPath);
        } else {
            ((org.springframework.content.commons.repository.AssociativeStore) (delegate)).unassociate(entity, propertyPath);
        }

        if (org.springframework.content.commons.repository.ContentStore.class.isAssignableFrom(storeInterface)) {
            org.springframework.content.commons.repository.events.AfterUnassociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnassociateEvent(entity, propertyPath, delegate);
            publisher.publishEvent(oldAfter);
        }

        if (org.springframework.content.commons.store.ContentStore.class.isAssignableFrom(storeInterface)) {
            AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, castToContentStore(delegate));
            publisher.publishEvent(after);
        }
    }

    private <SID extends Serializable> ContentStore<Object, SID> castToContentStore(Store<Serializable> delegate) {
        if (!(delegate instanceof ContentStore)) {
            return null;
        }
        return (ContentStore) delegate;
    }

    private <SID extends Serializable> org.springframework.content.commons.repository.ContentStore<Object, SID> castToOldContentStore(Store<Serializable> delegate) {
        if (!(delegate instanceof org.springframework.content.commons.repository.ContentStore)) {
            return null;
        }
        return (org.springframework.content.commons.repository.ContentStore) delegate;
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
