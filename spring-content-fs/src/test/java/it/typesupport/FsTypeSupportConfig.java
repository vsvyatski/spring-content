package it.typesupport;

import org.springframework.content.test.support.idtype.BigIntegerBasedContentEntityStore;
import org.springframework.content.test.support.idtype.LongBasedContentEntityStore;
import org.springframework.content.test.support.idtype.URIBasedContentEntityStore;
import org.springframework.content.test.support.idtype.UUIDBasedContentEntityStore;
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
