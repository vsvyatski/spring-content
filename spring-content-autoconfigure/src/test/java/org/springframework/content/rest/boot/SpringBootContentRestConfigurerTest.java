package org.springframework.content.rest.boot;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration.ContentRestProperties.ShortcutRequestMappings;
import internal.org.springframework.content.rest.boot.autoconfigure.SpringBootContentRestConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.http.MediaType;

import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
            @DisplayName("Given a base URI property")
            class BaseUriProperty {
                @BeforeEach
                void setUp() {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    properties.setBaseUri(URI.create("/test"));

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);
                }

                @Test
                void shouldSetPropertyOnRestConfiguration() {
                    verify(restConfig).setBaseUri(eq(properties.getBaseUri()));
                }
            }

            @Nested
            @DisplayName("Given a fully qualified links property setting")
            class FullyQualifiedLinksPropertySetting {
                @BeforeEach
                void setUp() {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    properties.setFullyQualifiedLinks(true);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);
                }

                @Test
                void shouldSetPropertyOnRestConfiguration() {
                    verify(restConfig).setFullyQualifiedLinks(eq(true));
                }
            }

            @Nested
            @DisplayName("Given disabled shortcut request mappings")
            class DisabledShortcutRequestMappings {
                @BeforeEach
                void setUp() {
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
                void shouldDisableShortcutLinks() {
                    verify(restConfig).setShortcutLinks(false);
                }
            }

            @Nested
            @DisplayName("Given excluded shortcut request mappings")
            class ExcludedShortcutRequestMappings {
                @BeforeEach
                void setUp() {
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
                void shouldSetExclusionsPropertyOnRestConfiguration() {
                    verify(exclusions).exclude("GET", MediaType.parseMediaType("a/b"));
                    verify(exclusions).exclude("GET", MediaType.parseMediaType("c/d"));
                    verify(exclusions).exclude("PUT", MediaType.parseMediaType("*/*"));
                }
            }

            @Nested
            @DisplayName("Given empty excluded shortcut request mapping")
            class EmptyExcludedShortcutRequestMapping {
                @BeforeEach
                void setUp() {
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
                void shouldNotSetExclusionsPropertyOnRestConfiguration() {
                    verify(exclusions, never()).exclude(any(), any());
                }
            }

            @Nested
            @DisplayName("Given empty excluded shortcut GET request mapping")
            class EmptyExcludedShortcutGETRequestMapping {
                @BeforeEach
                void setUp() {
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
                void shouldNotSetExclusionsPropertyOnRestConfiguration() {
                    verify(exclusions, never()).exclude(any(), any());
                }
            }

            @Nested
            @DisplayName("Given invalid excluded shortcut request mapping")
            class InvalidExcludedShortcutRequestMapping {
                @BeforeEach
                void setUp() {
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
                void shouldNotSetExclusionsPropertyOnRestConfiguration() {
                    verify(exclusions, never()).exclude(any(), any());
                }
            }

            @Nested
            @DisplayName("Given a null base URI property")
            class ullBaseUriProperty {
                @BeforeEach
                void setUp() {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    configurer = new SpringBootContentRestConfigurer(properties);
                    configurer.configure(restConfig);
                }

                @Test
                void shouldNotSetPropertyOnRestConfiguration() {
                    verify(restConfig, never()).setBaseUri(any());
                }
            }

            @Nested
            @DisplayName("Given null REST properties")
            class NullRestProperties {
                @BeforeEach
                void setUp() {
                    properties = new ContentRestProperties();
                    restConfig = mock(RestConfiguration.class);
                    exclusions = mock(RestConfiguration.Exclusions.class);
                    when(restConfig.shortcutExclusions()).thenReturn(exclusions);

                    configurer = new SpringBootContentRestConfigurer(properties = null);
                    configurer.configure(restConfig);
                }

                @Test
                void shouldNotSetPropertyOnRestConfiguration() {
                    verify(restConfig, never()).setBaseUri(any());
                    verify(restConfig, never()).setFullyQualifiedLinks(anyBoolean());
                }
            }
        }
    }
}
