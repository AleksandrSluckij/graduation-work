package searchengine.services;

import searchengine.model.repositories.*;

public interface DataBaseConnectionService {
    SiteRepository getSiteRepository ();
    PageRepository getPageRepository ();
    LemmaRepository getLemmaRepository ();
    IndexRepository getIndexRepository ();
    PreLemmaRepository getPreLemmaRepository ();
    PreResultRepository getPreResultRepository ();
    
}
