package org.springframework.versions.test.support;

/**
 * Package names for Spring Data and JPA scan configuration in versioning tests.
 *
 * @author Vladimir Svyatski
 */
public final class VersioningTestPackages {

    public static final String ROOT = "org.springframework.versions.test.support";

    public static final String JPA = ROOT + ".jpa";

    /**
     * {@link org.springframework.data.jpa.repository.config.EnableJpaRepositories} scan base; must include
     * {@code org.springframework.versions.impl} so {@code LockingAndVersioningRepositoryImpl} is found.
     */
    public static final String REPOSITORIES = "org.springframework.versions";

    private VersioningTestPackages() {
    }
}
