package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM page p WHERE p.site_id = ?1", nativeQuery = true)
    void deleteAllBySiteIdEquals(int siteId);
}