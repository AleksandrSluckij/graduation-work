package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    SiteEntity findByUrlEquals(String url);

    @Transactional
    @Modifying
    @Query(value = "UPDATE site SET status = ?1, last_error = ?3, status_time = ?4 WHERE url = ?2", nativeQuery = true)
    void updateSiteStatus (String status, String url, String error, LocalDateTime statusTime);

    @Query(value = "SELECT s.id FROM site s WHERE s.url = ?1", nativeQuery = true)
    Integer findSiteIdByUrl(String url);

    SiteEntity findByUrl (String url);

    @Query(value = "SELECT s.id FROM site s WHERE s.status = 'INDEXED'", nativeQuery = true)
    List<Integer> findSiteIdsIndexed();
}
