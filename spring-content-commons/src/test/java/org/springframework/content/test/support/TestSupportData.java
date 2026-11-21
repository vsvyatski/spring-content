package org.springframework.content.test.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shared literals for store unit and integration tests (spell-check friendly).
 *
 * @author Vladimir Svyatski
 */
public final class TestSupportData {

    /**
     * Synthetic {@code @ContentId} value for mocked store tests.
     */
    public static final String SAMPLE_CONTENT_ID = "12345-67890";

    /**
     * Minimal content id when tests only need a short opaque key.
     */
    public static final String SAMPLE_SHORT_CONTENT_ID = "12345";

    /**
     * File-system placement path paired with {@link #SAMPLE_CONTENT_ID} in FS store unit tests.
     */
    public static final String SAMPLE_FS_RESOURCE_PATH = "/12345/67890";

    /**
     * Fixed resource key for associate / missing-resource store IT setup.
     */
    public static final String UNASSOCIATED_RESOURCE_LOCATION = "unassociated-resource";

    /** Non-Office bytes for parser and format negative tests. */
    public static final String SAMPLE_INVALID_OFFICE_XML_PAYLOAD = "not-an-office-xml-file";

    /**
     * Default UTF-8 payload for round-trip content assertions.
     */
    public static final String SAMPLE_TEXT_PAYLOAD = "Hello Spring Content World!";

    public static final String SAMPLE_UPDATED_TEXT_PAYLOAD = "Hello Updated Spring Content World!";

    /**
     * Shorter payload used when ITs overwrite only part of dual content.
     */
    public static final String SAMPLE_PARTIAL_UPDATE_PAYLOAD = "Hello Spring World!";

    public static final String SAMPLE_HTML_RENDITION_PAYLOAD = "<html>" + SAMPLE_TEXT_PAYLOAD + "</html>";

    public static final String SAMPLE_UPDATED_HTML_RENDITION_PAYLOAD =
        "<html>" + SAMPLE_UPDATED_TEXT_PAYLOAD + "</html>";

    public static final String SAMPLE_PARTIAL_UPDATE_HTML_RENDITION =
        "<html>" + SAMPLE_PARTIAL_UPDATE_PAYLOAD + "</html>";

    /**
     * Rendition body returned by Spring Data REST content endpoints in ITs.
     */
    public static final String SAMPLE_REST_HTML_RENDITION_BODY =
        "<html><body>" + SAMPLE_TEXT_PAYLOAD + "</body></html>";

    /**
     * Payload for {@code @LastModifiedDate} REST controller ITs.
     */
    public static final String SAMPLE_LAST_MODIFIED_DATE_PAYLOAD = "Hello Spring Content LastModifiedDate World!";

    private TestSupportData() {
    }

    public static byte[] sampleTextBytes() {
        return SAMPLE_TEXT_PAYLOAD.getBytes(StandardCharsets.UTF_8);
    }

    public static ByteArrayInputStream sampleTextStream() {
        return new ByteArrayInputStream(sampleTextBytes());
    }

    public static ByteArrayInputStream sampleHtmlRenditionStream() {
        return new ByteArrayInputStream(SAMPLE_HTML_RENDITION_PAYLOAD.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteArrayInputStream sampleUpdatedTextStream() {
        return new ByteArrayInputStream(SAMPLE_UPDATED_TEXT_PAYLOAD.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteArrayInputStream sampleUpdatedHtmlRenditionStream() {
        return new ByteArrayInputStream(SAMPLE_UPDATED_HTML_RENDITION_PAYLOAD.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteArrayInputStream samplePartialUpdateStream() {
        return new ByteArrayInputStream(SAMPLE_PARTIAL_UPDATE_PAYLOAD.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteArrayInputStream samplePartialUpdateHtmlRenditionStream() {
        return new ByteArrayInputStream(SAMPLE_PARTIAL_UPDATE_HTML_RENDITION.getBytes(StandardCharsets.UTF_8));
    }
}
