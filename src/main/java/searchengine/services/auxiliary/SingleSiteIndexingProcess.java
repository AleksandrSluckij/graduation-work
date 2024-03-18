package searchengine.services.auxiliary;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.model.repositories.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Getter
@Slf4j
public class SingleSiteIndexingProcess extends Thread {

    private final Site site;
    private SiteEntity siteRecord;
    final Set<String> foundPages = ConcurrentHashMap.newKeySet();

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final PreLemmaRepository preLemmaRepository;

    public SingleSiteIndexingProcess(Site site, SiteRepository siteRepository, LemmaRepository lemmaRepository,
                                     IndexRepository indexRepository, PageRepository pageRepository, PreLemmaRepository preLemmaRepository) {
        this.site = site;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.preLemmaRepository = preLemmaRepository;
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
        siteRecord = siteRepository.saveAndFlush(siteRecord);

        foundPages.add("/");
        new ForkJoinPool().invoke(new PageIndexingRecursiveTask( "/", this));

        // TODO For debugging, remove later
        long end1 = System.currentTimeMillis();

        CollectLemmasService.collectSingleSiteLemmas(siteRecord.getId(), lemmaRepository, indexRepository);


        // TODO For debugging, remove later
        long end2 = System.currentTimeMillis();
        log.info("Site " + siteRecord.getUrl() + " indexed in " + ((System.currentTimeMillis() - start) / 1000) + " sec");
        log.info("Pages found in " + ((end1 - start) / 1000) + " sec");
        log.info("Lemmas indexed in " + ((end2 - end1) / 1000) + " sec");
        log.info("Unique pages: " + foundPages.size());
    }

    @Override
    public void interrupt() {
        Integer siteId = siteRepository.findSiteIdByUrl(site.getUrl());
        if (siteId != null) {
            CollectLemmasService.collectSingleSiteLemmas(siteId, lemmaRepository, indexRepository);
            updateSiteStatus(SiteStatus.FAILED, "Interrupted by user");
        }
        super.interrupt();
    }

    private void deleteExistingInfo() {

        Integer siteId = siteRepository.findSiteIdByUrl(site.getUrl());
        if (siteId != null) {
            lemmaRepository.deleteAllBySiteId(siteId);
            indexRepository.deleteAllBySiteId(siteId);
            siteRepository.deleteById(siteId);
            carefulDeletePages(siteId);

        }
    }

    public void updateSiteStatus(SiteStatus status, String lastError) {
        siteRepository.updateSiteStatus(status.toString(), siteRecord.getUrl(), lastError, LocalDateTime.now());
    }

    private void carefulDeletePages(int siteId) {
        boolean unsuccessful = true;
        while (unsuccessful) {
            try {
                pageRepository.deleteAllBySiteIdEquals(siteId);
                unsuccessful = false;
            } catch (CannotAcquireLockException ignored) {
            }
        }
    }
}
