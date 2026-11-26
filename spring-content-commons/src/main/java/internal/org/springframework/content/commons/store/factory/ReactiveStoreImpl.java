package internal.org.springframework.content.commons.store.factory;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.StoreEntityValidation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

public class ReactiveStoreImpl implements ReactiveContentStore<Object> {

    private final ReactiveContentStore<Object> delegate;

    public ReactiveStoreImpl(ReactiveContentStore<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Object> setContent(@NonNull Object entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer) {
        StoreEntityValidation.requireEntity(entity);
        return delegate.setContent(entity, path, contentLen, buffer);
    }

    @Override
    public Flux<ByteBuffer> getContent(@NonNull Object entity, PropertyPath path) {
        StoreEntityValidation.requireEntity(entity);
        return delegate.getContent(entity, path);
    }

    @Override
    public Mono<Object> unsetContent(@NonNull Object entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);
        return delegate.unsetContent(entity, propertyPath);
    }
}
