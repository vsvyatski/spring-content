package org.springframework.content.test.support.idtype;

import org.springframework.content.test.support.ContentFields;

import java.util.Random;

/**
 * @author Vladimir Svyatski
 */
public class LongBasedContentEntity extends ContentFields<Long> implements TypeSupportIdGenerator<Long> {

    @Override
    public Long generateId() {
        return new Random().nextLong();
    }
}
