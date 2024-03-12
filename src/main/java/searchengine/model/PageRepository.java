package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.auxdto.SitePagesCounts;

import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM page p WHERE p.site_id = ?1", nativeQuery = true)
    void deleteAllBySiteIdEquals(int siteId);

    @Query(value = "SELECT p.* FROM page p WHERE p.path = ?1 AND p.site_id = ?2", nativeQuery = true)
    PageEntity findPageByPathAndSiteId(String pagePath, int siteId);

    @Query(value = "SELECT COUNT(*) FROM page p WHERE p.site_id = ?1", nativeQuery = true)
    int countBySiteId(Integer id);

    @Query(value = "SELECT p.site_id AS site_id, COUNT(*) AS pages_count FROM pages p GROUP BY p.site_id", nativeQuery = true)
    public List<SitePagesCounts> getPageCountsOnSites ();
}
