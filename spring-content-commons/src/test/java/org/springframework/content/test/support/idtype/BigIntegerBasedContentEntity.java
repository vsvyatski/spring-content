package org.springframework.content.test.support.idtype;

import org.springframework.content.test.support.ContentFields;

import java.math.BigInteger;
import java.util.Random;

/**
 * @author Vladimir Svyatski
 */
public class BigIntegerBasedContentEntity extends ContentFields<BigInteger>
    implements TypeSupportIdGenerator<BigInteger> {

    @Override
    public BigInteger generateId() {
        return new BigInteger(96, new Random());
    }
}
