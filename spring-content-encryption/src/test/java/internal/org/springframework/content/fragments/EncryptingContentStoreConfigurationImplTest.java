package internal.org.springframework.content.fragments;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration.ContentEncryptionMethod;
import org.springframework.core.convert.support.GenericConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vladimir Svyatski
 */
class EncryptingContentStoreConfigurationImplTest {

    @Test
    void fluentChainIncludesConfigureConverters() {
        EncryptingContentStoreConfiguration<Object> config = new EncryptingContentStoreConfigurationImpl<>();
        AtomicBoolean convertersConfigured = new AtomicBoolean();

        assertThat(config.encryptionKeyContentProperty("key")
            .configureConverters(registry -> {
                convertersConfigured.set(true);
                registry.addConverter(String.class, Long.class, Long::parseLong);
                assertThat(registry).isInstanceOf(GenericConversionService.class);
                assertThat(((GenericConversionService) registry).canConvert(String.class, Long.class)).isTrue();
            })
            .unencryptedDataEncryptionKeys()
            .contentEncryptionMethod(ContentEncryptionMethod.AES_CTR_128)).isSameAs(config);

        assertThat(convertersConfigured).isTrue();
    }

    @Test
    void encryptingContentStoreConfigurerChainsConfigureConverters() {
        var config = new EncryptingContentStoreConfigurationImpl<>();
        AtomicBoolean convertersConfigured = new AtomicBoolean();
        EncryptingContentStoreConfigurer<Object> configurer = configuration -> configuration
            .encryptionKeyContentProperty("key")
            .configureConverters(registry -> {
                convertersConfigured.set(true);
                registry.addConverter(String.class, Integer.class, Integer::parseInt);
            })
            .unencryptedDataEncryptionKeys()
            .contentEncryptionMethod(ContentEncryptionMethod.AES_CTR_128);

        configurer.configure(config);

        assertThat(convertersConfigured).isTrue();
    }
}
