package internal.org.springframework.content.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.fs.config.FileSystemStoreConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import java.net.URI;
import java.util.List;

@Configuration
public class FileSystemStoreConfiguration {

    @Autowired(required = false)
    private List<FileSystemStoreConfigurer> configurers;

    @Bean
    public PlacementService filesystemStorePlacementService() {
        PlacementService conversion = new PlacementServiceImpl();
        conversion.addConverter(new URIStringConverter());

        addConverters(conversion);
        return conversion;
    }

    protected void addConverters(ConverterRegistry registry) {
        if (configurers == null)
            return;

        for (FileSystemStoreConfigurer configurer : configurers) {
            configurer.configureFileSystemStoreConverters(registry);
        }
    }

    /**
     * Important! We have a dedicated class here instead of in-place interface creation (I guess the compiler just
     * generates similar class on its own). The problem is, certain IDEs will show a warning and offer you to
     * transform that into a lambda expression. Unfortunately, that causes an exception at runtime. To avoid that
     * warning from IDEs we just implement the interface ourselves.
     */
    private static class URIStringConverter implements Converter<URI, String> {
        @Override
        public String convert(URI source) {
            return source.toString();
        }
    }
}
