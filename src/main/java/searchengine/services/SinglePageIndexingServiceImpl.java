package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingStatus;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ResultOfPageReading;
import searchengine.model.*;
import searchengine.model.repositories.*;
import searchengine.services.auxiliary.CollectLemmasService;
import searchengine.services.auxiliary.CommonAddrActions;
import searchengine.services.auxiliary.PageReadingService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SinglePageIndexingServiceImpl implements SinglePageIndexingService {

    private final SitesList sitesList;

    private final PreLemmaRepository preLemmaRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;



    @Autowired
    public SinglePageIndexingServiceImpl(SitesList sitesList, PreLemmaRepository preLemmaRepository, SiteRepository siteRepository,
                                         PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.sitesList = sitesList;
        this.preLemmaRepository = preLemmaRepository;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public String indexSinglePage(String pageAddr) {

        if (pageAddrNotCorrect(pageAddr)) {
            return "Ошибка в адресе страницы, либо данная страница находится за пределами сайтов, указанных в конфигурационном файле";
        }

        ResultOfPageReading readingResult = PageReadingService.readPage(pageAddr);
        if (readingResult.getCode() != 200) {
            return "Страница недоступна. Код ошибки " + readingResult.getCode() + ", " + readingResult.getError();
        }

        PageEntity page = new PageEntity();
        page.setPath(CommonAddrActions.extractPagePath(pageAddr));
        page.setContent(readingResult.getContent());
        page.setCode(readingResult.getCode());
        SiteEntity site = siteRepository.findByUrlEquals(CommonAddrActions.extractSiteHost(pageAddr));

        if (site == null) {
            Site siteInConfig = sitesList.getSites().stream()
                    .filter(s -> s.getUrl().equals(CommonAddrActions.extractSiteHost(pageAddr)))
                    .findFirst().orElse(null);
            if (siteInConfig == null) {
                return "Ошибка определения сайта " + CommonAddrActions.extractSiteHost(pageAddr);
            }

            site = new SiteEntity();
            site.setUrl(siteInConfig.getUrl());
            site.setName(siteInConfig.getName());
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("Never indexed");
            site.setStatusTime(LocalDateTime.now());
            site = siteRepository.saveAndFlush(site);
        }

        page.setSiteId(site.getId());

        removeExistingData(site.getId(), page.getPath());

        page = pageRepository.saveAndFlush(page);

        IndexingStatus.setIndexingTrue();
        siteRepository.updateSiteStatus(SiteStatus.INDEXING.toString(), site.getUrl(), "", LocalDateTime.now());
        preLemmaRepository.initPreLemmaTable();
        CollectLemmasService.extractLemmasFromPage(page, preLemmaRepository);
        CollectLemmasService.collectSinglePageLemmas(site.getId(), page.getId(), preLemmaRepository, siteRepository, pageRepository, lemmaRepository, indexRepository);
        IndexingStatus.setIndexingFalse();
        siteRepository.updateSiteStatus(SiteStatus.INDEXED.toString(), site.getUrl(), "", LocalDateTime.now());
        return null;
    }

    private void removeExistingData(int siteId, String pagePath) {

        PageEntity page = pageRepository.findPageByPathAndSiteId(pagePath, siteId);
        if (page == null) {
            return;
        }
        pageRepository.delete(page);

        List<IndexEntity> pageIndexes = indexRepository.findAllByPageIdEquals(page.getId());
        if (pageIndexes.isEmpty()) {
            return;
        }
        indexRepository.deleteAll(pageIndexes);

        List<Integer> pageLemmasIds = pageIndexes.stream().map(IndexEntity::getLemmaId).toList();

        lemmaRepository.decreaseLemmaFrequencyById(pageLemmasIds);

        lemmaRepository.deleteEmptyLemmas();
    }

    private boolean pageAddrNotCorrect(String pageAddr) {
        String url = CommonAddrActions.extractSiteHost(pageAddr);
        if (url == null) {
            return true;
        }
        List<String> urlsInConfig = sitesList.getSites().stream().map(Site :: getUrl).toList();
        return !urlsInConfig.contains(url);
    }
}
