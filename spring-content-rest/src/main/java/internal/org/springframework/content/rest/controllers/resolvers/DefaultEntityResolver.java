package internal.org.springframework.content.rest.controllers.resolvers;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;
import internal.org.springframework.content.rest.utils.RepositoryUtils;
import internal.org.springframework.content.rest.utils.StoreUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;

public class DefaultEntityResolver implements EntityResolver {
    private static final Logger logger = LoggerFactory.getLogger(DefaultEntityResolver.class);
    private static boolean ROOT_RESOURCE_INFORMATION_CLASS_PRESENT = false;

    static {
        try {
            ROOT_RESOURCE_INFORMATION_CLASS_PRESENT = DefaultEntityResolver.class.getClassLoader().loadClass(
                "org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver"
            ) != null;
        } catch (ClassNotFoundException ignored) {
        }
    }

    private final ApplicationContext context;
    private final Repositories repositories;
    private final Stores stores;
    private final ConversionService converters;
    private final String mapping;
    private final MappingContext mappingContext;

    public DefaultEntityResolver(ApplicationContext context, Repositories repositories, Stores stores, ConversionService converters, String mapping, MappingContext mappingContext) {
        this.context = context;
        this.repositories = repositories;
        this.stores = stores;
        this.converters = converters;
        this.mapping = mapping;
        this.mappingContext = mappingContext;
    }

    public static NativeWebRequest nativeWebRequestForGetItemResource(String requestUri) {
        return new ServletWebRequest(new GetItemResourceHttpServletRequest(requestUri));
    }

    @Override
    public String getMapping() {
        return this.mapping;
    }

    @Override
    public EntityResolution resolve(String pathInfo) {

        AntPathMatcher matcher = new AntPathMatcher();
        Map<String, String> variables = matcher.extractUriTemplateVariables(this.mapping, pathInfo);
        String repository = variables.get("repository");
        String id = variables.get("id");

        String[] pathSegments = pathInfo.split("/");
        String store = pathSegments[1];

        StoreInfo info = this.stores.getStore(Store.class, StoreUtils.withStorePath(store));
        if (info == null) {
            throw new IllegalArgumentException(String.format("Store for path %s not found", store));
        }

        Object domainObj;
        try {
            try {
                domainObj = findOne(repositories, info, info.getDomainObjectClass(), repository, id);
            } catch (IllegalArgumentException iae) {
                domainObj = findOne(repositories, info, repository, id);
            }
        } catch (HttpRequestMethodNotSupportedException e) {
            throw new ResourceNotFoundException();
        }

        String propertyPath = matcher.extractPathWithinPattern(this.mapping, pathInfo);
        if (!StringUtils.hasText(propertyPath)) {
            ContentProperty property = selectPrimaryContentProperty(info);
            if (property == null) {
                throw new ResourceNotFoundException();
            }
            propertyPath = property.getContentPropertyPath();
        }

        return new EntityResolution(domainObj, PropertyPath.from(propertyPath));
    }

    @Override
    public boolean hasPropertyFor(String pathInfo) {

        AntPathMatcher matcher = new AntPathMatcher();
        String[] pathSegments = pathInfo.split("/");
        String store = pathSegments[1];

        StoreInfo info = this.stores.getStore(Store.class, StoreUtils.withStorePath(store));
        if (info == null) {
            throw new IllegalArgumentException(String.format("Store for path %s not found", store));
        }

        String propertyPath = matcher.extractPathWithinPattern(this.mapping, pathInfo);
        if (!StringUtils.hasText(propertyPath)) {
            propertyPath = "";
        }

        return mappingContext.getContentProperty(info.getDomainObjectClass(), propertyPath) != null;
    }

    public Object findOne(Repositories repositories, StoreInfo info, String repository, String id)
        throws HttpRequestMethodNotSupportedException {

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, repository);

        if (ri == null) {
            throw new ResourceNotFoundException();
        }

        Class<?> domainObjClazz = ri.getDomainType();

