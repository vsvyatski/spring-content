package internal.org.springframework.content.encryption.keys.converter;

import internal.org.springframework.content.encryption.keys.converter.ByteBufferCodec.Field;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;

import java.util.List;

public final class EncryptedSymmetricDataEncryptionKeyConverter {

    private static final ByteBufferCodec<EncryptedSymmetricDataEncryptionKey, Object[]> CODEC = new ByteBufferCodec<>(
            'E',
            List.of(
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::wrappingAlgorithm,
                            (acc, v) -> {
                                acc[0] = v;
                                return acc;
                            }
                    ),
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::wrappingKeyId,
                            (acc, v) -> {
                                acc[1] = v;
                                return acc;
                            }
                    ),
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::wrappingKeyVersion,
                            (acc, v) -> {
                                acc[2] = v;
                                return acc;
                            }
                    ),
                    new Field<>(
                            String.class,
                            EncryptedSymmetricDataEncryptionKey::dataEncryptionAlgorithm,
                            (acc, v) -> {
                                acc[3] = v;
                                return acc;
                            }
                    ),
                    new Field<>(
                            byte[].class,
                            EncryptedSymmetricDataEncryptionKey::encryptedKeyData,
                            (acc, v) -> {
                                acc[4] = v;
                                return acc;
                            }
                    ),
                    new Field<>(
                            byte[].class,
                            EncryptedSymmetricDataEncryptionKey::initializationVector,
                            (acc, v) -> {
                                acc[5] = v;
                                return acc;
                            }
                    )
            ),
            () -> new Object[6],
            acc -> new EncryptedSymmetricDataEncryptionKey(
                    (String) acc[0],
                    (String) acc[1],
                    (String) acc[2],
                    (String) acc[3],
                    (byte[]) acc[4],
                    (byte[]) acc[5]
            )
    );

    private EncryptedSymmetricDataEncryptionKeyConverter() {
    }

    public static byte[] convert(EncryptedSymmetricDataEncryptionKey source) {
        return CODEC.encode(source);
    }

    public static EncryptedSymmetricDataEncryptionKey convert(byte[] bytes) {
        return CODEC.decode(bytes);
    }
}
