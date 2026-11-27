package internal.org.springframework.data.rest.extensions.contentsearch;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.webmvc.RootResourceInformation;

import java.util.List;
import java.util.Objects;

public class DefaultEntityLookupStrategy {

    public void lookup(RootResourceInformation rri, List<Object> contentIds, List<Object> results) {
        RepositoryInvoker invoker = rri.getInvoker();
        if (invoker == null) {
            return;
        }

        for (Object entity : invoker.invokeFindAll(Pageable.unpaged())) {
            for (Object contentId : contentIds) {
                Object candidate = BeanUtils.getAnnotatedValue(entity, ContentId.class);
                if (Objects.equals(contentId, candidate)) {
                    results.add(entity);
                }
            }
        }
    }
}
