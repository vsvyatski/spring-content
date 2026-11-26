package org.springframework.content.encryption.fs;

import org.springframework.content.encryption.store.EncryptingContentStore;
import org.springframework.content.fs.store.FileSystemContentStore;
import org.springframework.content.test.support.encryption.FsFile;

import java.util.UUID;

/**
 * FS encrypting store marker for {@link EncryptionIT}.
 *
 * @author Vladimir Svyatski
 */
public interface EncryptedFsFileStore
    extends FileSystemContentStore<FsFile, UUID>, EncryptingContentStore<FsFile> {
}
