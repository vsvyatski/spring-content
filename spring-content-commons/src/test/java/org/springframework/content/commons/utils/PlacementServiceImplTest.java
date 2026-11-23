package org.springframework.content.commons.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlacementServiceImplTest {

    private PlacementServiceImpl placer;

    @Nested
    class PlacementServiceImplCases {
        @Nested
        @DisplayName("Given a placement service")
        class PlacementService {
            @BeforeEach
            void setUp() {
                placer = new PlacementServiceImpl();
            }

            @Test
            void shouldHaveRemovedTheFallbackObjectToStringConverter() {
                assertThat(placer.canConvert(Object.class, String.class)).isFalse();
            }
        }
    }
}
