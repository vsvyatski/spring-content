package org.springframework.content.rest.config;

import internal.org.springframework.content.rest.utils.StoreUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.CacheControl;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class StoreCacheControlInterceptor implements HandlerInterceptor {

    private final List<CacheControlRule> cacheControlRules = new ArrayList<>();
    private URI baseUri;

    public StoreCacheControlInterceptor() {
    }

    public StoreCacheControlConfigurer configurer() {
        return new StoreCacheControlConfigurer(this);
    }

    public void addCacheControlRule(CacheControlRule cacheControlRule) {
        cacheControlRules.add(cacheControlRule);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        if (!"GET".equals(request.getMethod())) {
            return true;
        }

        UrlPathHelper pathHelper = UrlPathHelper.defaultInstance;

        String lookupPath = pathHelper.getLookupPathForRequest(request);
        String storeLookupPath = StoreUtils.storeLookupPath(lookupPath, baseUri);

        for (CacheControlRule rule : cacheControlRules) {

            if (rule.match(storeLookupPath)) {
                response.addHeader("Cache-Control", rule.cacheControl().getHeaderValue());
            }
        }

        return true;
    }

    public void setBaseUri(URI baseUri) {
        this.baseUri = baseUri;
    }

    public record CacheControlRule(String pattern, CacheControl cacheControl) {

        private static final AntPathMatcher matcher = new AntPathMatcher();

        public boolean match(String path) {
            return matcher.match(pattern, path);
        }
    }

    public static class StoreCacheControlConfigurer {

        private final StoreCacheControlInterceptor interceptor;

        public StoreCacheControlConfigurer(StoreCacheControlInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        public StoreCacheControlConfigurer antMatcher(String pattern, CacheControl cacheControl) {
            interceptor.addCacheControlRule(new CacheControlRule(pattern, cacheControl));
            return this;
        }
    }
}
