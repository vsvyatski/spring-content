package internal.org.springframework.content.rest.links;

import internal.org.springframework.content.rest.links.linkrel.LinkRelBaseUriConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebAppConfiguration
@ContextConfiguration(classes = {
        LinkRelBaseUriConfig.class,
        DelegatingWebMvcConfiguration.class,
        RepositoryRestMvcConfiguration.class,
        RestConfiguration.class,
        HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
@ExtendWith(SpringExtension.class)
@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.INHERIT)
public @interface ContentLinkRelIntegrationTest {
}