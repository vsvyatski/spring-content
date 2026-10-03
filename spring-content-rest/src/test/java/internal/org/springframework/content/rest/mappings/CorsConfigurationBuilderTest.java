package internal.org.springframework.content.rest.mappings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.content.commons.store.Store;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;



public class CorsConfigurationBuilderTest {

	private CorsConfigurationBuilder builder;
	private CorsConfiguration config;

	private Class<?> storeInterface;

	private Exception e;

	
    @Nested
    class CorsConfigurationBuilderCases {
        @Nested
        class Build {
            @Nested
            class GivenNoCrossOriginAnnotation {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithNoCrossOrigin.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldNotACorsConfiguration() throws Throwable {
                    assertThat(config).isNull();
                }
            }
            @Nested
            class GivenAnOriginsValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithOrigins.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThoseOrigins() throws Throwable {
                    assertThat(config.getAllowedOrigins()).contains("http://domain1.com", "http://domain2.com");
                }
            }
            @Nested
            class GivenAnEmptyOriginsValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithEmptyOrigins.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldSetTheDefaultOrigin() throws Throwable {
                    assertThat(config.getAllowedOrigins()).contains("*");
                }
            }
            @Nested
            class GivenAnAllowedMethodsValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithAllowedMethods.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThoseAllowedMethods() throws Throwable {
                    assertThat(config.getAllowedMethods()).contains("GET", "PUT", "POST", "DELETE");
                }
            }
            @Nested
            class GivenAnEmptyAllowedMethodsValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithEmptyAllowedMethods.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldSetTheDefaultAllowedMethods() throws Throwable {
                    assertThat(config.getAllowedMethods()).contains("GET", "POST", "HEAD");
                }
            }
            @Nested
            class GivenAnAllowedHeadersValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithAllowedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThoseAllowedHeaders() throws Throwable {
                    assertThat(config.getAllowedHeaders()).contains("header1", "header2");
                }
            }
            @Nested
            class GivenAnEmptyAllowedHeadersValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithEmptyAllowedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldSetTheDefaultAllowedHeaders() throws Throwable {
                    assertThat(config.getAllowedHeaders()).contains("*");
                }
            }
            @Nested
            class GivenAnExposedHeadersValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithExposedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThoseExposedHeaders() throws Throwable {
                    assertThat(config.getExposedHeaders()).contains("exposed1", "exposed2");
                }
            }
            @Nested
            class GivenAnEmptyExposedHeadersValue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithEmptyExposedHeaders.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithANullExposedHeaders() throws Throwable {
                    assertThat(config.getExposedHeaders()).isNull();
                }
            }
            @Nested
            class GivenAnAllowCredentialsValueOfTrue {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithAllowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThatValue() throws Throwable {
                    assertThat(config.getAllowCredentials()).isTrue();
                }
            }
            @Nested
            class GivenAnAllowCredentialsValueOfFalse {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithDisallowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThatValue() throws Throwable {
                    assertThat(config.getAllowCredentials()).isFalse();
                }
            }
            @Nested
            class GivenAnAllowCredentialsValueOfSomethingElse {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithMisconfiguredAllowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThatValue() throws Throwable {
                    assertThat(e).isNotNull();
                }
            }
            @Nested
            class GivenAnAllowCredentialsValueOf {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithEmptyAllowCredentials.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithTheDefaultAllowCredentialsValue() throws Throwable {
                    assertThat(config.getAllowCredentials()).isNull();
                }
            }
            @Nested
            class GivenAPositiveMaxAgeSpecification {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithMaxAge.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThatMaxAge() throws Throwable {
                    assertThat(config.getMaxAge()).isEqualTo(1000L);
                }
            }
            @Nested
            class GivenAnZeroMaxAgeSpecification {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithZeroMaxAge.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithThatMaxAge() throws Throwable {
                    assertThat(config.getMaxAge()).isEqualTo(0L);
                }
            }
            @Nested
            class GivenAnNegativeMaxAgeSpecification {
                @BeforeEach
                void setUp() throws Throwable {
                    storeInterface = StoreWithNegativeMaxAge.class;

                    builder = new CorsConfigurationBuilder();
                    					try {
                    						config = builder.build(storeInterface);
                    					}
                    					catch (Exception e) {
                    						CorsConfigurationBuilderTest.this.e = e;
                    					}
                }
                @Test
                void shouldCreateACorsConfigurationWithTheDefaultMaxAge() throws Throwable {
                    assertThat(config.getMaxAge()).isEqualTo(1800L);
                }
            }
        }
    }


	public interface StoreWithNoCrossOrigin extends Store<UUID> {
	}

	@CrossOrigin(origins = { "http://domain1.com", "http://domain2.com" })
	public interface StoreWithOrigins extends Store<UUID> {
	}

	@CrossOrigin(origins = {})
	public interface StoreWithEmptyOrigins extends Store<UUID> {
	}

	@CrossOrigin(allowedHeaders = { "header1", "header2" })
	public interface StoreWithAllowedHeaders extends Store<UUID> {
	}

	@CrossOrigin(allowedHeaders = {})
	public interface StoreWithEmptyAllowedHeaders extends Store<UUID> {
	}

	@CrossOrigin(exposedHeaders = { "exposed1", "exposed2" })
	public interface StoreWithExposedHeaders extends Store<UUID> {
	}

	@CrossOrigin(exposedHeaders = {})
	public interface StoreWithEmptyExposedHeaders extends Store<UUID> {
	}

	@CrossOrigin(methods = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST,
			RequestMethod.DELETE })
	public interface StoreWithAllowedMethods extends Store<UUID> {
	}

	@CrossOrigin(methods = {})
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

	@CrossOrigin(allowCredentials = "")
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
}
