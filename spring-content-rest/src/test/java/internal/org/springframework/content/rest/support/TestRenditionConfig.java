package internal.org.springframework.content.rest.support;

import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers shared test {@link RenditionProvider} beans for REST IT contexts.
 *
 * @author Vladimir Syatsky
 */
@Configuration
public class TestRenditionConfig {

    @Bean
    public RenditionProvider textToHtml() {
        return TestRenditionProviders.textPlainToHtml();
    }

    @Bean
    public RenditionProvider htmlToHtml() {
        return TestRenditionProviders.htmlToHtml();
    }
}