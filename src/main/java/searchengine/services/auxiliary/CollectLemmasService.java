package searchengine.services.auxiliary;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.PreLemmaEntity;
import searchengine.model.repositories.*;
import searchengine.services.morphology.MorphService;

import java.util.*;

@Component
@Slf4j
public class CollectLemmasService {

    public static void extractLemmasFromPage(PageEntity page, PreLemmaRepository preLemmaRepository) {
        String sourceText = Jsoup.parse(page.getContent()).text();
        Map<String, Integer> lemmasMap = new HashMap<>();
        MorphService morphService = MorphService.getMorphService();
        if (morphService != null) {
            lemmasMap = morphService.getLemmas(sourceText);
        }
        if (!lemmasMap.isEmpty()) {
            List<PreLemmaEntity> foundLemmas = new ArrayList<>();
            for (String lemmaName : lemmasMap.keySet()) {
                foundLemmas.add(new PreLemmaEntity(lemmaName, page.getSiteId(), page.getId(), lemmasMap.get(lemmaName)));
            }
            preLemmaRepository.saveAllAndFlush(foundLemmas);
        }
    }

    public static void collectSinglePageLemmas(int siteId, int pageId, PreLemmaRepository preLemmaRepository, SiteRepository siteRepository,
                                               PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        List<String> lemmaNamesFound = preLemmaRepository.findAllNames(pageId);
        if (lemmaNamesFound == null) {
            String fullAddr = siteRepository.findById(siteId).get().getUrl().concat(
                                pageRepository.findById(pageId).get().getPath());
            log.error("При индексации страницы " + fullAddr + " не найдено ни одной леммы!!!");
            return;
        }
        List<String> lemmaNamesNew = new ArrayList<>(List.copyOf(lemmaNamesFound));
        List<String> lemmaNamesInBase = lemmaRepository.findNamesBySiteIdAndNamesIn(siteId, lemmaNamesFound);
        if (!lemmaNamesInBase.isEmpty()) {
            lemmaNamesNew.removeAll(lemmaNamesInBase);
            lemmaRepository.increaseLemmaFrequencyByName(siteId, lemmaNamesInBase);
        }
        if (!lemmaNamesNew.isEmpty()) {
            List<LemmaEntity> newLemmas = new ArrayList<>();
            for (String name : lemmaNamesNew) {
                newLemmas.add(new LemmaEntity(1, name, siteId));
            }
            lemmaRepository.saveAllAndFlush(newLemmas);
        }

        indexRepository.collectSinglePageIndexes();
    }

    public static void collectSingleSiteLemmas (int siteId, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        lemmaRepository.collectingSingleSiteLemmas(siteId);
        indexRepository.collectSingleSiteIndexes(siteId);
    }

}
