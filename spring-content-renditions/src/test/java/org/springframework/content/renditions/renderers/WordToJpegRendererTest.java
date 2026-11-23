package org.springframework.content.renditions.renderers;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.POIService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WordToJpegRendererTest {

    private final String toMimeType = "image/jpeg";
    POIXMLProperties props;
    private POIService poi;
    private XWPFDocument doc;
    private RenditionProvider renderer;
    private InputStream input;
    private Exception e;

    @Nested
   class WordToJpegRendererCases {
        @Nested
        class Consumes {
            @BeforeEach
            void setUp() {
                poi = mock(POIService.class);
                renderer = new WordToJpegRenderer(poi);
            }

            @Test
            void shouldReturnWordMlMimetype() {
                assertThat(renderer.consumes()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
        }

        @Nested
        class Produces {
            @BeforeEach
            void setUp() {
                poi = mock(POIService.class);
                renderer = new WordToJpegRenderer(poi);
            }

            @Test
            void shouldReturnJpegMimetype() {
                assertThat(renderer.produces()).contains("image/jpg");
            }
        }

        @Nested
        class Convert {
            @Nested
            @DisplayName("Given an input stream and a mimetype")
            class InputStreamAndMimetype {
                @BeforeEach
                void setUp() throws IOException {
                    poi = mock(POIService.class);
                    renderer = new WordToJpegRenderer(poi);

                    doc = mock(XWPFDocument.class);
                    when(poi.xwpfDocument(any())).thenReturn(doc);
                    props = mock(POIXMLProperties.class);
                    when(doc.getProperties()).thenReturn(props);

                    input = new ByteArrayInputStream("".getBytes());

                    try {
                        renderer.convert(input, toMimeType);
                    } catch (Exception e) {
                        WordToJpegRendererTest.this.e = e;
                    }
                }

                @Test
                void shouldGetTheEmbeddedThumbnailFromTheXWPFDocumentSProperties() throws IOException {
                    verify(props).getThumbnailImage();
                }

                @Nested

                @DisplayName("When the input stream is not a valid word file")

                class InputStreamIsNotValidWordFile {
                    @BeforeEach
                    void setUp() throws IOException {
                        poi = mock(POIService.class);
                        renderer = new WordToJpegRenderer(poi);

                        doc = mock(XWPFDocument.class);
                        when(poi.xwpfDocument(any())).thenReturn(doc);
                        props = mock(POIXMLProperties.class);
                        when(doc.getProperties()).thenReturn(props);

                        input = new ByteArrayInputStream("".getBytes());

                        doc = mock(XWPFDocument.class);
                        doThrow(NotOfficeXmlFileException.class).when(poi)
                            .xwpfDocument(any());

                        try {
                            renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            WordToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowARenditionException() {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);
                    }
                }

                @Nested

                @DisplayName("When the word document fails to return properties")

                class WordDocumentFailsToReturnProperties {
                    @BeforeEach
                    void setUp() throws IOException {
                        poi = mock(POIService.class);
                        renderer = new WordToJpegRenderer(poi);

                        doc = mock(XWPFDocument.class);
                        when(poi.xwpfDocument(any())).thenReturn(doc);
                        props = mock(POIXMLProperties.class);
                        when(doc.getProperties()).thenReturn(props);

                        input = new ByteArrayInputStream("".getBytes());

                        doc = mock(XWPFDocument.class);
                        when(poi.xwpfDocument(any())).thenReturn(doc);
                        props = mock(POIXMLProperties.class);
                        doThrow(POIXMLException.class).when(doc).getProperties();

                        try {
                            renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            WordToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowARenditionException() {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);
                    }
                }

                @Nested

                @DisplayName("When the word document fails to return a thumbnail")

                class WordDocumentFailsToReturnThumbnail {
                    @BeforeEach
                    void setUp() throws IOException {
                        poi = mock(POIService.class);
                        renderer = new WordToJpegRenderer(poi);

                        doc = mock(XWPFDocument.class);
                        when(poi.xwpfDocument(any())).thenReturn(doc);
                        props = mock(POIXMLProperties.class);
                        when(doc.getProperties()).thenReturn(props);

                        input = new ByteArrayInputStream("".getBytes());

                        doc = mock(XWPFDocument.class);
                        when(poi.xwpfDocument(any())).thenReturn(doc);
                        props = mock(POIXMLProperties.class);
                        when(doc.getProperties()).thenReturn(props);
                        doThrow(IOException.class).when(props).getThumbnailImage();

                        try {
                            renderer.convert(input, toMimeType);
                        } catch (Exception e) {
                            WordToJpegRendererTest.this.e = e;
                        }
                    }

                    @Test
                    void shouldThrowARenditionException() {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);
                    }
                }
            }

            @Nested

            @DisplayName("Given a null input stream")

            class ullInputStream {
                @BeforeEach
                void setUp() {
                    poi = mock(POIService.class);
                    renderer = new WordToJpegRenderer(poi);

                    try {
                        renderer.convert(input, toMimeType);
                    } catch (Exception e) {
                        WordToJpegRendererTest.this.e = e;
                    }
                }

                @Test
                void shouldGetTheEmbeddedThumbnailFromTheXWPFDocumentSProperties() {
                    assertThat(e).isNotNull();
                }
            }
        }
    }
}
