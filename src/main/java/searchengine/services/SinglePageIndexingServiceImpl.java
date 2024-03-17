package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.IndexingStatus;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.ResultOfPageReading;
import searchengine.model.*;
import searchengine.services.auxiliary.CollectLemmasService;
import searchengine.services.auxiliary.CommonAddrActions;
import searchengine.services.auxiliary.PageReadingService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SinglePageIndexingServiceImpl implements SinglePageIndexingService {

    private final SitesList sitesList;
    private final DataBaseConnectionService dataService;

    @Autowired
    public SinglePageIndexingServiceImpl(SitesList sitesList, DataBaseConnectionService dataService) {
        this.sitesList = sitesList;
        this.dataService = dataService;
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
        SiteEntity site = dataService.getSiteRepository().findByUrlEquals(CommonAddrActions.extractSiteHost(pageAddr));

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
            site = dataService.getSiteRepository().saveAndFlush(site);
        }

        page.setSiteId(site.getId());

        removeExistingData(site.getId(), page.getPath());

        page = dataService.getPageRepository().saveAndFlush(page);

        IndexingStatus.setIndexingTrue();
        dataService.getSiteRepository().updateSiteStatus(SiteStatus.INDEXING.toString(), site.getUrl(), "", LocalDateTime.now());
        CollectLemmasService.initPreLemmaTable();
        CollectLemmasService.processLemmasOnPageCollection(page.getContent(), site.getId(), page.getId());
        CollectLemmasService.collectSinglePageLemmas(site.getId(), page.getId());
        IndexingStatus.setIndexingFalse();
        dataService.getSiteRepository().updateSiteStatus(SiteStatus.INDEXED.toString(), site.getUrl(), "", LocalDateTime.now());
        return null;
    }

    private void removeExistingData(int siteId, String pagePath) {

        PageEntity page = dataService.getPageRepository().findPageByPathAndSiteId(pagePath, siteId);
        if (page == null) {
            return;
        }
        dataService.getPageRepository().delete(page);

        List<IndexEntity> pageIndexes = dataService.getIndexRepository().findAllByPageIdEquals(page.getId());
        if (pageIndexes.isEmpty()) {
            return;
        }
        dataService.getIndexRepository().deleteAll(pageIndexes);

        List<Integer> pageLemmasIds = pageIndexes.stream().map(IndexEntity::getLemmaId).toList();

        dataService.getLemmaRepository().decreaseLemmaFrequencyById(pageLemmasIds);

        dataService.getLemmaRepository().deleteEmptyLemmas();
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
