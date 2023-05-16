package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "lemma")
@NoArgsConstructor
@Getter
@Setter
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @Column(name = "lemma", columnDefinition = "VARCHAR(255) NOT NULL")
    private String lemma;

    @Column(name = "frequency", columnDefinition = "INT NOT NULL")
    private Integer frequency;
}
