package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PreLemmaRepository extends JpaRepository<PreLemmaEntity, Integer> {
    @Query(value = "TRUNCATE TABLE pre_lemma", nativeQuery = true)
    @Modifying
    @Transactional
    void initPreLemmaTable ();

    @Query(value = "SELECT pl.lemma FROM pre_lemma pl WHERE pl.page_id = ?1", nativeQuery = true)
    List<String> findAllNames(int pageId);
}
