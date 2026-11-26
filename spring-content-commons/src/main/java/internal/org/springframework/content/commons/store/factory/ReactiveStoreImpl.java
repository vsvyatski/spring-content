package internal.org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.StoreEntityValidation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

@NullMarked
public class ReactiveStoreImpl implements ReactiveContentStore<Object> {

    private final ReactiveContentStore<Object> delegate;

    public ReactiveStoreImpl(ReactiveContentStore<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Object> setContent(Object entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(path);
        StoreEntityValidation.requireContentBuffer(buffer);
        return delegate.setContent(entity, path, contentLen, buffer);
    }

    @Override
    public @Nullable Flux<ByteBuffer> getContent(Object entity, PropertyPath path) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(path);
        return delegate.getContent(entity, path);
    }

    @Override
    public Mono<Object> unsetContent(Object entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        StoreEntityValidation.requirePropertyPath(propertyPath);
        return delegate.unsetContent(entity, propertyPath);
    }
}
