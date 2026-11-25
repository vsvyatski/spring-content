package internal.org.springframework.content.rest.support;

import org.springframework.http.MediaType;

/**
 * MIME type strings for REST tests where {@link MediaType} has no named constant (e.g. HAL).
 *
 * @author Vladimir Svyatski
 */
public final class RestTestMediaTypes {

    public static final String APPLICATION_HAL_JSON_VALUE =
        MediaType.parseMediaType("application/hal+json").toString();

    private RestTestMediaTypes() {
    }
}