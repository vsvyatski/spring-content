package internal.org.springframework.content.encryption.keys.converter;

import static org.assertj.core.api.Assertions.assertThat;


import jakarta.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.Test;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.EncryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;

public class UnencryptedSymmetricDataEncryptionKeyConverterTest {
    @Test
    public void encodesAndDecodes() throws Throwable {
        var key = new UnencryptedSymmetricDataEncryptionKey(
                "ABC",
                new byte[5],
                DatatypeConverter.parseHexBinary("ff504000f523659020")
        );

        var encoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var decoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded).isEqualTo(key);
    }

    @Test
    public void handlesEmptyObject() throws Throwable {
        var key = new UnencryptedSymmetricDataEncryptionKey(
                "",
                new byte[0],
                new byte[0]
        );

        var encoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(key);

        var decoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded).isEqualTo(key);
    }

    @Test
    public void doesNotDecodeDifferentType() throws Throwable {
        var encryptedKey = new EncryptedSymmetricDataEncryptionKey(
                "test",
                "123",
                "8",
                "ABC",
                new byte[5],
                new byte[4]
        );

        var encoded = EncryptedSymmetricDataEncryptionKeyConverter.convert(encryptedKey);

        var decoded = UnencryptedSymmetricDataEncryptionKeyConverter.convert(encoded);

        assertThat(decoded).isNull();
    }

}