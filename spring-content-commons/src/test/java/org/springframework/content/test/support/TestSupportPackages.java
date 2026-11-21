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

    public static final String FS = ROOT + ".fs";

    public static final String S3 = ROOT + ".s3";

    public static final String GCS = ROOT + ".gcs";

    public static final String AZURE = ROOT + ".azure";

    public static final String FULLTEXT = ROOT + ".fulltext";

    public static final String ENCRYPTION = ROOT + ".encryption";

    private TestSupportPackages() {
    }
}
