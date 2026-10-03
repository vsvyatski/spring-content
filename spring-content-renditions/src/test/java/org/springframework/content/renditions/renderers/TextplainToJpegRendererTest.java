package org.springframework.content.renditions.renderers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class TextplainToJpegRendererTest {

	private boolean wrapText = false;
	private RenditionProvider renderer;

	private InputStream input, result;
	private String mimeType;

	private Exception e;

    @Nested
    class TextplainToJpegRendererCases {
        @Nested
        class Consumes {
            @BeforeEach
            void setUp() {
                renderer = new TextplainToJpegRenderer(wrapText);
            }

            @Test
            void shouldReturnTextPlain() {
                assertThat(renderer.consumes()).isEqualTo("text/plain");
            }

        }

        @Nested
        class Produces {
            @BeforeEach
            void setUp() {
                renderer = new TextplainToJpegRenderer(wrapText);
            }

            @Test
            void shouldReturnJpegMimetype() {
                assertThat(renderer.produces()).contains("image/jpg");
                assertThat(renderer.produces()).contains("image/jpeg");
            }

        }

        @Nested
        class Convert {
            @Nested
            class GivenAPlainTextInput {
                @Nested
                class GivenASingleLineInput {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                        		"Hello Spring Content World!".getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                        	result = renderer.convert(input, mimeType);
                        }
                        catch (Exception e) {
                        	TextplainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() {
                        InputStream expected = this.getClass().getResourceAsStream(
                        		"/textplaintorenderer/single-line.jpeg");
                        assertThat(expected).isNotNull();
                        assertThat(result).isNotNull();
                        // InputStream expected =
                        // this.getClass().getResourceAsStream("/textplaintorenderer/single-line.jpeg");
                        // assertThat(expected).isNotNull();
                        // assertThat(IOUtils.contentEquals(expected, result),
                        // is(true));
                    }

                }

                @Nested
                class GivenAMultiLineInput {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                        		"Hello\nSpring\n\nContent\n\n\nWorld!".getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                        	result = renderer.convert(input, mimeType);
                        }
                        catch (Exception e) {
                        	TextplainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() {
                        assertThat(result).isNotNull();
                        // assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/multi-line.jpeg"),
                        // result)).isTrue();
                    }

                }

                @Nested
                class GivenALongLineAndWrapping {
                    @BeforeEach
                    void setUp() {
                        wrapText = true;
                        input = new ByteArrayInputStream(
                        		"Hello Spring Content World!  This is a really long line that we expect to wrap"
                        				.getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                        	result = renderer.convert(input, mimeType);
                        }
                        catch (Exception e) {
                        	TextplainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() {
                        assertThat(result).isNotNull();
                        // assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/wrapped-line.jpeg"),
                        // result)).isTrue();
                    }

                }

                @Nested
                class GivenALongLineAndNoWrapping {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                        		"Hello Spring Content World!  This is a really long line that we expect to wrap"
                        				.getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                        	result = renderer.convert(input, mimeType);
                        }
                        catch (Exception e) {
                        	TextplainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() {
                        assertThat(result).isNotNull();
                        // assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/overflowed-line.jpeg"),
                        // result)).isTrue();
                    }

                }

                @Nested
                class GivenALineFileWillOverflowTheImageSize {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                        		"Hello\n\nSpring\n\nContent\n\nWorld!\n\n\nThis\n\nis\n\na\n\nreally\n\nreally\n\nreally\n\nreally\n\nreally\n\nlong\n\nfile\n\nthat\n\nwill\n\noverflow\n\nthe\n\nimage"
                        				.getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                        	result = renderer.convert(input, mimeType);
                        }
                        catch (Exception e) {
                        	TextplainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() {
                        assertThat(result).isNotNull();
                        // assertThat(IOUtils.contentEquals(this.getClass().getResourceAsStream("/textplaintorenderer/overflowed-image.jpeg"),
                        // result)).isTrue();
                    }

                }

            }

            @Nested
            class WhenTheInputStreamIsNotAValidWordFile {
                @BeforeEach
                void setUp() {
                    input = this.getClass().getResourceAsStream("/sample-docx.docx");

                    renderer = new TextplainToJpegRenderer(wrapText);

                    try {
                    	result = renderer.convert(input, mimeType);
                    }
                    catch (Exception e) {
                    	TextplainToJpegRendererTest.this.e = e;
                    }
                }

                @Test
                void shouldNotError() {
                    assertThat(e).isNull();
                }

            }

            @Nested
            class GivenANullInputStream {
                @BeforeEach
                void setUp() {
                    renderer = new TextplainToJpegRenderer(wrapText);

                    try {
                    	result = renderer.convert(input, mimeType);
                    }
                    catch (Exception e) {
                    	TextplainToJpegRendererTest.this.e = e;
                    }
                }

                @Test
                void shouldReturnAnError() {
                    assertThat(e).isNotNull();
                }

            }

        }

    }

}
