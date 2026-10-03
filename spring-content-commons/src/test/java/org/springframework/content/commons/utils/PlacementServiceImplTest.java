package org.springframework.content.commons.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.assertThat;

public class PlacementServiceImplTest {

	private PlacementServiceImpl placer = null;

	
    @Nested
    class PlacementServiceImplCases {
        @Nested
        class GivenAPlacementService {
            @BeforeEach
            void setUp() throws Throwable {
                placer = new PlacementServiceImpl();

            }

            @Test
            void shouldHaveRemovedTheFallbackObjectToStringConverter() throws Throwable {
                assertThat(placer.canConvert(Object.class, String.class)).isFalse();

            }

        }

    }

}
