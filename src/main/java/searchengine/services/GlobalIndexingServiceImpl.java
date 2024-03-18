package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingStatus;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.repositories.*;
import searchengine.services.auxiliary.SingleSiteIndexingProcess;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GlobalIndexingServiceImpl implements GlobalIndexingService {

    private final SitesList sitesList;
    private final Set<SingleSiteIndexingProcess> tasks;

    private final PreLemmaRepository preLemmaRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    @Autowired
    public GlobalIndexingServiceImpl(SitesList sitesList, PreLemmaRepository preLemmaRepository, SiteRepository siteRepository,
                                     LemmaRepository lemmaRepository, IndexRepository indexRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.preLemmaRepository = preLemmaRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        tasks = ConcurrentHashMap.newKeySet();
    }

    @Override
    @Async
    public void startTotalIndexing() {
        IndexingStatus.setIndexingTrue();
        preLemmaRepository.initPreLemmaTable();
        for (Site site : sitesList.getSites()) {
            SingleSiteIndexingProcess task = new SingleSiteIndexingProcess(site, siteRepository, lemmaRepository, indexRepository, pageRepository, preLemmaRepository);
            tasks.add(task);
            task.start();
        }
        monitorThreadsAlive(tasks);
    }

    private void monitorThreadsAlive(Set<SingleSiteIndexingProcess> tasks) {
        while (!tasks.isEmpty()) {
            tasks.removeIf(indexingProcess -> indexingProcess.getState() == Thread.State.TERMINATED);
        }
        IndexingStatus.setIndexingFalse();
    }

    @Override
    public void stopTotalIndexing() {
        for (SingleSiteIndexingProcess indexingProcess : tasks) {
            indexingProcess.interrupt();
        }
        tasks.clear();
        IndexingStatus.setIndexingFalse();
    }

}
