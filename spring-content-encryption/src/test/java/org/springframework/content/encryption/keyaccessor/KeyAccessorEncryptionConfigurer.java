package org.springframework.content.encryption.keyaccessor;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.content.encryption.fs.EncryptedFsFileStore;
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.content.test.support.encryption.ContentEncryptionKey;
import org.springframework.content.test.support.encryption.ContentEncryptionKeyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.UUID;

/**
 * @author Vladimir Svyatski
 */
@Configuration
public class KeyAccessorEncryptionConfigurer {

    @Bean
    public EncryptingContentStoreConfigurer<EncryptedFsFileStore> config(
        ContentEncryptionKeyRepository encryptionKeyRepository) {
        return configuration -> configuration
            .dataEncryptionKeyAccessor(new EntityStorageDataEncryptionKeyAccessor<>(encryptionKeyRepository))
            .unencryptedDataEncryptionKeys();
    }

    record EntityStorageDataEncryptionKeyAccessor<S>(ContentEncryptionKeyRepository contentEncryptionKeyRepository)
        implements DataEncryptionKeyAccessor<S, UnencryptedSymmetricDataEncryptionKey> {

        @Override
        public Collection<UnencryptedSymmetricDataEncryptionKey> findKeys(S entity, ContentProperty contentProperty) {
            var contentId = (UUID) contentProperty.getContentId(entity);
            if (contentId == null) {
                return null;
            }
            return contentEncryptionKeyRepository.findById(contentId).stream()
                .map(encryptionKeyEntity -> new UnencryptedSymmetricDataEncryptionKey(
                    encryptionKeyEntity.getAlgorithm(),
                    encryptionKeyEntity.getEncryptionKey(),
                    encryptionKeyEntity.getIv()
                ))
                .toList();
        }

        @Override
        public S setKeys(S entity, ContentProperty contentProperty,
                         Collection<UnencryptedSymmetricDataEncryptionKey> dataEncryptionKeys
        ) {
            var contentId = (UUID) contentProperty.getContentId(entity);
            var maybeDataEncryptionKey = dataEncryptionKeys.stream().findFirst();

            if (maybeDataEncryptionKey.isEmpty()) {
                contentEncryptionKeyRepository.deleteById(contentId);
                return entity;
            }

            var dataEncryptionKey = maybeDataEncryptionKey.get();

            var encryptionKeyEntity = contentEncryptionKeyRepository.findById(contentId)
                .orElseGet(() -> {
                    var contentEncryptionKey = new ContentEncryptionKey();
                    contentEncryptionKey.setContentId(contentId);
                    return contentEncryptionKey;
                });

            encryptionKeyEntity.setAlgorithm(dataEncryptionKey.algorithm());
            encryptionKeyEntity.setEncryptionKey(dataEncryptionKey.keyData());
            encryptionKeyEntity.setIv(dataEncryptionKey.initializationVector());

            contentEncryptionKeyRepository.save(encryptionKeyEntity);

            return entity;
        }
    }
}