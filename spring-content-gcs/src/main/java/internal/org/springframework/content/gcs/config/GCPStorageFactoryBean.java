package internal.org.springframework.content.gcs.config;

import com.google.cloud.spring.storage.GoogleStorageProtocolResolver;
import com.google.cloud.storage.Storage;
import internal.org.springframework.content.gcs.store.DefaultGCPStorageImpl;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.store.Store;
import org.springframework.content.commons.store.factory.AbstractStoreFactoryBean;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.versions.LockingAndVersioningProxyFactory;

@SuppressWarnings("rawtypes")
public class GCPStorageFactoryBean extends AbstractStoreFactoryBean {

    private ApplicationContext context;

    private Storage client;

    private PlacementService s3StorePlacementService;

    private GoogleStorageProtocolResolver resolver;

    @Autowired(required = false)
    private MappingContext mappingContext;

    @Autowired(required = false)
    private LockingAndVersioningProxyFactory versioning;

    public GCPStorageFactoryBean(Class<? extends Store> storeInterface) {
        super(storeInterface);
    }

    @Autowired
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    @Autowired
    public void setClient(Storage client) {
        this.client = client;
    }

    @Autowired
    public void setS3StorePlacementService(PlacementService s3StorePlacementService) {
        this.s3StorePlacementService = s3StorePlacementService;
    }

    @Autowired
    public void setResolver(GoogleStorageProtocolResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void addProxyAdvice(ProxyFactory result, BeanFactory beanFactory) {
        if (versioning != null) {
            versioning.apply(result);
        }
    }

    @Override
    protected Object getContentStoreImpl() {

        DefaultResourceLoader loader = new DefaultResourceLoader();
        loader.addProtocolResolver(resolver);

        return new DefaultGCPStorageImpl(context, loader, mappingContext, s3StorePlacementService, client);
    }
}
