package internal.org.springframework.content.rest.mappings;

import internal.org.springframework.content.rest.annotations.StoreAwareController;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.net.URI;

import static java.lang.String.format;

public class StoreAwareHandlerMapping extends RequestMappingHandlerMapping {

    private RestConfiguration configuration;

    private String prefix;

    public StoreAwareHandlerMapping(RestConfiguration configuration) {
        Assert.notNull(configuration, "RestConfiguration must not be null!");
        this.configuration = configuration;
    }

    public RestConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {

        RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

        if (info == null) {
            return null;
        }

        PathPatternsRequestCondition pathPatterns = info.getPathPatternsCondition();
        if (pathPatterns == null) {
            return info;
        }

        String[] augmentedPatterns = pathPatterns.getPatternValues().stream()
                .map(pattern -> prefix.concat(pattern))
                .toArray(String[]::new);

        return info.mutate().paths(augmentedPatterns).build();
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {

        Class<?> type = ClassUtils.getUserClass(beanType);

        return type.isAnnotationPresent(StoreAwareController.class);
    }

    @Override
    public void afterPropertiesSet() {

        URI baseUri = configuration.getBaseUri();

        if (baseUri.isAbsolute()) {
            throw new UnsupportedOperationException(format("absolute base URIs not supported %s", baseUri));
        } else {
            this.prefix = baseUri.toString();
        }

        super.afterPropertiesSet();
    }
}
