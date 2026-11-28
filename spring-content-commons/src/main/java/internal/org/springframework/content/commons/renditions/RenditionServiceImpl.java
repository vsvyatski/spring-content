package internal.org.springframework.content.commons.renditions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.util.MimeType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenditionServiceImpl implements RenditionService {

    private final List<RenditionProvider> providers = new ArrayList<>();

    @Autowired(required = false)
    public RenditionServiceImpl(RenditionProvider... providers) {
        Collections.addAll(this.providers, providers);
    }

    @Override
    public boolean canConvert(String fromMimeType, String toMimeType) {
        return providerFor(fromMimeType, toMimeType) != null;
    }

    @Override
    public InputStream convert(InputStream fromInputSource, String fromMimeType, String toMimeType) {
        RenditionProvider provider = providerFor(fromMimeType, toMimeType);
        if (provider == null) {
            return null;
        }
        return provider.convert(fromInputSource, toMimeType);
    }

    private RenditionProvider providerFor(String fromMimeType, String toMimeType) {
        MimeType from = MimeType.valueOf(fromMimeType);
        MimeType to = MimeType.valueOf(toMimeType);
        for (RenditionProvider provider : providers) {
            if (!from.includes(MimeType.valueOf(provider.consumes()))) {
                continue;
            }
            for (String produce : provider.produces()) {
                if (to.includes(MimeType.valueOf(produce))) {
                    return provider;
                }
            }
        }
        return null;
    }
}
