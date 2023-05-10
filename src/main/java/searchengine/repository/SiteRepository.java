package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

@Transactional
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    void deleteAllByUrl(String url);

    SiteEntity findByUrl(String path);
}
