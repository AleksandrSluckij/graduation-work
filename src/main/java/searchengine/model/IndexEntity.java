package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "_index")
@NoArgsConstructor
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "page_id", nullable = false)
    private Integer pageId;
    @Column(name = "lemma_id", nullable = false)
    private Integer lemmaId;
    @Column(name = "_rank", nullable = false)
    private Integer rank;

    public IndexEntity(Integer lemmaId, int pageId, Integer rank) {
        this.lemmaId = lemmaId;
        this.pageId = pageId;
        this.rank = rank;
    }
}
