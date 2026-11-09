package internal.org.springframework.content.docx4j;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class WordToTextRenditionProviderTest {

    private RenditionProvider service;

    @BeforeEach
    void setUp() {
        service = new WordToTextRenditionProvider();
    }

    @Test
    void consumesDocxAndProducesPlainText() {
        assertThat(service.consumes()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(service.produces()).contains("text/plain");
    }

    @Test
    void convertsDocxToText() throws Exception {
        InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx2.docx"), "text/plain");

        assertThat(converted.available()).isGreaterThan(0);
        assertThat(((ObservableInputStream) converted).getObservers()).anyMatch(observer -> observer instanceof FileRemover);

        assertThat(IOUtils.toString(converted, StandardCharsets.UTF_8))
                .isEqualTo("This is the Document Title\n\n and this is the document body.\n\n\n");
    }

}
