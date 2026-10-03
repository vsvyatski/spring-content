package org.springframework.content.renditions.renderers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.RenditionException;
import org.springframework.renditions.poi.POIService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WordToJpegRendererTest {

    private POIService poi;
    private XWPFDocument doc;
    POIXMLProperties props;

    private RenditionProvider renderer;

    private InputStream input;
    private String mimeType;

    private Exception e;

    
    @Nested
    class WordToJpegRendererCases {
        @Nested
        class Consumes {
            @BeforeEach
            void setUp() throws Throwable {
                poi = mock(POIService.class);
                renderer = new WordToJpegRenderer(poi);

            }

            @Test
            void shouldReturnWordMlMimetype() throws Throwable {
                assertThat(renderer.consumes()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            }

        }

        @Nested
        class Produces {
            @BeforeEach
            void setUp() throws Throwable {
                poi = mock(POIService.class);
                renderer = new WordToJpegRenderer(poi);

            }

            @Test
            void shouldReturnJpegMimetype() throws Throwable {
                assertThat(renderer.produces()).contains("image/jpg");

            }

        }

        @Nested
        class Convert {
            @Nested
            class GivenAnInputStreamAndAMimetype {
                @BeforeEach
                void setUp() throws Throwable {
                    poi = mock(POIService.class);
                    renderer = new WordToJpegRenderer(poi);

                    doc = mock(XWPFDocument.class);
                    when(poi.xwpfDocument(any())).thenReturn(doc);
                    props = mock(POIXMLProperties.class);
                    when(doc.getProperties()).thenReturn(props);

                    input = new ByteArrayInputStream("".getBytes());

                    try {
                        renderer.convert(input, mimeType);
                    } catch (Exception e) {
                        WordToJpegRendererTest.this.e = e;
                    }

                }

                @Test
                void shouldGetTheEmbeddedThumbnailFromTheXWPFDocumentSProperties() throws Throwable {
                    verify(props).getThumbnailImage();

                }

                @Nested
                class WhenTheInputStreamIsNotAValidWordFile {
                    @BeforeEach
                    void setUp() throws Throwable {
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
                            renderer.convert(input, mimeType);
                        } catch (Exception e) {
                            WordToJpegRendererTest.this.e = e;
                        }

                    }

                    @Test
                    void shouldThrowARenditionException() throws Throwable {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);

                    }

                }

                @Nested
                class WhenTheWordDocumentFailsToReturnProperties {
                    @BeforeEach
                    void setUp() throws Throwable {
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
                            renderer.convert(input, mimeType);
                        } catch (Exception e) {
                            WordToJpegRendererTest.this.e = e;
                        }

                    }

                    @Test
                    void shouldThrowARenditionException() throws Throwable {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);

                    }

                }

                @Nested
                class WhenTheWordDocumentFailsToReturnAThumbnail {
                    @BeforeEach
                    void setUp() throws Throwable {
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
                            renderer.convert(input, mimeType);
                        } catch (Exception e) {
                            WordToJpegRendererTest.this.e = e;
                        }

                    }

                    @Test
                    void shouldThrowARenditionException() throws Throwable {
                        assertThat(e).isNotNull();
                        assertThat(e).isInstanceOf(RenditionException.class);

                    }

                }

            }

            @Nested
            class GivenANullInputStream {
                @BeforeEach
                void setUp() throws Throwable {
                    poi = mock(POIService.class);
                    renderer = new WordToJpegRenderer(poi);

                    try {
                        renderer.convert(input, mimeType);
                    } catch (Exception e) {
                        WordToJpegRendererTest.this.e = e;
                    }

                }

                @Test
                void shouldGetTheEmbeddedThumbnailFromTheXWPFDocumentSProperties() throws Throwable {
                    assertThat(e).isNotNull();

                }

            }

        }

    }

}
