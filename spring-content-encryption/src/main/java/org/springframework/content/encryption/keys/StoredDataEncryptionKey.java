package org.springframework.content.encryption.keys;

import java.util.Arrays;
import java.util.Objects;

/**
 * Representation of the stored data encryption key
 */
public sealed interface StoredDataEncryptionKey {

    /**
     * An unencrypted symmetric data encryption key
     *
     * @param algorithm            the encryption algorithm used for data encryption
     * @param keyData              the symmetric key for data encryption
     * @param initializationVector the IV for the encryption algorithm
     */
    record UnencryptedSymmetricDataEncryptionKey(
            String algorithm,
            byte[] keyData,
            byte[] initializationVector
    ) implements StoredDataEncryptionKey {

        @Override
        public boolean equals(Object o) {
            return o instanceof UnencryptedSymmetricDataEncryptionKey other
                    && Objects.equals(algorithm, other.algorithm)
                    && Arrays.equals(keyData, other.keyData)
                    && Arrays.equals(initializationVector, other.initializationVector);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(algorithm);
            result = 31 * result + Arrays.hashCode(keyData);
            result = 31 * result + Arrays.hashCode(initializationVector);
            return result;
        }
    }

    /**
     * An encrypted symmetric data encryption key
     *
     * @param wrappingAlgorithm       the encryption algorithm used for key encryption
     * @param wrappingKeyId           the identifier for the wrapping key that was used for key encryption
     * @param wrappingKeyVersion      the version of the wrapping key that was used for key encryption
     * @param dataEncryptionAlgorithm the encryption algorithm used for data encryption
     * @param encryptedKeyData        the encrypted data encryption key
     * @param initializationVector    the IV for the encryption algorithm
     */
    record EncryptedSymmetricDataEncryptionKey(
            String wrappingAlgorithm,
            String wrappingKeyId,
            String wrappingKeyVersion,
            String dataEncryptionAlgorithm,
            byte[] encryptedKeyData,
            byte[] initializationVector
    ) implements StoredDataEncryptionKey {

        @Override
        public boolean equals(Object o) {
            return o instanceof EncryptedSymmetricDataEncryptionKey other
                    && Objects.equals(wrappingAlgorithm, other.wrappingAlgorithm)
                    && Objects.equals(wrappingKeyId, other.wrappingKeyId)
                    && Objects.equals(wrappingKeyVersion, other.wrappingKeyVersion)
                    && Objects.equals(dataEncryptionAlgorithm, other.dataEncryptionAlgorithm)
                    && Arrays.equals(encryptedKeyData, other.encryptedKeyData)
                    && Arrays.equals(initializationVector, other.initializationVector);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(wrappingAlgorithm, wrappingKeyId, wrappingKeyVersion, dataEncryptionAlgorithm);
            result = 31 * result + Arrays.hashCode(encryptedKeyData);
            result = 31 * result + Arrays.hashCode(initializationVector);
            return result;
        }
    }
}
