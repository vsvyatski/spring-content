package org.springframework.data.rest.extensions.entitycontent;

import internal.org.springframework.content.rest.contentservice.ContentStoreContentService;
import internal.org.springframework.content.rest.controllers.BadRequestException;
import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import internal.org.springframework.content.rest.controllers.resolvers.AssociativeStoreResourceResolver;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext;
import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToRequestMappingContext;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import internal.org.springframework.content.rest.utils.ControllerUtils;
import internal.org.springframework.content.rest.utils.StoreUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.core.mapping.ResourceType;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.*;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RepositoryRestController
public class RepositoryEntityMultipartController {

    private static final String ENTITY_POST_MAPPING = "/{repository}";

    private final RestConfiguration restConfig;
    private final RepositoryInvokerFactory repoInvokerFactory;
    private final ContentPropertyToRequestMappingContext requestMappingContext;
    private final MappingContext mappingContext;
    private final ContentPropertyToExportedContext exportedMappingContext;
    private final StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;
    private final Stores stores;
    private final SelfLinkProvider selfLinkProvider;
    private final HttpHeadersPreparer headersPreparer;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public RepositoryEntityMultipartController(RestConfiguration restConfig, RepositoryInvokerFactory repoInvokerFactory, ContentPropertyToRequestMappingContext requestMappingContext, SelfLinkProvider selfLinkProvider, Stores stores, MappingContext mappingContext, ContentPropertyToExportedContext exportedMappingContext, StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler, @Qualifier("entityMultipartHttpMessageConverterConfigurer") RepositoryRestConfigurer configurer, HttpHeadersPreparer headersPreparer, ApplicationEventPublisher publisher) {
        this.restConfig = restConfig;
        this.repoInvokerFactory = repoInvokerFactory;
        this.requestMappingContext = requestMappingContext;
        this.selfLinkProvider = selfLinkProvider;
        this.stores = stores;
        this.mappingContext = mappingContext;
        this.exportedMappingContext = exportedMappingContext;
        this.byteRangeRestRequestHandler = byteRangeRestRequestHandler;
        this.headersPreparer = headersPreparer;
        this.publisher = publisher;
    }

    @ResponseBody
    @PostMapping(value = ENTITY_POST_MAPPING, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> createEntityAndContent(RootResourceInformation repoInfo, PersistentEntityResource payload, PersistentEntityResourceAssembler assembler, @PathVariable("repository") String repository, @RequestHeader HttpHeaders headers, MultipartHttpServletRequest req, HttpServletResponse resp)
        throws IOException, MethodNotAllowedException, HttpRequestMethodNotSupportedException {

        repoInfo.verifySupportedMethod(HttpMethod.POST, ResourceType.COLLECTION);

        Class<?> domainType = repoInfo.getDomainType();

        Object savedEntity = payload.getContent();

        String pathInfo = new UrlPathHelper().getPathWithinApplication(req);
        pathInfo = StoreUtils.storeLookupPath(pathInfo, restConfig.getBaseUri());

        String[] pathSegments = pathInfo.split("/");
        if (pathSegments.length < 2) {
            throw new BadRequestException();
        }

        String store = pathSegments[1];

        BeforeCreateEvent beforeCreate = new BeforeCreateEvent(savedEntity);
        publisher.publishEvent(beforeCreate);
        // Save the entity and re-assign the result to savedEntity, so that it exists in the repository before content is added to it.
        savedEntity = repoInvokerFactory.getInvokerFor(domainType).invokeSave(savedEntity);

        AfterCreateEvent afterCreate = new AfterCreateEvent(savedEntity);
        publisher.publishEvent(afterCreate);

        StoreInfo info = this.stores.getStore(Store.class, StoreUtils.withStorePath(store));
        if (info != null) {
            ContentStoreContentService service = new ContentStoreContentService(restConfig, repoInvokerFactory.getInvokerFor(domainType), mappingContext, exportedMappingContext, byteRangeRestRequestHandler);
            MultiValueMap<String, MultipartFile> files = req.getMultiFileMap();
            for (String path : files.keySet()) {
                MultipartFile file = files.get(path).get(0);

                String resolvedPath = file.getName();
                if (this.requestMappingContext.hasInverseMapping(domainType, file.getName())) {
                    resolvedPath = this.requestMappingContext.getInverseMappings(domainType).get(file.getName());
                }
                Resource storeResource = new AssociativeStoreResourceResolver(mappingContext).resolve(new InternalWebRequest(req, resp), info, savedEntity, PropertyPath.from(resolvedPath));

                headers.setContentLength(file.getSize());
                service.setContent(req, resp, headers, new InputStreamResourceWithFilename(file.getInputStream(), file.getOriginalFilename()), MediaType.parseMediaType(file.getContentType()), storeResource);
            }
        }

        Optional<PersistentEntityResource> resource = Optional.of(assembler.toFullResource(savedEntity));
        headers.setContentType(new MediaType("application", "hal+json"));

        HttpHeaders respHeaders = headersPreparer.prepareHeaders(resource);
        String selfLink = selfLinkProvider.createSelfLinkFor(savedEntity).withSelfRel().expand(ArrayUtils.EMPTY_OBJECT_ARRAY).getHref();
        respHeaders.setLocation(UriTemplate.of(selfLink).expand());
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, respHeaders, resource);
    }

    private record InternalWebRequest(MultipartHttpServletRequest req,
                                      HttpServletResponse resp) implements NativeWebRequest {

        @Override
        public @NonNull Object getNativeRequest() {
            return req;
        }

        @Override
        public Object getNativeResponse() {
            return resp;
        }

        @Override
        public <T> T getNativeRequest(Class<T> requiredType) {
            return (T) req;
        }

        @Override
        public <T> T getNativeResponse(Class<T> requiredType) {
            return (T) resp;
        }

        @Override
        public String getHeader(@NonNull String headerName) {
            return req.getHeader(headerName);
        }

        @Override
        public String[] getHeaderValues(@NonNull String headerName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull Iterator<String> getHeaderNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getParameter(@NonNull String paramName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getParameterValues(@NonNull String paramName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull Iterator<String> getParameterNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull Map<String, String[]> getParameterMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull Locale getLocale() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull String getContextPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getRemoteUser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUserInRole(@NonNull String role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkNotModified(long lastModifiedTimestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkNotModified(@NonNull String etag) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull String getDescription(boolean includeClientInfo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(@NonNull String name, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(@NonNull String name, @NonNull Object value, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(@NonNull String name, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getAttributeNames(int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerDestructionCallback(@NonNull String name, @NonNull Runnable callback, int scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object resolveReference(@NonNull String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull String getSessionId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull Object getSessionMutex() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InputStreamResourceWithFilename extends InputStreamResource {

        private final String filename;

        public InputStreamResourceWithFilename(InputStream inputStream, String filename) {
            super(inputStream);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
