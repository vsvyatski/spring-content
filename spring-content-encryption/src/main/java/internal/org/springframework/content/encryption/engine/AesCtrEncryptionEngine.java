package internal.org.springframework.content.encryption.engine;

import org.springframework.content.encryption.engine.ContentEncryptionEngine;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Symmetric data encryption engine using AES-CTR encryption mode
 */
public class AesCtrEncryptionEngine implements ContentEncryptionEngine {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int AES_BLOCK_SIZE_BYTES = 16; // AES has a 128-bit block size
    private static final int IV_SIZE_BYTES = AES_BLOCK_SIZE_BYTES; // IV is the same size as a block
    private final KeyGenerator keyGenerator;

    public AesCtrEncryptionEngine(int keySizeBits) {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The key generator for AES is not available.", e);
        }
        keyGenerator.init(keySizeBits, secureRandom);
    }

    private static long calculateBlockOffset(long offsetBytes) {
        return (offsetBytes - (offsetBytes % AES_BLOCK_SIZE_BYTES)) / AES_BLOCK_SIZE_BYTES;
    }

    @Override
    public EncryptionParameters createNewParameters() {
        var secretKey = keyGenerator.generateKey();
        byte[] iv = new byte[IV_SIZE_BYTES];
        secureRandom.nextBytes(iv);
        return new EncryptionParameters(
                secretKey,
                iv
        );
    }

    private Cipher initializeCipher(EncryptionParameters parameters, boolean forEncryption)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(
                forEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                parameters.secretKey(),
                new IvParameterSpec(parameters.initializationVector())
        );

        return cipher;
    }

    @Override
    public InputStream encrypt(InputStream plainText, EncryptionParameters encryptionParameters) {
        try {
            return new CipherInputStream(plainText, initializeCipher(encryptionParameters, true));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize AES-CTR cipher", e);
        }
    }

    @Override
    public InputStream decrypt(
            Function<InputStreamRequestParameters, InputStream> cipherTextStreamRequest,
            EncryptionParameters encryptionParameters,
            InputStreamRequestParameters requestParameters
    ) {
        var blockStartOffset = calculateBlockOffset(requestParameters.startByteOffset());

        var adjustedIv = adjustIvForOffset(encryptionParameters.initializationVector(), blockStartOffset);

        var adjustedParameters = new EncryptionParameters(
                encryptionParameters.secretKey(),
                adjustedIv
        );

        var byteStartOffset = blockStartOffset * AES_BLOCK_SIZE_BYTES;

        var cipherTextStream = cipherTextStreamRequest.apply(requestParameters);

        Cipher cipher;
        try {
            cipher = initializeCipher(adjustedParameters, false);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize AES-CTR cipher", e);
        }

        return new ZeroPrefixedInputStream(
                new EnsureSingleSkipInputStream(
                        new CipherInputStream(
                                new SkippingInputStream(
                                        cipherTextStream,
                                        byteStartOffset
                                ),
                                cipher
                        )
                ),
                byteStartOffset
        );
    }

    private byte[] adjustIvForOffset(byte[] iv, long offsetBlocks) {
        // Optimization: no need to adjust the IV when we have no block offset
        if (offsetBlocks == 0) {
            return iv;
        }

        // AES-CTR works by having a separate IV for every block.
        // This block IV is built from the initial IV and the block counter.
        var initialIv = new BigInteger(1, iv);
        byte[] bigintBytes = initialIv.add(BigInteger.valueOf(offsetBlocks))
                .toByteArray();

        // Because we're using BigInteger for math here,
        // the resulting byte array may be longer (when overflowing the IV size, we should wrap around)
        // or shorter (when our IV starts with a bunch of 0)
        // It needs to be the proper length, and aligned properly
        if (bigintBytes.length == AES_BLOCK_SIZE_BYTES) {
            return bigintBytes;
        } else if (bigintBytes.length > AES_BLOCK_SIZE_BYTES) {
            // Byte array is longer, we need to cut a part of the front
            return Arrays.copyOfRange(bigintBytes, bigintBytes.length - IV_SIZE_BYTES, bigintBytes.length);
        } else {
            // Byte array is shorter, we need to pad the front with 0 bytes
            // Note that a bytes array is initialized to be all-zero by default
            byte[] ivBytes = new byte[IV_SIZE_BYTES];
            System.arraycopy(bigintBytes, 0, ivBytes, IV_SIZE_BYTES - bigintBytes.length, bigintBytes.length);
            return ivBytes;
        }
    }

}
