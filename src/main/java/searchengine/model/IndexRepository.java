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
}
