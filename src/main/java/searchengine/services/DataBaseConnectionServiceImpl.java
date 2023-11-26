package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;

@Component
public class DataBaseConnectionServiceImpl implements DatabaseConnService{
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Autowired
    public DataBaseConnectionServiceImpl(SiteRepository siteRepository, PageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @Value("${user-agent-name}")
    String userAgent;

    @Value("${referer-name}")
    String referer;

    @Override
    public SiteRepository getSiteRepository() {
        return siteRepository;
    }

    @Override
    public PageRepository getPageRepository() {
        return pageRepository;
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String getReferer () {
        return referer;
    }
}
