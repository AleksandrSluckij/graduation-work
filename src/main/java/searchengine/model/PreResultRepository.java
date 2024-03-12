package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PreResultRepository extends JpaRepository<PreResultEntity, Integer> {

    @Query(value = "TRUNCATE TABLE pre_result", nativeQuery = true)
    @Modifying
    @Transactional
    void initPreResultTable ();

    @Query(value = "SELECT MAX(relevance) FROM pre_result", nativeQuery = true)
    Optional<Integer> findMaxRelevance();

    @Query(value = "SELECT COUNT(*) FROM pre_result", nativeQuery = true)
    int getCount();

    @Query(value = "SELECT pr.* FROM pre_result pr ORDER BY pr.relevance DESC LIMIT ?2 OFFSET ?1", nativeQuery = true)
    List<PreResultEntity> getResultPage(int offset, int limit);

}
