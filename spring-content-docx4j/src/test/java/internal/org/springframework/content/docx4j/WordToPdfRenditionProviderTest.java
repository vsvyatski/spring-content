package internal.org.springframework.content.docx4j;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class WordToPdfRenditionProviderTest {

	private RenditionProvider service;

	@BeforeEach
	void setUp() {
		service = new WordToPdfRenditionProvider();
	}

	@Test
	void consumesDocxAndProducesPdf() {
		assertThat(service.consumes()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertThat(service.produces()).contains("application/pdf");
	}

	@Test
	void convertsDocxToPdf() throws Exception {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample-docx2.docx"), "application/pdf");

		assertThat(converted.available()).isGreaterThan(0);
		assertThat(((ObservableInputStream) converted).getObservers()).anyMatch(observer -> observer instanceof FileRemover);

		assertThat(pdfToText(converted).replaceAll("\\s+", " ").trim())
				.isEqualTo("This is the Document Title and this is the document body.");
	}

	private String pdfToText(InputStream in) throws Exception {
		try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
			return new PDFTextStripper().getText(doc);
		}
	}

}
