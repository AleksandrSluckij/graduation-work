package searchengine.services;

import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;

public interface DatabaseConnService {
    SiteRepository getSiteRepository ();
    PageRepository getPageRepository ();
    String getUserAgent ();
    String getReferer ();
}
