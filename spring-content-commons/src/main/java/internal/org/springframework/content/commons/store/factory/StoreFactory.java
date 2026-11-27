package internal.org.springframework.content.commons.store.factory;

import org.springframework.content.commons.store.Store;

import java.io.Serializable;

public interface StoreFactory {
    Class<? extends Store<? extends Serializable>> getStoreInterface();

    <T> T getStore();
}
