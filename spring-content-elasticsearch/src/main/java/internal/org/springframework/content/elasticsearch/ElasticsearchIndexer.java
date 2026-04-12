package internal.org.springframework.content.elasticsearch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.events.AbstractStoreEventListener;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.IndexService;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

	public static final String INDEX_NAME = "spring-content-fulltext-index";

	private static final Log LOGGER = LogFactory.getLog(ElasticsearchIndexer.class);

	private final IndexService indexService;

	public ElasticsearchIndexer(IndexService indexService) {
		this.indexService = indexService;
	}

	@Override
	protected void onAfterSetContent(AfterSetContentEvent event) {
		if (event.getStore() instanceof ContentStore) {
			this.indexService.index(event.getSource(), ((ContentStore)event.getStore()).getContent(event.getSource()));
		}
	}

	@Override
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
		this.indexService.unindex(event.getSource());
	}
}