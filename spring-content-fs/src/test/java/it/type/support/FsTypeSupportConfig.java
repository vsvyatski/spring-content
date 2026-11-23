package it.type.support;

import org.springframework.content.test.support.id.type.BigIntegerBasedContentEntityStore;
import org.springframework.content.test.support.id.type.LongBasedContentEntityStore;
import org.springframework.content.test.support.id.type.URIBasedContentEntityStore;
import org.springframework.content.test.support.id.type.UUIDBasedContentEntityStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FsTypeSupportConfig {
    @Bean
    public UUIDBasedContentEntityStore uuidStore() {
        return new UUIDBasedContentEntityStore();
    }

    @Bean
    public URIBasedContentEntityStore uriStore() {
        return new URIBasedContentEntityStore();
    }

    @Bean
    public LongBasedContentEntityStore longStore() {
        return new LongBasedContentEntityStore();
    }

    @Bean
    public BigIntegerBasedContentEntityStore bigIntStore() {
        return new BigIntegerBasedContentEntityStore();
    }
}
