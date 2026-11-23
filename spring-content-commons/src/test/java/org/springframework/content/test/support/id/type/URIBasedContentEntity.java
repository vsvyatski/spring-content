package org.springframework.content.test.support.id.type;

import org.springframework.content.test.support.ContentFields;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public class URIBasedContentEntity extends ContentFields<URI> implements TypeSupportIdGenerator<URI> {

    @Override
    public URI generateId() {
        try {
            return new URI("http://www.example.com/" + UUID.randomUUID());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to generate a random URI in tests.", e);
        }
    }
}
