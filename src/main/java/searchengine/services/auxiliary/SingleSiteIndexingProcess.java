package searchengine.services.auxiliary;

import lombok.Getter;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.services.DatabaseConnService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;


@Getter
public class SingleSiteIndexingProcess extends Thread{
    private final DatabaseConnService dataService;
    private final Site site;
    private SiteEntity siteRecord;
    final Set<String> foundPages = ConcurrentHashMap.newKeySet();
    private int siteId;

    public SingleSiteIndexingProcess(Site site, DatabaseConnService dataService) {
        this.site = site;
        this.dataService = dataService;
    }

    @Override
    public void run () {

        deleteExistingInfo();

        siteRecord = new SiteEntity();
        siteRecord.setName(site.getName());
        siteRecord.setUrl(site.getUrl());
        siteRecord.setStatus(SiteStatus.INDEXING);
        siteRecord.setStatusTime(LocalDateTime.now());
        siteRecord = dataService.getSiteRepository().saveAndFlush(siteRecord);
        siteId = siteRecord.getId();

        new ForkJoinPool().invoke(new PageIndexingRecursiveTask( "/", this));
    }

    @Override
    public void interrupt() {
        updateSiteStatus(SiteStatus.FAILED, "Interrupted by user");
        // TODO Don't forget flash page records buffer!!!! If exist
        super.interrupt();
    }

    private void deleteExistingInfo() {
        //TODO consider the need to return a value
        Integer siteId = dataService.getSiteRepository().findSiteIdByUrl(site.getUrl());
        if (siteId != null) {
            dataService.getSiteRepository().deleteById(siteId);
            dataService.getPageRepository().deleteAllBySiteIdEquals(siteId);
        }
    }

    public void updateSiteStatus(SiteStatus status, String lastError) {
        dataService.getSiteRepository().updateSiteStatus(status.toString(), siteRecord.getUrl(), lastError, LocalDateTime.now());
    }
}
