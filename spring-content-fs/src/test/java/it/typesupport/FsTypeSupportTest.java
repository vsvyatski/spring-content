package it.typesupport;

import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.github.f4b6a3.uuid.UuidCreator;
import it.typesupport.model.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.UUID;

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
    class JavaUtilUUID {
        @Nested
        class GivenAContentEntity {
            @Nested
            class GivenTheApplicationSetsTheID {
                @BeforeEach
                void setUp() {
                    entity = new UUIDBasedContentEntity();
                    id = UuidCreator.getTimeOrdered();
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
            class GivenSpringContentGeneratesTheID {
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
    class JavaNetURI {
        @Nested
        class GivenAContentEntity {
            @Nested
            class GivenTheApplicationSetsTheID {
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
    class JavaLangLong {
        @Nested
        class GivenAContentEntity {
            @Nested
            class GivenTheApplicationSetsTheID {
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
    class JavaMathBigInteger {
        @Nested
        class GivenAContentEntity {
            @Nested
            class GivenTheApplicationSetsTheID {
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

    @Test
    public void noop() throws IOException {
    }
}
