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
public class GlobalIndexingServiceImpl implements GlobalIndexingService {

    private final SitesList sitesList;
    private final Set<SingleSiteIndexingProcess> tasks;
    private final DataBaseConnectionService dataService;


    @Autowired
    public GlobalIndexingServiceImpl(SitesList sitesList, DataBaseConnectionService dataService) {
        this.sitesList = sitesList;
        this.dataService = dataService;
        tasks = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void startTotalIndexing() {
        IndexingStatus.setIndexingTrue();
        for (Site site : sitesList.getSites()) {
            SingleSiteIndexingProcess task = new SingleSiteIndexingProcess(site, dataService);
            tasks.add(task);
            task.start();
        }
        monitorThreadsAlive(tasks);
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
    public void stopTotalIndexing() {
        for (SingleSiteIndexingProcess indexingProcess : tasks) {
            indexingProcess.interrupt();
        }
        tasks.clear();
        IndexingStatus.setIndexingFalse();
    }

}
