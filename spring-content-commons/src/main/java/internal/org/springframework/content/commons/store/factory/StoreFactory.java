package internal.org.springframework.content.commons.store.factory;

import org.springframework.content.commons.store.Store;

public interface StoreFactory {
    Class<? extends Store> getStoreInterface();

    <T> T getStore();
}
