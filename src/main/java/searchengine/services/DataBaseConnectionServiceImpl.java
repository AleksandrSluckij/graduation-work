package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.*;

@Component
public class DataBaseConnectionServiceImpl implements DataBaseConnectionService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PreLemmaRepository preLemmaRepository;

    @Autowired
    public DataBaseConnectionServiceImpl(SiteRepository siteRepository, PageRepository pageRepository,
                                         LemmaRepository lemmaRepository, IndexRepository indexRepository,
                                         PreLemmaRepository preLemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.preLemmaRepository = preLemmaRepository;
    }

    @Override
    public SiteRepository getSiteRepository() {
        return siteRepository;
    }

    @Override
    public PageRepository getPageRepository() {
        return pageRepository;
    }

    @Override
    public LemmaRepository getLemmaRepository() {
        return lemmaRepository;
    }

    @Override
    public IndexRepository getIndexRepository() {
        return indexRepository;
    }

    @Override
    public PreLemmaRepository getPreLemmaRepository() {
        return preLemmaRepository;
    }

}
