package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    List<IndexEntity> findAllByPageIdEquals(Integer pageId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM _index i WHERE i.page_id IN (SELECT p.id FROM page p WHERE p.site_id = ?1)", nativeQuery = true)
    void deleteAllBySiteId(Integer siteId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO _index (page_id, lemma_id, _rank) SELECT pl.page_id AS page_id, l.id AS lemma_id, pl._count_on_page AS _rank " +
            "FROM pre_lemma pl JOIN lemma l ON pl.lemma = l.lemma AND pl.site_id = l.site_id", nativeQuery = true)
    void collectSinglePageIndexes();

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO _index (page_id, lemma_id, _rank) SELECT pl.page_id AS page_id, l.id AS lemma_id, pl._count_on_page AS _rank " +
            "FROM pre_lemma pl JOIN lemma l ON pl.lemma = l.lemma AND pl.site_id = l.site_id WHERE pl.site_id = ?1", nativeQuery = true)
    void collectSingleSiteIndexes(int siteId);

    @Query(value = "SELECT i.page_id FROM _index i JOIN page p ON p.id = i.page_id WHERE i.lemma_id = ?1 AND p.site_id = ?2", nativeQuery = true)
    List<Integer> findPagesIdsByLemmaIdAndSiteId(int lemmaId, int siteId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO pre_result (site_url, site_name, page_path, page_content, relevance) " +
            "SELECT s.url, s.name, p.path, p.content, SUM(i._rank) FROM _index i JOIN page p ON p.id = i.page_id JOIN site s ON s.id = p.site_id " +
                "WHERE i.page_id IN ?2 AND i.lemma_id IN ?1 GROUP BY i.page_id", nativeQuery = true)
    void fillPreResultTable(List<Integer> lemmasIdsToProcess, List<Integer> pagesIdsFound);

}
