package internal.org.springframework.renditions.poi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.renditions.renderers.WordToJpegRenderer;
import org.springframework.renditions.poi.POIService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class POIServiceTest {

	private POIService poi;

	private InputStream stream;

    @Nested
    class POIServiceCases {
        @Nested
        class XwpfDocument {
            @Nested
            class GivenAnInputStream {
                @BeforeEach
                void setUp() {
                    poi = new POIServiceImpl();

                    stream = this.getClass().getResourceAsStream("/sample-docx.docx");
                }

                @Test
                void shouldReturnAnInstanceOfAnXPWFDocument() throws IOException {
                    assertThat(poi.xwpfDocument(stream)).isNotNull();
                }

            }

            @Nested
            class GivenANullInputstream {
                @BeforeEach
                void setUp() {
                    poi = new POIServiceImpl();
                }

                @Test
                void shouldThrowAnException() {
                    try {
                    	poi.xwpfDocument(stream);
                    	fail("no exception thrown");
                    }
                    catch (Exception e) {
                    	assertThat(e).isNotNull();
                    	assertThat(e).isInstanceOf(IllegalArgumentException.class);
                    }
                }

            }

            @Nested
            class GivenAnInvalidInputstream {
                @BeforeEach
                void setUp() {
                    poi = new POIServiceImpl();

                    stream = new ByteArrayInputStream("asdhg".getBytes());
                }

                @Test
                void shouldThrowAnException() {
                    try {
                    	poi.xwpfDocument(stream);
                    	fail("no exception thrown");
                    }
                    catch (Exception e) {
                    	assertThat(e).isNotNull();
                    	assertThat(e).isInstanceOf(NotOfficeXmlFileException.class);
                    }
                }

            }

        }

    }

}
