package internal.org.springframework.content.rest.mappings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.store.Store;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CorsConfigurationBuilderTest {

    private CorsConfigurationBuilder builder;
    private CorsConfiguration config;

    private Class<?> storeInterface;

    private Exception e;

    public interface StoreWithNoCrossOrigin extends Store<UUID> {
    }

    @CrossOrigin(origins = {"http://domain1.com", "http://domain2.com"})
    public interface StoreWithOrigins extends Store<UUID> {
    }

    @CrossOrigin()
    public interface StoreWithEmptyOrigins extends Store<UUID> {
    }

    @CrossOrigin(allowedHeaders = {"header1", "header2"})
    public interface StoreWithAllowedHeaders extends Store<UUID> {
    }

    @CrossOrigin()
    public interface StoreWithEmptyAllowedHeaders extends Store<UUID> {
    }

    @CrossOrigin(exposedHeaders = {"exposed1", "exposed2"})
    public interface StoreWithExposedHeaders extends Store<UUID> {
    }

    @CrossOrigin()
    public interface StoreWithEmptyExposedHeaders extends Store<UUID> {
    }

    @CrossOrigin(methods = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST,
        RequestMethod.DELETE})
    public interface StoreWithAllowedMethods extends Store<UUID> {
    }

    @CrossOrigin()
    public interface StoreWithEmptyAllowedMethods extends Store<UUID> {
    }

    @CrossOrigin(allowCredentials = "true")
    public interface StoreWithAllowCredentials extends Store<UUID> {
    }

    @CrossOrigin(allowCredentials = "false")
    public interface StoreWithDisallowCredentials extends Store<UUID> {
    }

    @CrossOrigin(allowCredentials = "something-else")
    public interface StoreWithMisconfiguredAllowCredentials extends Store<UUID> {
    }

    @CrossOrigin()
    public interface StoreWithEmptyAllowCredentials extends Store<UUID> {
    }

    @CrossOrigin(maxAge = 1000L)
    public interface StoreWithMaxAge extends Store<UUID> {
    }

    @CrossOrigin(maxAge = 0L)
    public interface StoreWithZeroMaxAge extends Store<UUID> {
    }

    @CrossOrigin(maxAge = -1000L)
    public interface StoreWithNegativeMaxAge extends Store<UUID> {
    }

    @Nested
    class CorsConfigurationBuilderCases {
        @Nested
        class Build {
            @Nested
            @DisplayName("Given no @CrossOrigin annotation")
            class NoCrossOriginAnnotation {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithNoCrossOrigin.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldNotCorsConfiguration() {
                    assertThat(config).isNull();
                }
            }

            @Nested
            @DisplayName("Given an origins value")
            class OriginsValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithOrigins.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThoseOrigins() {
                    assertThat(config.getAllowedOrigins()).contains("http://domain1.com", "http://domain2.com");
                }
            }

            @Nested
            @DisplayName("Given an empty origins value")
            class EmptyOriginsValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithEmptyOrigins.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldSetDefaultOrigin() {
                    assertThat(config.getAllowedOrigins()).contains("*");
                }
            }

            @Nested
            @DisplayName("Given an allowed methods value")
            class AllowedMethodsValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithAllowedMethods.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThoseAllowedMethods() {
                    assertThat(config.getAllowedMethods()).contains("GET", "PUT", "POST", "DELETE");
                }
            }

            @Nested
            @DisplayName("Given an empty allowed methods value")
            class EmptyAllowedMethodsValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithEmptyAllowedMethods.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldSetDefaultAllowedMethods() {
                    assertThat(config.getAllowedMethods()).contains("GET", "POST", "HEAD");
                }
            }

            @Nested
            @DisplayName("Given an allowed headers value")
            class AllowedHeadersValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithAllowedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThoseAllowedHeaders() {
                    assertThat(config.getAllowedHeaders()).contains("header1", "header2");
                }
            }

            @Nested
            @DisplayName("Given an empty allowed headers value")
            class EmptyAllowedHeadersValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithEmptyAllowedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldSetDefaultAllowedHeaders() {
                    assertThat(config.getAllowedHeaders()).contains("*");
                }
            }

            @Nested
            @DisplayName("Given an exposed headers value")
            class ExposedHeadersValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithExposedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThoseExposedHeaders() {
                    assertThat(config.getExposedHeaders()).contains("exposed1", "exposed2");
                }
            }

            @Nested
            @DisplayName("Given an empty exposed headers value")
            class EmptyExposedHeadersValue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithEmptyExposedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithNullExposedHeaders() {
                    assertThat(config.getExposedHeaders()).isNull();
                }
            }

            @Nested
            @DisplayName("Given an allow credentials value of true")
            class AllowCredentialsValueOfTrue {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithAllowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThatValue() {
                    assertThat(config.getAllowCredentials()).isTrue();
                }
            }

            @Nested
            @DisplayName("Given an allow credentials value of false")
            class AllowCredentialsValueOfFalse {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithDisallowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThatValue() {
                    assertThat(config.getAllowCredentials()).isFalse();
                }
            }

            @Nested
            @DisplayName("Given an allow credentials value of something else")
            class AllowCredentialsValueOfSomethingElse {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithMisconfiguredAllowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThatValue() {
                    assertThat(e).isNotNull();
                }
            }

            @Nested
            @DisplayName("Given an allow credentials value of")
            class AllowCredentialsValueOf {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithEmptyAllowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithDefaultAllowCredentialsValue() {
                    assertThat(config.getAllowCredentials()).isNull();
                }
            }

            @Nested
            @DisplayName("Given a positive max age specification")
            class PositiveMaxAgeSpecification {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithMaxAge.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThatMaxAge() {
                    assertThat(config.getMaxAge()).isEqualTo(1000L);
                }
            }

            @Nested
            @DisplayName("Given an zero max age specification")
            class ZeroMaxAgeSpecification {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithZeroMaxAge.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithThatMaxAge() {
                    assertThat(config.getMaxAge()).isEqualTo(0L);
                }
            }

            @Nested
            @DisplayName("Given an negative max age specification")
            class NegativeMaxAgeSpecification {
                @BeforeEach
                void setUp() {
                    storeInterface = StoreWithNegativeMaxAge.class;

                    builder = new CorsConfigurationBuilder();
                    try {
                        config = builder.build(storeInterface);
                    } catch (Exception e) {
                        CorsConfigurationBuilderTest.this.e = e;
                    }
                }

                @Test
                void shouldCreateCorsConfigurationWithDefaultMaxAge() {
                    assertThat(config.getMaxAge()).isEqualTo(1800L);
                }
            }
        }
    }
}
