package internal.org.springframework.content.s3.store;

import internal.org.springframework.content.commons.utils.ContentPropertyInfoTypeDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ReactiveContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreEntityValidation;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Transactional
public class DefaultReactiveS3StoreImpl<S, SID extends Serializable> implements ReactiveContentStore<S> {

    private static final Log logger = LogFactory.getLog(DefaultReactiveS3StoreImpl.class);

    private final MappingContext mappingContext;
    private final PlacementService placementService;
    private final S3AsyncClient asyncClient;

    public DefaultReactiveS3StoreImpl(ApplicationContext context, ResourceLoader loader, MappingContext mappingContext,
                                      PlacementService placementService, S3AsyncClient asyncClient) {
        Assert.notNull(context, "context must be specified");
        Assert.notNull(loader, "loader must be specified");
        Assert.notNull(placementService, "placementService must be specified");
        this.placementService = placementService;
        this.asyncClient = asyncClient;
        this.mappingContext = mappingContext != null ? mappingContext : new MappingContext("/", ".");
    }

    @SuppressWarnings("unchecked")
    private S3ObjectId getS3ObjectId(S entity, PropertyPath path, ContentProperty property) {
        TypeDescriptor contentPropertyInfoType = ContentPropertyInfoTypeDescriptor.withGenerics(entity, property);
        if (!placementService.canConvert(contentPropertyInfoType, TypeDescriptor.valueOf(S3ObjectId.class))) {
            throw new IllegalStateException(String.format("Unable to convert %s to an S3ObjectId", contentPropertyInfoType));
        }
        ContentPropertyInfo<S, SID> contentPropertyInfo =
            new ContentPropertyInfo<>(entity, (SID) property.getContentId(entity), path, property);
        return (S3ObjectId) placementService.convert(contentPropertyInfo, contentPropertyInfoType,
            TypeDescriptor.valueOf(S3ObjectId.class));
    }

    @Transactional
    @Override
    public Mono<S> setContent(@NonNull S entity, PropertyPath path, long contentLen, Flux<ByteBuffer> buffer) {
        StoreEntityValidation.requireEntity(entity);

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), path.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", path.name()));
        }

        if (property.getContentId(entity) == null) {
            Serializable newId = UUID.randomUUID().toString();
            Object convertedId = placementService.convert(
                newId,
                TypeDescriptor.forObject(newId),
                property.getContentIdType(entity));
            property.setContentId(entity, convertedId, null);
        }

        S3ObjectId s3ObjectId = getS3ObjectId(entity, path, property);

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
            .bucket(s3ObjectId.getBucket())
            .contentLength(contentLen)
            .key(s3ObjectId.getKey());

        Object mimeType = property.getMimeType(entity);
        if (mimeType != null) {
            requestBuilder.contentType(mimeType.toString());
        }

        CompletableFuture<PutObjectResponse> future = asyncClient.putObject(
            requestBuilder.build(),
            AsyncRequestBody.fromPublisher(buffer)
        );

        return Mono.fromFuture(future)
            .map((response) -> {
                try {
                    property.setContentId(entity, s3ObjectId.getKey(), null);
                } catch (Exception e) {
                    logger.error("Error setting content id " + s3ObjectId.getKey(), e);
                    throw e;
                }
                try {
                    property.setContentLength(entity, contentLen);
                } catch (Exception e) {
                    logger.error("Error setting content length " + contentLen, e);
                    throw e;
                }
                return entity;
            });
    }

    @Override
    public Flux<ByteBuffer> getContent(@NonNull S entity, PropertyPath path) {
        StoreEntityValidation.requireEntity(entity);

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), path.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", path.name()));
        }

        if (property.getContentId(entity) == null) {
            return Flux.empty();
        }

        S3ObjectId s3ObjectId = getS3ObjectId(entity, path, property);
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(s3ObjectId.getBucket())
            .key(s3ObjectId.getKey())
            .build();

        CompletableFuture<ResponsePublisher<GetObjectResponse>> responseFuture =
            asyncClient.getObject(request, AsyncResponseTransformer.toPublisher());

        ResponsePublisher<GetObjectResponse> responsePublisher = responseFuture.join();
        return Flux.from(responsePublisher);
    }

    @Transactional
    @Override
    public Mono<S> unsetContent(@NonNull S entity, PropertyPath propertyPath) {
        StoreEntityValidation.requireEntity(entity);

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.name());
        if (property == null) {
            throw new StoreAccessException(String.format("Content property %s does not exist", propertyPath.name()));
        }

        if (property.getContentId(entity) == null) {
            return Mono.just(entity);
        }

        S3ObjectId s3ObjectId = getS3ObjectId(entity, propertyPath, property);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
            .bucket(s3ObjectId.getBucket())
            .key(s3ObjectId.getKey())
            .build();

        CompletableFuture<DeleteObjectResponse> future = asyncClient.deleteObject(deleteRequest);

        return Mono.fromFuture(future)
            .map((response) -> {
                property.setContentId(entity, null, descriptor -> {
                    for (Annotation annotation : descriptor.getAnnotations()) {
                        String canonicalName = annotation.annotationType().getCanonicalName();
                        if ("jakarta.persistence.Id".equals(canonicalName)
                            || "org.springframework.data.annotation.Id".equals(canonicalName)) {
                            return false;
                        }
                    }
                    return true;
                });
                property.setContentLength(entity,
                    BeanUtils.getDefaultValueForType(property.getContentLengthType().getType()));
                return entity;
            });
    }
}
