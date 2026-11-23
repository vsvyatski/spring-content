package org.springframework.content.renditions.renderers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.PDFService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PdfToJpegRendererTest {

    private final String toMimeType = "image/jpeg";
    private PDFService pdf;
    private PDDocument doc;
    private PDFRenderer pdfRenderer;
    private RenditionProvider renderer;
    private InputStream input;
    private Exception e;

    @Nested
    class PdfToJpegRendererCases {
        @Nested
        class Consumes {
            @BeforeEach
            void setUp() {
                pdf = mock(PDFService.class);
                renderer = new PdfToJpegRenderer(pdf);
            }

            @Test
            void shouldReturnPdfMimeType() {
                assertThat(renderer.consumes()).isEqualTo("application/pdf");
            }
        }

        @Nested
        class Produces {
            @BeforeEach
            void setUp() {
                pdf = mock(PDFService.class);
                renderer = new PdfToJpegRenderer(pdf);
            }

            @Test
            void shouldReturnJpegMimeType() {
                assertThat(renderer.produces()).contains("image/jpg");
            }
        }

        @Nested
        class Convert {
            @Nested
            @DisplayName("Given an input stream and a MIME type")
            class InputStreamAndMimeType {
                @Nested
                @DisplayName("When the pdf has more than one page")
                class PdfHasMoreThanOnePage {
                    @BeforeEach
                    void setUp() throws IOException {
                        pdf = mock(PDFService.class);
                        renderer = new PdfToJpegRenderer(pdf);

                        doc = mock(PDDocument.class);
                        when(pdf.load(any())).thenReturn(doc);
                        pdfRenderer = mock(PDFRenderer.class);
                        when(pdf.renderer(doc)).thenReturn(pdfRenderer);

                        input = new ByteArrayInputStream("".getBytes());

                        when(doc.getNumberOfPages()).thenReturn(1);
                        try {
                            renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            PdfToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldRenderFirstPageAtDpi() throws IOException {
                        verify(pdfRenderer).renderImageWithDPI(0, 300, ImageType.RGB);
                    }

                    @Test
                    void shouldOutputRenderedImage() throws IOException {
                        verify(pdf).writeImage(any(), eq("jpeg"), isA(OutputStream.class));
                    }

                    @Nested
                    @DisplayName("When the pdf document fails to return a thumbnail")
                    class PdfDocumentFailsToReturnThumbnail {
                        @BeforeEach
                        void setUp() throws IOException {
                            pdf = mock(PDFService.class);
                            renderer = new PdfToJpegRenderer(pdf);

                            doc = mock(PDDocument.class);
                            when(pdf.load(any())).thenReturn(doc);
                            pdfRenderer = mock(PDFRenderer.class);
                            when(pdf.renderer(doc)).thenReturn(pdfRenderer);

                            input = new ByteArrayInputStream("".getBytes());

                            when(doc.getNumberOfPages()).thenReturn(1);
                            doThrow(IOException.class).when(pdfRenderer)
                                .renderImageWithDPI(0, 300, ImageType.RGB);
                            try {
                                renderer.convert(input, toMimeType);
                            } catch (Exception e) {
                                PdfToJpegRendererTest.this.e = e;
                            }
                        }

                        @Test
                        void shouldThrowRenditionException() {
                            assertThat(e).isNotNull();
                            assertThat(e).isInstanceOf(RenditionException.class);
                        }

                        @Test
                        void shouldCloseDocument() throws IOException {
                            verify(doc).close();
                        }
                    }
                }

                @Nested
                @DisplayName("When the input stream is not a valid pdf file")
                class InputStreamIsNotValidPdfFile {
                    @BeforeEach
                    void setUp() throws IOException {
                        pdf = mock(PDFService.class);
                        renderer = new PdfToJpegRenderer(pdf);

                        doc = mock(PDDocument.class);
                        when(pdf.load(any())).thenReturn(doc);
                        pdfRenderer = mock(PDFRenderer.class);
                        when(pdf.renderer(doc)).thenReturn(pdfRenderer);

                        input = new ByteArrayInputStream("".getBytes());

                        doThrow(IOException.class).when(pdf).load(any());
                        try {
                            renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            PdfToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowRenditionException() {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);
                    }
                }
            }

            @Nested
            @DisplayName("Given a null input stream")
            class NullInputStream {
                @BeforeEach
                void setUp() {
                    pdf = mock(PDFService.class);
                    renderer = new PdfToJpegRenderer(pdf);

                    try {
                        renderer.convert(input, toMimeType);
                    } catch (Exception e) {
                        PdfToJpegRendererTest.this.e = e;
                    }
                }

                @Test
                void shouldThrowWhenInputStreamIsNull() {
                    assertThat(e).isNotNull();
                }
            }
        }
    }
}
