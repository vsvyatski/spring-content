package org.springframework.content.renditions.renderers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.renditions.RenditionProvider;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class TextPlainToJpegRendererTest {

    private final String toMimeType = "image/jpeg";
    private boolean wrapText = false;
    private RenditionProvider renderer;
    private InputStream input, result;
    private Exception e;

    static void assertJpegMatchesGoldenPng(InputStream jpeg, String goldenPngClasspath) throws IOException {
        assertThat(jpeg).isNotNull();
        BufferedImage expected = ImageIO.read(
            Objects.requireNonNull(TextPlainToJpegRendererTest.class.getResourceAsStream(goldenPngClasspath)));
        BufferedImage actual = ImageIO.read(jpeg);
        assertThat(actual).isNotNull();
        assertThat(expected).isNotNull();

        int width = expected.getWidth();
        int height = expected.getHeight();
        assertThat(actual.getWidth()).isEqualTo(width);
        assertThat(actual.getHeight()).isEqualTo(height);

        int[] expectedArgb = toIntRgb(expected).getRGB(0, 0, width, height, null, 0, width);
        int[] actualArgb = toIntRgb(actual).getRGB(0, 0, width, height, null, 0, width);
        assertThat(actualArgb).isEqualTo(expectedArgb);
    }

    private static BufferedImage toIntRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    @Nested
    class TextPlainToJpegRendererCases {
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
            @DisplayName("Given a plain text input")
            class PlainTextInput {
                @Nested
                @DisplayName("Given a single line input")
                class SingleLineInput {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                            "Hello Spring Content World!".getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                            result = renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            TextPlainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() throws Exception {
                        assertJpegMatchesGoldenPng(result, "/textplaintorenderer/single-line.png");
                    }
                }

                @Nested
                @DisplayName("Given a multi line input")
                class MultiLineInput {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                            "Hello\nSpring\n\nContent\n\n\nWorld!".getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                            result = renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            TextPlainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() throws Exception {
                        assertJpegMatchesGoldenPng(result, "/textplaintorenderer/multi-line.png");
                    }
                }

                @Nested
                @DisplayName("Given a long line and wrapping")
                class LongLineAndWrapping {
                    @BeforeEach
                    void setUp() {
                        wrapText = true;
                        input = new ByteArrayInputStream(
                            "Hello Spring Content World!  This is a really long line that we expect to wrap"
                                .getBytes());

                        renderer = new TextplainToJpegRenderer(true);

                        try {
                            result = renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            TextPlainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() throws Exception {
                        assertJpegMatchesGoldenPng(result, "/textplaintorenderer/wrapped-line.png");
                    }
                }

                @Nested
                @DisplayName("Given a long line and no wrapping")
                class LongLineAndNoWrapping {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                            "Hello Spring Content World!  This is a really long line that we expect to wrap"
                                .getBytes());

                        renderer = new TextplainToJpegRenderer(false);

                        try {
                            result = renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            TextPlainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() throws Exception {
                        assertJpegMatchesGoldenPng(result, "/textplaintorenderer/overflowed-line.png");
                    }
                }

                @Nested
                @DisplayName("Given a line file will overflow the image size")
                class LineFileWillOverflowImageSize {
                    @BeforeEach
                    void setUp() {
                        input = new ByteArrayInputStream(
                            "Hello\n\nSpring\n\nContent\n\nWorld!\n\n\nThis\n\nis\n\na\n\nreally\n\nreally\n\nreally\n\nreally\n\nreally\n\nlong\n\nfile\n\nthat\n\nwill\n\noverflow\n\nthe\n\nimage"
                                .getBytes());

                        renderer = new TextplainToJpegRenderer(wrapText);

                        try {
                            result = renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            TextPlainToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldProduceTheCorrectImage() throws Exception {
                        assertJpegMatchesGoldenPng(result, "/textplaintorenderer/overflowed-image.png");
                    }
                }
            }

            @Nested
            @DisplayName("When the input stream is not a valid word file")
            class InputStreamIsNotValidWordFile {
                @BeforeEach
                void setUp() {
                    input = this.getClass().getResourceAsStream("/sample-docx.docx");

                    renderer = new TextplainToJpegRenderer(wrapText);

                    try {
                        result = renderer.convert(input, toMimeType);
                    } catch (Exception e) {
                        TextPlainToJpegRendererTest.this.e = e;
                    }
                }

                @Test
                void shouldNotError() {
                    assertThat(e).isNull();
                }
            }

            @Nested
            @DisplayName("Given a null input stream")
            class ullInputStream {
                @BeforeEach
                void setUp() {
                    renderer = new TextplainToJpegRenderer(wrapText);

                    try {
                        result = renderer.convert(input, toMimeType);
                    } catch (Exception e) {
                        TextPlainToJpegRendererTest.this.e = e;
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
