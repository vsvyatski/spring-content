package org.springframework.content.test.support;

/**
 * Shared literals for store unit and integration tests (spell-check friendly).
 *
 * @author Vladimir Svyatski
 */
public final class TestSupportData {

    /** Synthetic {@code @ContentId} value for mocked store tests. */
    public static final String SAMPLE_CONTENT_ID = "12345-67890";

    /** Minimal content id when tests only need a short opaque key. */
    public static final String SAMPLE_SHORT_CONTENT_ID = "12345";

    /**
     * File-system placement path paired with {@link #SAMPLE_CONTENT_ID} in FS store unit tests.
     */
    public static final String SAMPLE_FS_RESOURCE_PATH = "/12345/67890";

    /** Default UTF-8 payload for round-trip content assertions. */
    public static final String SAMPLE_TEXT_PAYLOAD = "Hello Spring Content World!";

    private TestSupportData() {
    }
}