        return findOne(repositories, info, domainObjClazz, repository, id);
    }

    public Object findOne(Repositories repositories, StoreInfo info, Class<?> domainObjClass, String repository, Serializable id)
        throws HttpRequestMethodNotSupportedException {

        Optional<Object> domainObj = Optional.empty();

        if (ROOT_RESOURCE_INFORMATION_CLASS_PRESENT) {

            RepositoryInvoker invoker;
            try {
                invoker = resolveRootResourceInformation(info, repository, id, new ModelAndViewContainer());
                if (invoker != null) {
                    domainObj = invoker.invokeFindById(id);
                } else {
                    logger.warn("Could not resolve RootResourceInformation");
                }
            } catch (ConverterNotFoundException e) {
                logger.debug("invoking Repository findById(id) method failed");
                domainObj = findOneByReflection(repositories, domainObjClass, id);
            } catch (AccessDeniedException ace) {
                logger.debug("invoking Repository findById(id) method threw AccessDeniedException");
                throw ace;
            } catch (Exception e) {
                logger.error("invoking Repository findById(id) method threw exception", e.getCause());
            }
        } else {

            domainObj = findOneByReflection(repositories, domainObjClass, id);
        }
        return domainObj.orElseThrow(ResourceNotFoundException::new);
    }

    @SuppressWarnings("unchecked")
    public Optional<Object> findOneByReflection(Repositories repositories, Class<?> domainObjClass, Serializable id)
        throws HttpRequestMethodNotSupportedException {

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, domainObjClass);

        if (ri == null) {
            throw new ResourceNotFoundException();
        }

        Class<?> domainObjClazz = ri.getDomainType();
        Class<?> idClazz = ri.getIdType();

        Optional<Method> findOneMethod = ri.getCrudMethods().getFindOneMethod();
        if (findOneMethod.isEmpty()) {
            throw new HttpRequestMethodNotSupportedException("fineOne");
        }

        Serializable oid = id;
        if (converters.canConvert(String.class, idClazz)) {
            oid = (Serializable) converters.convert(id, idClazz);
        }

        Object repositoryBean = repositories.getRepositoryFor(domainObjClazz)
            .orElseThrow(ResourceNotFoundException::new);
        return (Optional<Object>) ReflectionUtils.invokeMethod(findOneMethod.get(), repositoryBean, oid);
    }

    private RepositoryInvoker resolveRootResourceInformation(StoreInfo info, String repository, Serializable id,
                                                             ModelAndViewContainer mavContainer) throws Exception {

        Method m = ReflectionUtils.findMethod(RepositoryEntityControllerFacade.class, "getItemResource", RootResourceInformation.class);
        if (m == null) {
            throw new IllegalStateException("getItemResource method not found on RepositoryEntityControllerFacade");
        }
        MethodParameter repoRequestMethodParameter = new MethodParameter(m, 0);

        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repositories, info.getDomainObjectClass());

        // this above lookup may fail when the path for a content store for a child entity is mapped to the same path as
        // repository path for the parent entity
        // this should probably not be allowed
        // when this is the case we perform an additional lookup using the repository variable from the URI
        if (ri == null) {
            ri = RepositoryUtils.findRepositoryInformation(repositories, repository);
        }

        if (ri == null) {
            throw new IllegalStateException(String.format("Unable to resolve root resource information for %s",
                String.join("/", repository, id.toString())));
        }

        String repo = RepositoryUtils.repositoryPath(ri);

        String repoUri = String.format("/%s/%s", repo, id);
        BaseUri baseUri = context.getBean(BaseUri.class);
        if (!baseUri.equals(BaseUri.NONE)) {
            repoUri = String.format("%s%s", baseUri.getUri(), repoUri);
        }

        NativeWebRequest repoRequestFacade = nativeWebRequestForGetItemResource(repoUri);
        RootResourceInformationHandlerMethodArgumentResolver rootResourceInfoResolver =
            context.getBean(RootResourceInformationHandlerMethodArgumentResolver.class);
        RootResourceInformation rri = rootResourceInfoResolver.resolveArgument(
            repoRequestMethodParameter, mavContainer, repoRequestFacade, null);
        if (rri == null) {
            return null;
        }
        return rri.getInvoker();
    }

    private @Nullable ContentProperty selectPrimaryContentProperty(StoreInfo info) {
        ContentProperty contentProperty = null;
        try {
            contentProperty = mappingContext.getContentProperties(info.getDomainObjectClass()).iterator().next();
        } catch (NoSuchElementException ignored) {
        }
        return contentProperty;
    }

    /**
     * Minimal servlet request for Spring Data REST item-resource lookup via
     * {@link org.springframework.web.util.UrlPathHelper#getLookupPathForRequest(jakarta.servlet.http.HttpServletRequest)}.
     */
    public static class GetItemResourceHttpServletRequest implements HttpServletRequest {

        private static final HttpServletMapping DEFAULT_SERVLET_MAPPING = new HttpServletMapping() {
            @Override
            public String getMatchValue() {
                return "";
            }

            @Override
            public String getPattern() {
                return "/*";
            }

            @Override
            public String getServletName() {
                return "dispatcherServlet";
            }

            @Override
            public MappingMatch getMappingMatch() {
                return MappingMatch.DEFAULT;
            }
        };

        private final String requestUri;

        public GetItemResourceHttpServletRequest(String requestUri) {
            this.requestUri = requestUri;
        }

        @Override
        public Object getAttribute(String name) {

            return null;
        }

        @Override
        public Enumeration<String> getAttributeNames() {

            return null;
        }

        @Override
        public String getCharacterEncoding() {

            return null;
        }

        @Override
        public void setCharacterEncoding(String env) {
        }

        @Override
        public int getContentLength() {
            return 0;
        }

        @Override
        public long getContentLengthLong() {
            return 0L;
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getProtocol() {
            return null;
        }

        @Override
        public String getScheme() {
            return null;
        }

        @Override
        public String getServerName() {
            return null;
        }

        @Override
        public int getServerPort() {
            return 0;
        }

        @Override
        public BufferedReader getReader() {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public String getRemoteHost() {
            return null;
        }

        @Override
        public void setAttribute(String name, Object o) {
        }

        @Override
        public void removeAttribute(String name) {
        }

        @Override
        public Locale getLocale() {
            return null;
        }

        @Override
        public Enumeration<Locale> getLocales() {
            return null;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        @Override
        public int getRemotePort() {
            return 0;
        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public String getLocalAddr() {
            return null;
        }

        @Override
        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {

            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return DispatcherType.REQUEST;
        }

        @Override
        public String getRequestId() {
            return null;
        }

        @Override
        public String getProtocolRequestId() {
            return null;
        }

        @Override
        public ServletConnection getServletConnection() {
            return null;
        }

        @Override
        public String getAuthType() {
            return null;
        }

        @Override
        public Cookie[] getCookies() {
            return null;
        }

        @Override
        public long getDateHeader(String name) {
            return 0;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return null;
        }

        @Override
        public int getIntHeader(String name) {
            return 0;
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public String getPathInfo() {
            return null;
        }

        @Override
        public String getPathTranslated() {
            return null;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        @Override
        public String getQueryString() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            return false;
        }

        @Override
        public Principal getUserPrincipal() {
            return null;
        }

        @Override
        public String getRequestedSessionId() {
            return null;
        }

        @Override
        public String getRequestURI() {
            return requestUri;
        }

        @Override
        public HttpServletMapping getHttpServletMapping() {
            return DEFAULT_SERVLET_MAPPING;
        }

        @Override
        public StringBuffer getRequestURL() {
            return null;
        }

        @Override
        public String getServletPath() {
            return "";
        }

        @Override
        public HttpSession getSession(boolean create) {
            return null;
        }

        @Override
        public HttpSession getSession() {
            return null;
        }

        @Override
        public String changeSessionId() {
            return null;
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        @Override
        public boolean authenticate(HttpServletResponse response) {
            return false;
        }

        @Override
        public void login(String username, String password) {
        }

        @Override
        public void logout() {
        }

        @Override
        public Collection<Part> getParts() {
            return null;
        }

        @Override
        public Part getPart(String name) {
            return null;
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
            return null;
        }
    }

    public static class RepositoryEntityControllerFacade {

        @RequestMapping(value = "/{repository}/{id}", method = RequestMethod.GET)
        @SuppressWarnings("unused")
        public ResponseEntity<EntityModel<?>> getItemResource(
            @QuerydslPredicate RootResourceInformation resourceInformation) {
            return null;
        }
    }
}
