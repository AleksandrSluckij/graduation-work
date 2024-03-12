package searchengine.services;

import searchengine.model.*;

public interface DataBaseConnectionService {
    SiteRepository getSiteRepository ();
    PageRepository getPageRepository ();
    LemmaRepository getLemmaRepository ();
    IndexRepository getIndexRepository ();
    PreLemmaRepository getPreLemmaRepository ();
    PreResultRepository getPreResultRepository ();
    
}
