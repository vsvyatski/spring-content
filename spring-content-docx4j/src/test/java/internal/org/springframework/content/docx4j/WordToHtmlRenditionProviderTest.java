package internal.org.springframework.content.docx4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class WordToHtmlRenditionProviderTest {

    private RenditionProvider service;

    @BeforeEach
    void setUp() {
        service = new WordToHtmlRenditionProvider();
    }

    @Test
    void consumesDocxAndProducesHtml() {
        assertThat(service.consumes()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(service.produces()).contains("text/html");
    }

    @Test
    void convertsDocxToHtml() throws Exception {
        InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx.docx"), "text/html");

        assertThat(converted.available()).isGreaterThan(0);
        assertThat(((ObservableInputStream) converted).getObservers()).anyMatch(observer -> observer instanceof FileRemover);

        Document doc = Jsoup.parse(converted, "UTF8", "http://example.com");
        assertThat(doc.getElementsByTag("html")).hasSize(1);
    }

}
