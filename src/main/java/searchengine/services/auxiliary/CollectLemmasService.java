package searchengine.services.auxiliary;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.LemmaEntity;
import searchengine.model.PreLemmaEntity;
import searchengine.services.DataBaseConnectionService;
import searchengine.services.morphology.MorphService;

import java.util.*;

import static searchengine.config.LoggingConfig.LOGGER;
import static searchengine.config.LoggingConfig.MARKER;

@Component
public class CollectLemmasService {
    private static DataBaseConnectionService dataService;

    @Autowired
    public CollectLemmasService(DataBaseConnectionService dataBaseConnectionService) {
        dataService = dataBaseConnectionService;
    }

    public static void processLemmasOnPageCollection(String pageContent, int siteId, int pageId) {
        String sourceText = Jsoup.parse(pageContent).text();
        Map<String, Integer> lemmasMap = new HashMap<>();
        MorphService morphService = MorphService.getMorphService();
        if (morphService != null) {
            lemmasMap = morphService.getLemmas(sourceText);
        }
        if (!lemmasMap.isEmpty()) {
            List<PreLemmaEntity> foundLemmas = new ArrayList<>();
            for (String lemmaName : lemmasMap.keySet()) {
                foundLemmas.add(new PreLemmaEntity(lemmaName, siteId, pageId, lemmasMap.get(lemmaName)));
            }
            dataService.getPreLemmaRepository().saveAllAndFlush(foundLemmas);
        }
    }

    public static void initPreLemmaTable () {
        dataService.getPreLemmaRepository().initPreLemmaTable();
    }

    public static void collectSinglePageLemmas(int siteId, int pageId) {
        List<String> lemmaNamesFound = dataService.getPreLemmaRepository().findAllNames(pageId);
        if (lemmaNamesFound == null) {
            String fullAddr = dataService.getSiteRepository().findById(siteId).get().getUrl().concat(
                                dataService.getPageRepository().findById(pageId).get().getPath());
            LOGGER.error(MARKER, "При индексации страницы " + fullAddr + " не найдено ни одной леммы!!!");
            return;
        }
        List<String> lemmaNamesNew = new ArrayList<>(List.copyOf(lemmaNamesFound));
        List<String> lemmaNamesInBase = dataService.getLemmaRepository().findNamesBySiteIdAndNamesIn(siteId, lemmaNamesFound);
        if (!lemmaNamesInBase.isEmpty()) {
            lemmaNamesNew.removeAll(lemmaNamesInBase);
            dataService.getLemmaRepository().increaseLemmaFrequencyByName(siteId, lemmaNamesInBase);
        }
        if (!lemmaNamesNew.isEmpty()) {
            List<LemmaEntity> newLemmas = new ArrayList<>();
            for (String name : lemmaNamesNew) {
                newLemmas.add(new LemmaEntity(1, name, siteId));
            }
            dataService.getLemmaRepository().saveAllAndFlush(newLemmas);
        }

        dataService.getIndexRepository().collectSinglePageIndexes();
    }

    public static void collectSingleSiteLemmas (int siteId) {
        dataService.getLemmaRepository().collectingSingleSiteLemmas(siteId);
        dataService.getIndexRepository().collectSingleSiteIndexes(siteId);
    }

//            LOGGER.error(MARKER, "Lemmas counts not match!! Page: " + dataService.getSiteRepository().findById(siteId).get().getUrl()
//                    + dataService.getPageRepository().findById(pageId).get().getPath());
//            LOGGER.error(MARKER, "Page id: " + pageId);
//            LOGGER.error(MARKER, "Lemmas on page count: " + lemmasMap.size());
//            LOGGER.error(MARKER, ">>> " + String.join(", ", lemmasMap.keySet()));
//            LOGGER.error(MARKER, " Lemmas in base count: " + lemmasInBaseNew.size());
//            LOGGER.error(MARKER, "--- " + String.join(", ", lemmasInBaseNew.stream().map(LemmaEntity :: getLemma).toList()));
//            LOGGER.error(MARKER, "Initial count of lemmas in base: " + lemmasInBaseInitial.size());
//            LOGGER.error(MARKER, "=== " + String.join(", ", lemmasInBaseInitial.stream().map(LemmaEntity :: getLemma).toList()));
//            LOGGER.error(MARKER, "The page is skipping!!!!");
//        }
//
//        List<IndexEntity> indexes = new ArrayList<>();
//        for (LemmaEntity lemmaInBase : lemmasInBaseNew) {
//            indexes.add(new IndexEntity(lemmaInBase.getId(), pageId, lemmasMap.get(lemmaInBase.getLemma())));
//        }
//        dataService.getIndexRepository().saveAllAndFlush(indexes);
//    }
}
