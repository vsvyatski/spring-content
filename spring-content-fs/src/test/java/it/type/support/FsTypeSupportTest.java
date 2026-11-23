package it.type.support;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.test.support.id.type.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {FsTypeSupportConfig.class})
@ExtendWith(SpringExtension.class)
public class FsTypeSupportTest {

    @Autowired
    protected UUIDBasedContentEntityStore uuidStore;
    @Autowired
    protected URIBasedContentEntityStore uriStore;
    @Autowired
    protected LongBasedContentEntityStore longStore;
    @Autowired
    protected BigIntegerBasedContentEntityStore bigIntStore;

    Object entity;
    Object id;

    @Nested

    @DisplayName("Java.util.uuid")

    class JavaUtilUuid {
        @Nested
        @DisplayName("Given a content entity")
        class ContentEntity {
            @Nested
            @DisplayName("Given the application sets the id")
            class ApplicationSetsId {
                @BeforeEach
                void setUp() {
                    entity = new UUIDBasedContentEntity();
                    id = UUID.randomUUID();
                    ((UUIDBasedContentEntity) entity).setContentId((UUID) id);

                    uuidStore.setContent((UUIDBasedContentEntity) entity, new ByteArrayInputStream("uuid".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    uuidStore.unsetContent((UUIDBasedContentEntity) entity);
                    assertThat(((UUIDBasedContentEntity) entity).getContentId()).isNull();
                }

                @Test
                void shouldStoreTheContentSuccessfully() throws IOException {
                    assertThat(IOUtils.contentEquals(
                        uuidStore.getContent((UUIDBasedContentEntity) entity),
                        IOUtils.toInputStream("uuid", Charset.defaultCharset())
                    )).isTrue();
                }
            }

            @Nested

            @DisplayName("Given spring content generates the id")

            class StoreGeneratesId {
                @BeforeEach
                void setUp() {
                    entity = new UUIDBasedContentEntity();
                    uuidStore.setContent(
                        (UUIDBasedContentEntity) entity,
                        new ByteArrayInputStream("uuid".getBytes()
                        ));
                }

                @AfterEach
                void tearDown() {
                    uuidStore.unsetContent((UUIDBasedContentEntity) entity);
                    assertThat(((UUIDBasedContentEntity) entity).getContentId()).isNull();
                }

                @Test
                void shouldStoreTheContentSuccessfully() throws IOException {
                    assertThat(IOUtils.contentEquals(
                        uuidStore.getContent((UUIDBasedContentEntity) entity),
                        IOUtils.toInputStream("uuid", Charset.defaultCharset())
                    )).isTrue();
                }
            }
        }
    }

    @Nested

    @DisplayName("Java.net.uri")

    class JavaNetUri {
        @Nested
        @DisplayName("Given a content entity")
        class ContentEntity {
            @Nested
            @DisplayName("Given the application sets the id")
            class ApplicationSetsId {
                @BeforeEach
                void setUp() throws URISyntaxException {
                    entity = new URIBasedContentEntity();
                    id = new URI("http://some.org/deep/location.html");
                    ((URIBasedContentEntity) entity).setContentId((URI) id);

                    uriStore.setContent((URIBasedContentEntity) entity, new ByteArrayInputStream("uri".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    uriStore.unsetContent((URIBasedContentEntity) entity);
                    assertThat(((URIBasedContentEntity) entity).getContentId()).isNull();
                }

                @Test
                void shouldStoreTheContentSuccessfully() throws IOException {
                    assertThat(IOUtils.contentEquals(
                        uriStore.getContent((URIBasedContentEntity) entity),
                        IOUtils.toInputStream("uri", Charset.defaultCharset())
                    )).isTrue();
                }
            }
        }
    }

    @Nested

    @DisplayName("Java.lang.long")

    class JavaLangLong {
        @Nested
        @DisplayName("Given a content entity")
        class ContentEntity {
            @Nested
            @DisplayName("Given the application sets the id")
            class ApplicationSetsId {
                @BeforeEach
                void setUp() {
                    entity = new LongBasedContentEntity();
                    id = Long.MAX_VALUE;
                    ((LongBasedContentEntity) entity).setContentId((Long) id);

                    longStore.setContent((LongBasedContentEntity) entity, new ByteArrayInputStream("long".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    longStore.unsetContent((LongBasedContentEntity) entity);
                    assertThat(((LongBasedContentEntity) entity).getContentId()).isNull();
                }

                @Test
                void shouldStoreTheContentSuccessfully() throws IOException {
                    assertThat(IOUtils.contentEquals(
                        longStore.getContent((LongBasedContentEntity) entity),
                        IOUtils.toInputStream("long", Charset.defaultCharset())
                    )).isTrue();
                }
            }
        }
    }

    @Nested

    @DisplayName("Java.math.biginteger")

    class JavaMathBigInteger {
        @Nested
        @DisplayName("Given a content entity")
        class ContentEntity {
            @Nested
            @DisplayName("Given the application sets the id")
            class ApplicationSetsId {
                @BeforeEach
                void setUp() {
                    entity = new BigIntegerBasedContentEntity();
                    id = BigInteger.valueOf(Long.MAX_VALUE);
                    ((BigIntegerBasedContentEntity) entity).setContentId((BigInteger) id);

                    bigIntStore.setContent((BigIntegerBasedContentEntity) entity, new ByteArrayInputStream("big-int".getBytes()));
                }

                @AfterEach
                void tearDown() {
                    bigIntStore.unsetContent((BigIntegerBasedContentEntity) entity);
                    assertThat(((BigIntegerBasedContentEntity) entity).getContentId()).isNull();
                }

                @Test
                void shouldStoreTheContentSuccessfully() throws IOException {
                    assertThat(IOUtils.contentEquals(
                        bigIntStore.getContent((BigIntegerBasedContentEntity) entity),
                        IOUtils.toInputStream("big-int", Charset.defaultCharset())
                    )).isTrue();
                }
            }
        }
    }
}
