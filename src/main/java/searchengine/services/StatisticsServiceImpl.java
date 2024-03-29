package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingStatus;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponseBody;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.repositories.LemmaRepository;
import searchengine.model.repositories.PageRepository;
import searchengine.model.SiteEntity;
import searchengine.model.repositories.SiteRepository;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Autowired
    public StatisticsServiceImpl(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public StatisticsResponseBody getStatistics() {
        StatisticsResponseBody response = new StatisticsResponseBody();
        response.setResult(true);
        TotalStatistics total = new TotalStatistics();
        total.setIndexing(IndexingStatus.isAlreadyIndexing());
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        List<DetailedStatisticsItem> detailedList = new ArrayList<>();
        List<SiteEntity> sitesList = siteRepository.findAll();
        for (SiteEntity site : sitesList) {
            DetailedStatisticsItem detailed = new DetailedStatisticsItem();
            detailed.setUrl(site.getUrl());
            detailed.setName(site.getName());
            detailed.setStatus(site.getStatus().toString());
            detailed.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.of("+03:00")) * 1000);
            detailed.setError(site.getLastError());
            detailed.setPages(pageRepository.countBySiteId(site.getId()));
            detailed.setLemmas(lemmaRepository.countBySiteId(site.getId()));
            detailedList.add(detailed);
        }
        StatisticsData statistics = new StatisticsData();
        statistics.setTotal(total);
        statistics.setDetailed(detailedList);
        response.setStatistics(statistics);
        return response;
    }
}
