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
        assertThat(service.consumes()).isEqualTo(Docx4jMimeTypes.WORDPROCESSINGML);
        assertThat(service.produces()).contains(Docx4jMimeTypes.TEXT_HTML);
    }

    @Test
    void convertsDocxToHtml() throws Exception {
        InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx.docx"),
            Docx4jMimeTypes.TEXT_HTML);

        assertThat(converted.available()).isGreaterThan(0);
        assertThat(((ObservableInputStream) converted).getObservers()).anyMatch(observer -> observer instanceof FileRemover);

        Document doc = Jsoup.parse(converted, "UTF8", "http://example.com");
        assertThat(doc.getElementsByTag("html")).hasSize(1);
    }
}
