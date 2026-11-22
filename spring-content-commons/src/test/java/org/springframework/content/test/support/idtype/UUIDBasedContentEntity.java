package org.springframework.content.test.support.idtype;

import org.springframework.content.test.support.UuidSingleContentFields;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public class UUIDBasedContentEntity extends UuidSingleContentFields implements TypeSupportIdGenerator<UUID> {

    @Override
    public UUID generateId() {
        return UUID.randomUUID();
    }
}
