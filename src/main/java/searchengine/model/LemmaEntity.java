package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "lemma")
@NoArgsConstructor
public class LemmaEntity implements Comparable<LemmaEntity>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "site_id", nullable = false)
    private Integer siteId;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private Integer frequency;

    public LemmaEntity(int frequency, String lemma, int siteId) {
        this.frequency = frequency;
        this.lemma = lemma;
        this.siteId = siteId;
    }


    @Override
    public int compareTo(LemmaEntity l) {
        return (this.frequency - l.frequency);
    }
}
