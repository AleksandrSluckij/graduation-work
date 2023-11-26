package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingStatus;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.services.auxiliary.SingleSiteIndexingProcess;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IndexingServiceImpl implements IndexingService{

    private final SitesList sitesList;
    private Set<SingleSiteIndexingProcess> tasks;
    private DatabaseConnService dataService;


    @Autowired
    public IndexingServiceImpl(SitesList sitesList, DatabaseConnService dataService) {
        this.sitesList = sitesList;
        this.dataService = dataService;
        tasks = ConcurrentHashMap.newKeySet();
    }

    @Override
    public String startTotalIndexing(DatabaseConnService dataService) {
        if (IndexingStatus.isAlreadyIndexing()) return "Индексация уже запущена";
        IndexingStatus.setIndexingTrue();
        for (Site site : sitesList.getSites()) {
            SingleSiteIndexingProcess task = new SingleSiteIndexingProcess(site, dataService);
            tasks.add(task);
            task.start();
        }
        monitorThreadsAlive(tasks);
        return null;
    }

    @Async
    private void monitorThreadsAlive(Set<SingleSiteIndexingProcess> tasks) {
        while (!tasks.isEmpty()) {
            for (SingleSiteIndexingProcess indexingProcess : tasks) {
                if (indexingProcess.getState() == Thread.State.TERMINATED) {
                    tasks.remove(indexingProcess);
                }
            }
        }
        IndexingStatus.setIndexingFalse();
    }

    @Override
    public String stopTotalIndexing() {
        if (!IndexingStatus.isAlreadyIndexing()) return "Индексация не запущена";
        for (SingleSiteIndexingProcess indexingProcess : tasks) {
            indexingProcess.interrupt();
        }
        tasks.clear();
        IndexingStatus.setIndexingFalse();
        return null;
    }

}
