package org.springframework.content.commons.store;

import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.property.PropertyPath;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

public interface ReactiveContentStore<S> {

    Mono<S> setContent(@NonNull S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer);

    Flux<ByteBuffer> getContent(@NonNull S entity, PropertyPath path);

    Mono<S> unsetContent(@NonNull S entity, PropertyPath propertyPath);
}
