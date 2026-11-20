package org.springframework.content.test.support;

/**
 * Package names for Spring Data scan configuration in integration tests.
 *
 * @author Vladimir Svyatski
 */
public final class TestSupportPackages {

    public static final String ROOT = "org.springframework.content.test.support";

    public static final String JPA = ROOT + ".jpa";

    public static final String MONGO = ROOT + ".mongo";

    private TestSupportPackages() {
    }
}