package org.springframework.content.commons.convert;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * {@link GenericConverter} that resolves a bucket name (from a {@code bucketAnnotation} on the source object or a
 * configured default) and an object key from {@link Object#toString()}, then builds a backend-specific blob id.
 *
 * @author Vladimir Svyatski
 */
public abstract class AbstractBucketBlobIdResolverConverter implements GenericConverter {

    private final String defaultBucket;
    private final Class<? extends Annotation> bucketAnnotation;
    private final Class<?> blobIdType;
    private final BiFunction<String, String, ?> blobIdFactory;

    protected AbstractBucketBlobIdResolverConverter(String defaultBucket,
                                                    Class<? extends Annotation> bucketAnnotation,
                                                    Class<?> blobIdType,
                                                    BiFunction<String, String, ?> blobIdFactory) {
        this.defaultBucket = defaultBucket;
        this.bucketAnnotation = bucketAnnotation;
        this.blobIdType = blobIdType;
        this.blobIdFactory = blobIdFactory;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(new ConvertiblePair(Serializable.class, blobIdType));
    }

    @Override
    public @Nullable Object convert(@Nullable Object source, @NonNull TypeDescriptor sourceType,
                                    @NonNull TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        String bucket = resolveBucket(source, defaultBucket, bucketAnnotation);
        String key = source.toString();
        return key != null ? blobIdFactory.apply(bucket, key) : null;
    }

      private static String resolveBucket(@NonNull Object source, String defaultBucketName,
                                        Class<? extends Annotation> bucketAnnotation) {
        Object bucket = BeanUtils.getAnnotatedValue(source, bucketAnnotation);
        if (bucket == null) {
            bucket = defaultBucketName;
        }
        if (bucket == null) {
            throw new StoreAccessException("Bucket not set");
        }
        return bucket.toString();
    }
}
