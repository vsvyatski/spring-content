package internal.org.springframework.content.rest.support;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.renditions.RenditionProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal rendition providers shared by REST integration test configurations.
 *
 * @author Vladimir Syatsky
 */
public final class TestRenditionProviders {

    private TestRenditionProviders() {
    }

    public static RenditionProvider textPlainToHtml() {
        return minimalHtmlRendition("text/plain");
    }

    public static RenditionProvider htmlToHtml() {
        return minimalHtmlRendition("text/html");
    }

    private static RenditionProvider minimalHtmlRendition(String consumes) {
        return new RenditionProvider() {

            @Override
            public String consumes() {
                return consumes;
            }

            @Override
            public String[] produces() {
                return new String[]{"text/html"};
            }

            @Override
            public InputStream convert(InputStream fromInputSource, String toMimeType) {
                String input;
                try {
                    input = IOUtils.toString(fromInputSource, StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    input = "";
                }
                return new ByteArrayInputStream(
                    String.format("<html><body>%s</body></html>", input).getBytes(StandardCharsets.UTF_8));
            }
        };
    }
}
