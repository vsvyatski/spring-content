package org.springframework.content.test.support.encryption;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
public interface ContentEncryptionKeyRepository extends CrudRepository<ContentEncryptionKey, UUID> {
}
