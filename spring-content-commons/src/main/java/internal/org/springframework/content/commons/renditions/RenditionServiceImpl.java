package internal.org.springframework.content.commons.renditions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.util.MimeType;

import java.io.InputStream;
import java.util.*;

public class RenditionServiceImpl implements RenditionService {

    private final List<RenditionProvider> providers = new ArrayList<>();

    @Autowired(required = false)
    public RenditionServiceImpl(RenditionProvider... providers) {
        Collections.addAll(this.providers, providers);
    }

    @Override
    public boolean canConvert(String fromMimeType, String toMimeType) {
        for (RenditionProvider provider : providers) {
            if (MimeType.valueOf(fromMimeType).includes(MimeType.valueOf(provider.consumes()))) {
                for (String produce : provider.produces()) {
                    if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf(produce))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String[] conversions(String fromMimeType) {
        Set<String> conversions = new HashSet<>();
        for (RenditionProvider provider : providers) {
            if (provider.consumes().equals(fromMimeType)) {
                conversions.addAll(Arrays.asList(provider.produces()));
            }
        }
        return conversions.toArray(new String[]{});
    }

    @Override
    public InputStream convert(String fromMimeType, InputStream fromInputSource, String toMimeType) {
        for (RenditionProvider provider : providers) {
            if (MimeType.valueOf(fromMimeType)
                    .includes(MimeType.valueOf(provider.consumes()))) {
                for (String produce : provider.produces()) {
                    if (MimeType.valueOf(toMimeType)
                            .includes(MimeType.valueOf(produce))) {
                        return provider.convert(fromInputSource, toMimeType);
                    }
                }
            }
        }
        return null;
    }
}
