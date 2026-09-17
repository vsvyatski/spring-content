package internal.org.springframework.content.encryption.keys.converter;

import internal.org.springframework.content.encryption.keys.converter.ByteBufferCodec.Field;
import java.util.List;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;

public final class UnencryptedSymmetricDataEncryptionKeyConverter {

    private UnencryptedSymmetricDataEncryptionKeyConverter() {
    }
    private static final ByteBufferCodec<UnencryptedSymmetricDataEncryptionKey, Object[]> CODEC = new ByteBufferCodec<>(
            'U',
            List.of(
                    new Field<>(
                            String.class,
                            UnencryptedSymmetricDataEncryptionKey::algorithm,
                            (acc, v) -> { acc[0] = v; return acc; }
                    ),
                    new Field<>(
                            byte[].class,
                            UnencryptedSymmetricDataEncryptionKey::keyData,
                            (acc, v) -> { acc[1] = v; return acc; }
                    ),
                    new Field<>(
                            byte[].class,
                            UnencryptedSymmetricDataEncryptionKey::initializationVector,
                            (acc, v) -> { acc[2] = v; return acc; }
                    )
            ),
            () -> new Object[3],
            acc -> new UnencryptedSymmetricDataEncryptionKey((String) acc[0], (byte[]) acc[1], (byte[]) acc[2])
    );

    public static byte[] convert(UnencryptedSymmetricDataEncryptionKey source) {
        return CODEC.encode(source);
    }

    public static UnencryptedSymmetricDataEncryptionKey convert(byte[] bytes) {
        return CODEC.decode(bytes);
    }
}
