package org.springframework.content.renditions.boot;

import internal.org.springframework.content.docx4j.WordToHtmlRenditionProvider;
import internal.org.springframework.content.docx4j.WordToPdfRenditionProvider;
import internal.org.springframework.content.docx4j.WordToTextRenditionProvider;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.renditions.renderers.JpegToPngRenditionProvider;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.content.renditions.renderers.TextplainToJpegRenderer;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentRenditionsAutoConfigurationTest {

    @Nested
    class ContentRenditionsAutoConfiguration {
        @Nested
        class GivenADefaultConfiguration {
            @Test
            void shouldLoadTheAllRenderers() {
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.register(TestConfig.class);
                context.refresh();

                assertThat(context.getBean(PdfToJpegRenderer.class)).isNotNull();
                assertThat(context.getBean(TextplainToJpegRenderer.class)).isNotNull();
                assertThat(context.getBean(WordToJpegRenderer.class)).isNotNull();

                assertThat(context.getBean(JpegToPngRenditionProvider.class)).isNotNull();
                assertThat(context.getBean(WordToHtmlRenditionProvider.class)).isNotNull();
                assertThat(context.getBean(WordToPdfRenditionProvider.class)).isNotNull();
                assertThat(context.getBean(WordToTextRenditionProvider.class)).isNotNull();

                context.close();
            }
        }
    }

    @Configuration
    @AutoConfigurationPackage
    @EnableAutoConfiguration(exclude = {SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
    @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
    public static class TestConfig {
    }
}
