package org.springframework.versions.test.support;

/**
 * Package names for Spring Data and JPA scan configuration in versioning tests.
 *
 * @author Vladimir Svyatski
 */
public final class VersioningTestPackages {

    public static final String ROOT = "org.springframework.versions.test.support";

    public static final String JPA = ROOT + ".jpa";

    private VersioningTestPackages() {
    }
}
