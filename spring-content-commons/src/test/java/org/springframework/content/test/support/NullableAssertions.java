package org.springframework.content.test.support;

import org.jspecify.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssertJ {@code isNull()} for values that may be null but appear non-null to null-marked APIs.
 *
 * @author Vladimir Svyatski
 */
public final class NullableAssertions {

    private NullableAssertions() {
    }

    public static void assertNull(@Nullable Object value) {
        assertThat(value).isNull();
    }
}
