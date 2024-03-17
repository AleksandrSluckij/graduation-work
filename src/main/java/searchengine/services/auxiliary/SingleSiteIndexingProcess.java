package searchengine.services.auxiliary;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.services.DataBaseConnectionService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Getter
@Slf4j
public class SingleSiteIndexingProcess extends Thread{
    private final DataBaseConnectionService dataService;
    private final Site site;
    private SiteEntity siteRecord;
    final Set<String> foundPages = ConcurrentHashMap.newKeySet();

    public SingleSiteIndexingProcess(Site site, DataBaseConnectionService dataService) {
        this.site = site;
        this.dataService = dataService;
    }

    @Override
    public void run () {

        // TODO For debugging, remove later
        long start = System.currentTimeMillis();

        deleteExistingInfo();

        siteRecord = new SiteEntity();
        siteRecord.setName(site.getName());
        siteRecord.setUrl(site.getUrl());
        siteRecord.setStatus(SiteStatus.INDEXING);
        siteRecord.setStatusTime(LocalDateTime.now());
        siteRecord = dataService.getSiteRepository().saveAndFlush(siteRecord);

        foundPages.add("/");
        new ForkJoinPool().invoke(new PageIndexingRecursiveTask( "/", this));

        // TODO For debugging, remove later
        long end1 = System.currentTimeMillis();

        CollectLemmasService.collectSingleSiteLemmas(siteRecord.getId());


        // TODO For debugging, remove later
        long end2 = System.currentTimeMillis();
        log.info("Site " + siteRecord.getUrl() + " indexed in " + ((System.currentTimeMillis() - start) / 1000) + " sec");
        log.info("Pages found in " + ((end1 - start) / 1000) + " sec");
        log.info("Lemmas indexed in " + ((end2 - end1) / 1000) + " sec");
        log.info("Unique pages: " + foundPages.size());
    }

    @Override
    public void interrupt() {
        Integer siteId = dataService.getSiteRepository().findSiteIdByUrl(site.getUrl());
        if (siteId != null) {
            CollectLemmasService.collectSingleSiteLemmas(siteId);
            updateSiteStatus(SiteStatus.FAILED, "Interrupted by user");
        }
        super.interrupt();
    }

    private void deleteExistingInfo() {

        Integer siteId = dataService.getSiteRepository().findSiteIdByUrl(site.getUrl());
        if (siteId != null) {
            dataService.getLemmaRepository().deleteAllBySiteId(siteId);
            dataService.getIndexRepository().deleteAllBySiteId(siteId);
            dataService.getSiteRepository().deleteById(siteId);
            carefulDeletePages(siteId);

        }
    }

    public void updateSiteStatus(SiteStatus status, String lastError) {
        dataService.getSiteRepository().updateSiteStatus(status.toString(), siteRecord.getUrl(), lastError, LocalDateTime.now());
    }

    private void carefulDeletePages(int siteId) {
        boolean unsuccessful = true;
        while (unsuccessful) {
            try {
                dataService.getPageRepository().deleteAllBySiteIdEquals(siteId);
                unsuccessful = false;
            } catch (CannotAcquireLockException ignored) {
            }
        }
    }
}
