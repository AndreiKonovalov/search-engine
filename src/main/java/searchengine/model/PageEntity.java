package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "site_id, path")})
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @Column(name = "path", columnDefinition = "VARCHAR(512) NOT NULL")
    private String path;

    @Column(name = "code", columnDefinition = "INT NOT NULL")
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;
}
