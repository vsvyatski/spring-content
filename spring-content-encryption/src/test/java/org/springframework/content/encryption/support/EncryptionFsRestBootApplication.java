package org.springframework.content.encryption.support;

import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.lang.annotation.*;

/**
 * @author Vladimir Svyatski
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootApplication(exclude = {S3ContentAutoConfiguration.class})
@ImportAutoConfiguration(ContentRestAutoConfiguration.class)
public @interface EncryptionFsRestBootApplication {
}
