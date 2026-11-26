package org.springframework.content.commons.store;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.property.PropertyPath;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

@NullMarked
public interface ReactiveContentStore<S> {

    Mono<S> setContent(S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer);

    @Nullable
    Flux<ByteBuffer> getContent(S entity, PropertyPath path);

    Mono<S> unsetContent(S entity, PropertyPath propertyPath);
}
