package internal.org.springframework.content.rest.support.mock.store;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.content.test.support.CommonsTestStores;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.io.Serializable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MockStoreFactoryBean extends AbstractStoreFactoryBean {

    private ContentStore<Object, Serializable> mock;

    protected MockStoreFactoryBean(Class<? extends Store<?>> storeInterface) {
        super(storeInterface);
    }

    public ContentStore<Object, Serializable> getMock() {
        return mock;
    }

    @Override
    protected Object getContentStoreImpl() {
        mock = CommonsTestStores.mockContentStore();
        when(mock.setContent(any(), any(InputStream.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mock.setContent(any(), any(Resource.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mock.setContent(any(), any(PropertyPath.class), any(Resource.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        return mock;
    }
}
