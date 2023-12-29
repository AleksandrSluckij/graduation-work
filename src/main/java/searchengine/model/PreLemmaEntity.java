package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pre_lemma")
public class PreLemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String lemma;
    @Column(nullable = false, name = "site_id")
    private int siteId;
    @Column(nullable = false, name = "page_id")
    private int pageId;
    @Column(nullable = false, name = "_count_on_page")
    private int count;

    public PreLemmaEntity (String lemma, int siteId, int pageId, int count) {
        this.lemma = lemma;
        this.siteId = siteId;
        this.pageId = pageId;
        this.count = count;
    }
}
