package org.springframework.content.rest.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.http.MediaType;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties.ShortcutRequestMappings;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;

public class SpringBootContentRestConfigurerTest {

    private SpringBootContentRestConfigurer configurer;

    private ContentRestProperties properties;

    // mocks
    private RestConfiguration restConfig;
    private RestConfiguration.Exclusions exclusions;

    
    @Nested
    class SpringBootContentRestConfigurerCases {
        @Nested
        class Configure {
            @Nested
            class GivenABaseUriProperty {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    properties.setBaseUri(URI.create("/test"));

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldSetThePropertyOnTheRestConfiguration() throws Throwable {
                    verify(restConfig).setBaseUri(eq(properties.getBaseUri()));

                }

            }

            @Nested
            class GivenAFullyQualifiedLinksPropertySetting {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    properties.setFullyQualifiedLinks(true);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldSetThePropertyOnTheRestConfiguration() throws Throwable {
                    verify(restConfig).setFullyQualifiedLinks(eq(true));

                }

            }

            @Nested
            class GivenDisabledShortcutRequestMappings {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                    mappings.setDisabled(true);
                    properties.setShortcutRequestMappings(mappings);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldDisableTheShortcutLinks() throws Throwable {
                    verify(restConfig).setShortcutLinks(false);

                }

            }

            @Nested
            class GivenExcludedShortcutRequestMappings {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                    mappings.setExcludes("GET=a/b,c/d:PUT=*/*");
                    properties.setShortcutRequestMappings(mappings);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldSetTheExclusionsPropertyOnTheRestConfiguration() throws Throwable {
                    verify(exclusions).exclude("GET", MediaType.parseMediaType("a/b"));
                    verify(exclusions).exclude("GET", MediaType.parseMediaType("c/d"));
                    verify(exclusions).exclude("PUT", MediaType.parseMediaType("*/*"));

                }

            }

            @Nested
            class GivenEmptyExcludedShortcutRequestMapping {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                    mappings.setExcludes("");
                    properties.setShortcutRequestMappings(mappings);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldNotSetTheExclusionsPropertyOnTheRestConfiguration() throws Throwable {
                    verify(exclusions, never()).exclude(any(), any());

                }

            }

            @Nested
            class GivenEmptyExcludedShortcutGETRequestMapping {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                    mappings.setExcludes("GET=");
                    properties.setShortcutRequestMappings(mappings);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldNotSetTheExclusionsPropertyOnTheRestConfiguration() throws Throwable {
                    verify(exclusions, never()).exclude(any(), any());

                }

            }

            @Nested
            class GivenInvalidExcludedShortcutRequestMapping {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    ShortcutRequestMappings mappings = new ShortcutRequestMappings();
                    mappings.setExcludes("GET=/");
                    properties.setShortcutRequestMappings(mappings);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldNotSetTheExclusionsPropertyOnTheRestConfiguration() throws Throwable {
                    verify(exclusions, never()).exclude(any(), any());

                }

            }

            @Nested
            class GivenANullBaseUriProperty {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldNotSetThePropertyOnTheRestConfiguration() throws Throwable {
                    verify(restConfig, never()).setBaseUri(any());

                }

            }

            @Nested
            class GivenANullProperties {
                @BeforeEach
                void setUp() throws Throwable {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    properties = null;

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);

                }

                @Test
                void shouldNotSetThePropertyOnTheRestConfiguration() throws Throwable {
                    verify(restConfig, never()).setBaseUri(any());
                    verify(restConfig, never()).setFullyQualifiedLinks(anyBoolean());

                }

            }

        }

    }

}
