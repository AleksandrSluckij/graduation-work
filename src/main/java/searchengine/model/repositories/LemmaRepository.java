package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;

import java.util.List;
import java.util.Set;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    @Query(value = "SELECT l.* FROM lemma l WHERE (l.lemma IN ?1) AND (l.site_id = ?2)", nativeQuery = true)
    List<LemmaEntity> findLemmas(Set<String> lemmasOnPage, int siteId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM lemma l WHERE l.site_id = ?1", nativeQuery = true)
    void deleteAllBySiteId(Integer siteId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM lemma l WHERE l.frequency = 0", nativeQuery = true)
    void deleteEmptyLemmas();

    @Query(value = "SELECT COUNT(*) FROM lemma l WHERE l.site_id = ?1", nativeQuery = true)
    int countBySiteId(Integer id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE lemma l SET l.frequency = l.frequency - 1 WHERE l.id IN ?1", nativeQuery = true)
    void decreaseLemmaFrequencyById(List<Integer> pageLemmasIds);

    @Query(value = "SELECT l.lemma FROM lemma l WHERE l.site_id = ?1 AND l.lemma IN ?2", nativeQuery = true)
    List<String> findNamesBySiteIdAndNamesIn(int siteId, List<String> lemmaNamesFound);

    @Transactional
    @Modifying
    @Query(value = "UPDATE lemma l SET l.frequency = l.frequency + 1 WHERE l.site_id = ?1 AND l.lemma IN ?2", nativeQuery = true)
    void increaseLemmaFrequencyByName(int siteId, List<String> lemmaNamesInBase);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) SELECT pl.site_id AS site_id, pl.lemma AS lemma, COUNT(*) AS frequency " +
            "FROM pre_lemma pl WHERE pl.site_id = ?1 GROUP BY pl.lemma", nativeQuery = true)
    void collectingSingleSiteLemmas(int siteId);

    @Query(value = "SELECT * FROM lemma l WHERE l.lemma IN ?1", nativeQuery = true)
    List<LemmaEntity> findLemmasByNames(Set<String> queryLemmas);

    @Query(value = "SELECT ((SELECT l.frequency * 100 FROM lemma l WHERE l.id = ?1)/(SELECT COUNT(*) FROM p WHERE p.site_id = ?2 AND p.response_code = 200))",
            nativeQuery = true)
    int getLemmaRating(int lemmaId, int siteId);

    @Query(value = "SELECT * FROM lemma l WHERE l.lemma IN ?1 AND l.site_id = ?2", nativeQuery = true)
    List<LemmaEntity> findLemmasByNamesAndSiteId(Set<String> queryLemmas, int siteId);
}
