package org.springframework.content.renditions.renderers;

import static org.assertj.core.api.Assertions.assertThat;


import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.renditions.RenditionProvider;

public class JpegToPngRenditionProviderTest {

	private RenditionProvider service;

	@BeforeEach
	public void setUp() {
		service = new JpegToPngRenditionProvider();
	}

	@Test
	public void testCanConvert() {
		assertThat(service.consumes()).isEqualTo("image/jpeg");
		assertThat(Arrays.asList(service.produces())).contains("image/png");
	}

	@Test
	public void testConvert() throws IOException {
		InputStream converted = service.convert(this.getClass().getResourceAsStream("/sample.jpeg"), "image/png");

		assertThat(converted.available()).isGreaterThan(0);
		assertThat(((ObservableInputStream)converted).getObservers()).anySatisfy(item -> { assertThat(item).isInstanceOf(FileRemover.class); });

		BufferedImage expectedImage = ImageIO.read(this.getClass().getResourceAsStream("/sample.png"));
		byte[] expectedRastaData = ((DataBufferByte) expectedImage.getData().getDataBuffer()).getData();

		BufferedImage actualImage = ImageIO.read(converted);
		byte[] actualRastaData = ((DataBufferByte) actualImage.getData().getDataBuffer()).getData();

        assertThat(expectedRastaData).isEqualTo(actualRastaData);
	}
}
