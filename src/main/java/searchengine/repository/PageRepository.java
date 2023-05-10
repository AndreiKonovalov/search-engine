package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
@Transactional
public interface PageRepository extends JpaRepository<PageEntity, Integer> {


}
