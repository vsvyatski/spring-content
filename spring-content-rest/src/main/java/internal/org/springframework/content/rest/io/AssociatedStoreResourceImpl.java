package internal.org.springframework.content.rest.io;

import jakarta.persistence.Version;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.HttpResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
/**
 * Represents a Spring Content Resource that is associated with a Spring Data Entity.
 * <p>
 * Overrides content length with the value stored on the Spring Data entity when present,
 * otherwise uses the backing content resource.
 * <p>
 * Also sets appropriate headers to pass the Spring Data Entity recorded filename, if exists.
 */
public class AssociatedStoreResourceImpl<S> implements HttpResource, AssociatedStoreResource<S> {

    private final S entity;
    private final Resource original;
    private final StoreInfo info;
    private final ContentProperty contentProperty;
    private final PropertyPath propertyPath;

    public AssociatedStoreResourceImpl(StoreInfo info, S entity, PropertyPath propertyPath, ContentProperty property, Resource original) {
        this.info = info;
        this.entity = entity;
        this.propertyPath = propertyPath;
        this.contentProperty = property;
        this.original = original;
    }

    @Override
    public StoreInfo getStoreInfo() {
        return info;
    }

    @Override
    public S getAssociation() {
        return entity;
    }

    @Override
    public PropertyPath getPropertyPath() {
        return this.propertyPath;
    }

    @Override
    public ContentProperty getContentProperty() {
        return this.contentProperty;
    }

    protected Resource getDelegate() {
        return original;
    }

    @Override
    public boolean isRenderableAs(org.springframework.util.MimeType mimeType) {
        Renderable<S> renderer = renderableStore();
        if (renderer == null) {
            return false;
        }
        return renderer.hasRendition(getAssociation(), this.getPropertyPath(), mimeType.toString());
    }

    @Override
    public InputStream renderAs(org.springframework.util.MimeType mimeType) {
        Renderable<S> renderer = renderableStore();
        if (renderer == null) {
            return null;
        }
        return renderer.getRendition(getAssociation(), this.getPropertyPath(), mimeType.toString());
    }

    @SuppressWarnings("unchecked")
    private @Nullable Renderable<S> renderableStore() {
        if (!Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {
            return null;
        }
        return (Renderable<S>) this.getStoreInfo().getImplementation(AssociativeStore.class);
    }

    @Override
    public Object getETag() {
        Object etag = BeanUtils.getFieldWithAnnotation(entity, Version.class);
        if (etag == null) {
            etag = "";
        }

        return etag.toString();
    }

    @Override
    public MediaType getMimeType() {
        Object mimeType = this.getContentProperty().getMimeType(this.getAssociation());

        return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
    }

    @Override
    public long contentLength() throws IOException {
        // TODO: can we remove this is all properties are effectively embedded?
        // Long contentLength = null; (Long) BeanUtils.getFieldWithAnnotation(property, ContentLength.class);
        Long contentLength = (Long) this.getContentProperty().getContentLength(this.getAssociation());
        if (contentLength == null) {
            contentLength = getDelegate().contentLength();
        }
        return contentLength;
    }

    @Override
    public long lastModified() throws IOException {
        Resource delegate = getDelegate();
        if (delegate != null) {
            return delegate.lastModified();
        }
        return -1L;
    }

    @Override
    public @NonNull HttpHeaders getResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        // Modified to show download
        Object originalFileName = this.getContentProperty().getOriginalFileName(this.getAssociation());
        if (originalFileName != null && StringUtils.hasText(originalFileName.toString())) {
            headers.setContentDisposition(ContentDisposition.attachment().filename((String) originalFileName, StandardCharsets.UTF_8).build());
        } else {
            headers.setContentDisposition(ContentDisposition.attachment().build());
        }
        return headers;
    }

    @Override
    public boolean exists() {
        return contentProperty.getContentId(entity) != null;
    }

    @Override
    public boolean isReadable() {
        return original.isReadable();
    }

    @Override
    public boolean isOpen() {
        return original.isOpen();
    }

    @Override
    public boolean isFile() {
        return original.isFile();
    }

    @Override
    public @NonNull URL getURL() throws IOException {
        return original.getURL();
    }

    @Override
    public @NonNull URI getURI() throws IOException {
        return original.getURI();
    }

    @Override
    public @NonNull File getFile() throws IOException {
        return original.getFile();
    }

    @Override
    public @NonNull ReadableByteChannel readableChannel() throws IOException {
        return original.readableChannel();
    }

    @Override
    public @NonNull Resource createRelative(@NonNull String relativePath) throws IOException {
        return original.createRelative(relativePath);
    }

    @Override
    public String getFilename() {
        return original.getFilename();
    }

    @Override
    public @NonNull String getDescription() {
        return original.getDescription();
    }

    @Override
    public @NonNull InputStream getInputStream() throws IOException {
        return original.getInputStream();
    }

    @Override
    public boolean isWritable() {
        return ((WritableResource) original).isWritable();
    }

    @Override
    public @NonNull OutputStream getOutputStream() throws IOException {
        return ((WritableResource) original).getOutputStream();
    }

    @Override
    public @NonNull WritableByteChannel writableChannel() throws IOException {
        return ((WritableResource) original).writableChannel();
    }


    @Override
    public void delete() throws IOException {
        ((DeletableResource) original).delete();
    }

    @Override
    public void setRange(String range) {
        if (original instanceof RangeableResource) {
            ((RangeableResource) original).setRange(range);
        }
    }
}
