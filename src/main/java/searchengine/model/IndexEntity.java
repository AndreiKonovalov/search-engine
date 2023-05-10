package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity(name = "`index`")
@NoArgsConstructor
@Getter
@Setter
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "page_id")
    private PageEntity page;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "lemma_id")
    private LemmaEntity lemma;

    @Column(name = "`rank`", columnDefinition = "FLOAT NOT NULL")
    private Float rank;
}
