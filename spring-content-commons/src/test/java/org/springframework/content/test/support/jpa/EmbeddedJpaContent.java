package org.springframework.content.test.support.jpa;

import jakarta.persistence.Embeddable;
import org.springframework.content.test.support.SingleContentFields;

/**
 * {@link jakarta.persistence.Embedded} content metadata for JPA store ITs.
 *
 * @author Vladimir Svyatski
 */
@Embeddable
public class EmbeddedJpaContent extends SingleContentFields {
}
