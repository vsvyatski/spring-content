package internal.org.springframework.content.rest.mappings;

import internal.org.springframework.content.rest.annotations.ContentRestController;
import internal.org.springframework.content.rest.controllers.resolvers.EntityResolvers;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyRequest;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import internal.org.springframework.content.rest.utils.StoreUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.content.rest.config.RestConfiguration.Exclusions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

public class ContentHandlerMapping extends StoreAwareHandlerMapping {

    private static final MediaType HAL_JSON = MediaType.parseMediaType("application/hal+json");
    private final ContentPropertyToRequestMappingContext requestMappingContext;
    private Exclusions exclusions;
    private final Stores contentStores;
    private final EntityResolvers entityResolvers;

    public ContentHandlerMapping(Stores contentStores, EntityResolvers entityResolvers, ContentPropertyToRequestMappingContext requestMappingContext, RestConfiguration config) {
        super(config);
        this.contentStores = contentStores;
        this.entityResolvers = entityResolvers;
        this.requestMappingContext = requestMappingContext;
        initExclusions(config);
        setOrder(Ordered.LOWEST_PRECEDENCE - 200);
    }

    private void initExclusions(RestConfiguration config) {
        this.exclusions = config.shortcutExclusions();
        if (this.exclusions.isEmpty()) {
            this.exclusions
                .exclude("GET", MediaType.APPLICATION_JSON)
                .exclude("GET", HAL_JSON)
                .exclude("DELETE", MediaType.APPLICATION_JSON)
                .exclude("DELETE", HAL_JSON)
                .exclude("PUT", MediaType.APPLICATION_JSON)
                .exclude("PUT", HAL_JSON)
                .exclude("POST", MediaType.APPLICATION_JSON)
                .exclude("POST", HAL_JSON)
                .exclude("PATCH", MediaType.ALL)
                .exclude("HEAD", MediaType.ALL);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#
     * isHandler(java.lang.Class)
     */
    @Override
    protected boolean isHandler(Class<?> beanType) {
        return AnnotationUtils.findAnnotation(beanType, ContentRestController.class) != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#
     * lookupHandlerMethod(java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected HandlerMethod lookupHandlerMethod(@NonNull String lookupPath, @NonNull HttpServletRequest request)
        throws Exception {

        String storeLookupPath = StoreUtils.storeLookupPath(lookupPath, this.getConfiguration().getBaseUri());

        if (storeLookupPath != null) {
            // is a content property, if so look up a handler method?
            String[] path = storeLookupPath.split("/");
            if (path.length < 3)
                return null;

            StoreInfo info2 = contentStores.getStore(Store.class, StoreUtils.withStorePath(path[1]));
            if (info2 != null) {

                if (isFullyQualifiedContentPropertyRequest(path, info2)) {

                    String resolvedContentPropertyPath = requestMappingContext.resolveContentPropertyPath(info2.getDomainObjectClass(), ContentPropertyRequest.from(storeLookupPath).contentPropertyPath());
                    String resolvedStoreLookupPath = ContentPropertyRequest.from(path[1], path[2], resolvedContentPropertyPath).getRequestURI();

                    if (entityResolvers.hasPropertyFor(resolvedStoreLookupPath)) {
                        return super.lookupHandlerMethod(lookupPath, request);
                    }
                } else if (this.getConfiguration().shortcutLinks() && !isExcludedShortcutContentPropertyRequest(request)) {
                    // for backward compatibility
                    return super.lookupHandlerMethod(lookupPath, request);
                }
            }
        }
        return null;
    }

    @Override
    protected boolean hasCorsConfigurationSource(@NonNull Object handler) {
        return true;
    }

    @Override
    protected CorsConfiguration getCorsConfiguration(@NonNull Object handler, @NonNull HttpServletRequest request) {
        String lookupPath = ServletRequestPathUtils.getCachedPathValue(request);
        String storeLookupPath = StoreUtils.storeLookupPath(lookupPath, this.getConfiguration().getBaseUri());
        CorsConfiguration corsConfiguration = super.getCorsConfiguration(handler, request);

        if (storeLookupPath == null) {
            return corsConfiguration;
        }

        String[] path = storeLookupPath.split("/");
        if (path.length < 3)
            return corsConfiguration;

        StoreInfo info2 = contentStores.getStore(Store.class, StoreUtils.withStorePath(path[1]));
        if (info2 == null) {
            return corsConfiguration;
        }

        CorsConfigurationBuilder builder = new CorsConfigurationBuilder();
        CorsConfiguration storeCorsConfiguration = builder.build(info2.getInterface());

        return corsConfiguration == null ? storeCorsConfiguration : corsConfiguration.combine(storeCorsConfiguration);
    }

    private boolean isExcludedShortcutContentPropertyRequest(HttpServletRequest request) {
        String method = request.getMethod();

        List<MediaType> excludedMediaTypes = this.exclusions.get(method);
        if (excludedMediaTypes == null) {
            return false;
        }

        String mediaTypes = MediaType.ALL_VALUE;
        if ("GET".equals(method) || "DELETE".equals(method)) {
            mediaTypes = request.getHeader("Accept");
        } else if ("PUT".equals(method) || "POST".equals(method)) {
            mediaTypes = request.getHeader("Content-Type");
        }

        List<MediaType> acceptedMediaTypes = MediaType.parseMediaTypes(mediaTypes);

        for (MediaType excludedMediaType : excludedMediaTypes) {
            for (MediaType acceptedMediaType : acceptedMediaTypes) {
                if (excludedMediaType.includes(acceptedMediaType)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isFullyQualifiedContentPropertyRequest(String[] path, StoreInfo info2) {
        return AssociativeStore.class.isAssignableFrom(info2.getInterface()) && path.length >= 4;
    }

    @Override
    protected void detectHandlerMethods(final @NonNull Object handler) {
        super.detectHandlerMethods(handler);
    }

    /**
     * Store requests have to be handled by different RequestMappings based on whether the
     * request is targeting a Store or content associated with an Entity
     */
    @Override
    protected RequestCondition<?> getCustomMethodCondition(@NonNull Method method) {
        StoreType typeAnnotation = AnnotationUtils.findAnnotation(method,
            StoreType.class);
        if (typeAnnotation != null) {
            return new StoreCondition(typeAnnotation, this.contentStores, this.getConfiguration().getBaseUri());
        }
        return null;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface StoreType {

        String STORE = "store";

        String CONTENT_STORE = "contentStore";

        String value() default STORE;
    }

    public static class StoreCondition implements RequestCondition<StoreCondition> {

        private final String storeType;
        private final Stores stores;
        private final URI baseUri;

        public StoreCondition(StoreType typeAnnotation, Stores stores, URI baseUri) {
            this.storeType = typeAnnotation.value();
            this.stores = stores;
            this.baseUri = baseUri;
        }

        @Override
        public @NonNull StoreCondition combine(@NonNull StoreCondition other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StoreCondition getMatchingCondition(@NonNull HttpServletRequest request) {
            return isMappingForRequest(request) ? this : null;
        }

        @Override
        public int compareTo(@NonNull StoreCondition other, @NonNull HttpServletRequest request) {
            boolean thisMatches = isMappingForRequest(request);
            boolean otherMatches = other.isMappingForRequest(request);
            if (thisMatches && !otherMatches) {
                return 1;
            }
            if (!thisMatches && otherMatches) {
                return -1;
            }
            String storeLookupPath = storeLookupPath(request);
            if (storeLookupPath == null) {
                return 0;
            }
            String extension = FilenameUtils.getExtension(FilenameUtils.getName(storeLookupPath));
            if (extension.isEmpty()) {
                return 0;
            }
            if (StoreType.STORE.equals(storeType)) {
                return -1;
            }
            if (StoreType.CONTENT_STORE.equals(storeType)) {
                return 1;
            }
            return 0;
        }

        public boolean isMappingForRequest(HttpServletRequest request) {
            String storeLookupPath = storeLookupPath(request);
            if (storeLookupPath == null) {
                return false;
            }
            String[] segments = storeLookupPath.split("/");
            if (segments.length < 3) {
                return false;
            }
            StoreInfo info = stores.getStore(Store.class, StoreUtils.withStorePath(segments[1]));
            return matchesStoreInterface(info);
        }

        private String storeLookupPath(HttpServletRequest request) {
            String path = new UrlPathHelper().getPathWithinApplication(request);
            return StoreUtils.storeLookupPath(path, baseUri);
        }

        private boolean matchesStoreInterface(StoreInfo info) {
            if (info == null) {
                return false;
            }
            Class<?> storeInterface = info.getInterface();
            return (Store.class.isAssignableFrom(storeInterface) && StoreType.STORE.equals(storeType))
                || (ContentStore.class.isAssignableFrom(storeInterface) && StoreType.CONTENT_STORE.equals(storeType));
        }
    }
}
