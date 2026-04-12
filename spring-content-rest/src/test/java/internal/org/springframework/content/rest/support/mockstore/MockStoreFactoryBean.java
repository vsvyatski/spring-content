package internal.org.springframework.content.rest.support.mockstore;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.core.io.Resource;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockStoreFactoryBean extends AbstractStoreFactoryBean {

    private ContentStore mock;

    protected MockStoreFactoryBean(Class<? extends Store> storeInterface) {
        super(storeInterface);
    }

    public ContentStore getMock() {
        return mock;
    }

    @Override
    protected Object getContentStoreImpl() {
        mock = mock(ContentStore.class);
        when(mock.setContent(any(), any(InputStream.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    return args[0];
                });
        when(mock.setContent(any(), any(Resource.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    return args[0];
                });
        when(mock.setContent(any(), any(PropertyPath.class), any(Resource.class)))
                .thenAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    return args[0];
                });
        return mock;
    }
}
