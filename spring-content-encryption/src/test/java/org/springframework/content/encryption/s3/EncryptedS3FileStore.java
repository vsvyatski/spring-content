package org.springframework.content.encryption.s3;

import org.springframework.content.encryption.store.EncryptingContentStore;
import org.springframework.content.s3.store.S3ContentStore;
import org.springframework.content.test.support.encryption.FsFile;

import java.util.UUID;

/**
 * S3 encrypting store marker for {@link EncryptionIT}.
 *
 * @author Vladimir Svyatski
 */
public interface EncryptedS3FileStore extends S3ContentStore<FsFile, UUID>, EncryptingContentStore<FsFile, UUID> {
}