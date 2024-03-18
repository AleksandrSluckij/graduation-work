package searchengine.services.search;

import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResultDto;
import searchengine.dto.search.SingleSearchResultDto;
import searchengine.model.LemmaEntity;
import searchengine.model.PreResultEntity;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.model.repositories.*;
import searchengine.services.morphology.MorphService;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService{

    private final PreResultRepository preResultRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    private Set<String> queryLemmas;
    private String query;
    private List<Integer> siteIds;
    private double maxRelevanceFound = 0.0;
    private int resultsCount = 0;


    @Autowired
    public SearchServiceImpl(PreResultRepository preResultRepository, LemmaRepository lemmaRepository, PageRepository pageRepository,
                             IndexRepository indexRepository, SiteRepository siteRepository) {
        this.preResultRepository = preResultRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
    }


    @Override
    public SearchResultDto processQuery(Map<String, String> parameters) {
        String error = null;

        int offset = Integer.parseInt(parameters.getOrDefault("offset", "0"));
        int limit = Integer.parseInt(parameters.getOrDefault("limit", "10"));

        error = decodeQueryToLemmas(parameters.getOrDefault("query", null));
        if (error != null) {
            return new SearchResultDto(error);
        }                                           // Set queryLemmas ready


        if (offset == 0) {
            searchPreResultTableInit();
            error = createSearchPreResultTable(parameters);
        }

        maxRelevanceFound = (double) preResultRepository.findMaxRelevance().orElse(0);
        resultsCount = preResultRepository.getCount();

        return error == null ? getPartSearchResults(offset, limit) : new SearchResultDto(error);
    }

    private SearchResultDto getPartSearchResults(int offset, int limit) {
        if (maxRelevanceFound < 0.001 || resultsCount == 0) {
            return new SearchResultDto(0, new ArrayList<>());
        }

        List<PreResultEntity> resultPage = preResultRepository.getResultPage(offset, limit);
        if (!resultPage.isEmpty()) {
            return new SearchResultDto(resultsCount, createSearchResultData(resultPage));
        } else {
            return new SearchResultDto(0, new ArrayList<>());
        }
    }

    private List<SingleSearchResultDto> createSearchResultData(List<PreResultEntity> resultPage) {
        List<SingleSearchResultDto> resultDtoList = new ArrayList<>();
        for (PreResultEntity preResult : resultPage) {
            SingleSearchResultDto singleSearchResultDto = new SingleSearchResultDto();
            singleSearchResultDto.setSite(preResult.getSite());
            singleSearchResultDto.setSiteName(preResult.getSiteName());
            singleSearchResultDto.setUri(preResult.getUri());
            Document pageContent = Jsoup.parse(preResult.getContent());
            singleSearchResultDto.setTitle(pageContent.title());
            singleSearchResultDto.setSnippet(createSnippet(pageContent.text()));
            singleSearchResultDto.setRelevance(preResult.getRelevance() / maxRelevanceFound);
            resultDtoList.add(singleSearchResultDto);
        }
        return resultDtoList;
    }

    private String createSnippet(String text) {
        String result = SnippetCreator.getExactlySnippet(text, query);
        if (result == null) {
            result = SnippetCreator.getApproxSnippet(text, query);
        }
        return result;
    }


    private String createSearchPreResultTable (Map<String, String> parameters) {

        String error = decodeSiteUrlToSiteIds(parameters.getOrDefault("site", null));
        if (error != null) {
            return error;
        }                                           // siteIds List ready


        if (siteIds.size() == 1) {
            error = fillPreResultTableBySiteId(siteIds.get(0));
            return error;
        } else {
            error = "Неконкретный запрос. Задайте более строгие условия.";
            for (Integer siteId : siteIds) {
                String singleError = fillPreResultTableBySiteId(siteId);
                error = singleError == null ? null : error;
            }
        }
        return error;
    }

    private String fillPreResultTableBySiteId (Integer siteId) {
        List<LemmaEntity> lemmasToProcess = lemmaRepository.findLemmasByNamesAndSiteId(queryLemmas, siteId);
        if (lemmasToProcess == null || lemmasToProcess.size() < queryLemmas.size()) {
            return null;
        }
        int pageCountOnSite = pageRepository.countBySiteId(siteId);
        lemmasToProcess = oneSiteRemoveFrequentlyLemmas(lemmasToProcess, pageCountOnSite);
        if (lemmasToProcess.isEmpty()) {
            return "Неконкретный запрос. Задайте более строгие условия.";
        }
        Collections.sort(lemmasToProcess);
        List<Integer> pagesIdsFound = new ArrayList<>();
        pagesIdsFound = indexRepository.findPagesIdsByLemmaIdAndSiteId(lemmasToProcess.get(0).getId(), siteId);
        if (lemmasToProcess.size() > 1) {
            for (int i = 1; i < lemmasToProcess.size(); i++) {
                List<Integer> pagesIdsFoundNext = indexRepository.findPagesIdsByLemmaIdAndSiteId(lemmasToProcess.get(i).getId(), siteId);
                pagesIdsFound.retainAll(pagesIdsFoundNext);
            }
        }
        if (pagesIdsFound.isEmpty()) {
            return null;
        }

        List<Integer> lemmasIdsToProcess = lemmasToProcess.stream().map(LemmaEntity::getId).toList();
        indexRepository.fillPreResultTable(lemmasIdsToProcess, pagesIdsFound);

        return null;
    }

    private void searchPreResultTableInit() {
        preResultRepository.initPreResultTable();
    }

    private List<LemmaEntity> oneSiteRemoveFrequentlyLemmas (List<LemmaEntity> lemmasToProcess, int pagesCountOnSite) {
        List<LemmaEntity> lemmasToRemove = new ArrayList<>();
        for (LemmaEntity lemma : lemmasToProcess) {
            if ((lemma.getFrequency() * 100 / pagesCountOnSite) > 60) {
                lemmasToRemove.add(lemma);
            }
        }
        lemmasToProcess.removeAll(lemmasToRemove);
        return lemmasToProcess;
    }


    private String decodeSiteUrlToSiteIds(String siteUrl) {
        siteIds = new ArrayList<>();
        if (siteUrl == null) {
            siteIds = siteRepository.findSiteIdsIndexed();
            if (siteIds.isEmpty()) {
                return "Нет проиндексированных сайтов";
            }
        } else {
            SiteEntity site = siteRepository.findByUrl(siteUrl);
            if (site == null) {
                return "Указанный сайт отсутствует в базе.";
            }
            if (!site.getStatus().equals(SiteStatus.INDEXED)) {
                return "Указанный сайт не проиндексирован.";
            } else {
                siteIds.add(site.getId());
            }
        }

        return null;
    }

    private String decodeQueryToLemmas(String inputQuery) {
        if (Strings.isEmpty(inputQuery)) {
            return "Не задан поисковый запрос.";
        }

        query = inputQuery;
        queryLemmas = new HashSet<>();
        MorphService morphService = MorphService.getMorphService();
        if (morphService != null) {
            queryLemmas = morphService.getLemmas(inputQuery).keySet();
        } else {
            return "Невозможно создать LemmaFinder.";
        }
        if (queryLemmas.isEmpty()) {
            return "Поисковый запрос не содержит лемм.";
        }

        return null;
    }

}
