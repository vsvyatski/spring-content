package internal.org.springframework.content.commons.config;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;

public class StoreFragments extends ArrayList<StoreFragment<?>> {

    @Serial
    private static final long serialVersionUID = 5978772614353539244L;

    public StoreFragments(Collection<? extends StoreFragment<?>> fragments) {
        super(fragments);
    }
}
