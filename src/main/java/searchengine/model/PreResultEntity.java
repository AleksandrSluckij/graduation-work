package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "pre_result")
public class PreResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(name = "site_url", columnDefinition = "VARCHAR(255) NOT NULL")
    private String site;
    @Column(name = "site_name", columnDefinition = "VARCHAR(255) NOT NULL")
    private String siteName;
    @Column(name = "page_path", columnDefinition = "VARCHAR(255) NOT NULL")
    private String uri;
    @Column(name = "page_content", columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;
    @Column(name = "relevance", columnDefinition = "DOUBLE")
    private double relevance;

}
