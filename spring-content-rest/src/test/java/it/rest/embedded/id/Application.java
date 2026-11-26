package it.rest.embedded.id;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.content.fs.config.EnableFileSystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true, basePackages = {"it.rest.embedded.id"})
@EnableFileSystemStores(basePackages = "it.rest.embedded.id")
@Import({RestConfiguration.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public FileSystemResourceLoader filesystemRoot() throws IOException {
        return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
    }

    @Bean
    public ContentRestConfigurer configureConversionService() {
        return config -> config.converters().addConverter((Converter<String, EmbeddedIdTest.TestEntityId>) source -> {
            String[] segments = source.split("_");
            return new EmbeddedIdTest.TestEntityId(segments[0], segments[1]);
        });
    }
}
