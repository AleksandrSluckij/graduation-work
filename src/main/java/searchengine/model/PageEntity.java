package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "page")
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "site_id", nullable = false)
    private Integer siteId;
    @Column(columnDefinition = "TEXT NOT NULL, KEY idx_path (path(150))")
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    public PageEntity(String path, String content, int code, Integer siteId) {
        this.siteId = siteId;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
