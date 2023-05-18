package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteEntity;
import searchengine.model.StatusEnum;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.siteparser.ForkJoinSiteParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingServiceImpl implements IndexingService {
    @Autowired
    private SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private JsoupConfig jsoupConfig;
    private List<Thread> threadsList;
    private List<ForkJoinPool> joinPools;


    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse indexingResponse = new IndexingResponse();
        if (isIndexed()) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация уже запущена");
        }else {
            new Thread(this::indexing).start();
            indexingResponse.setResult(true);
            indexingResponse.setError(null);
        }
        return indexingResponse;
    }

    public IndexingResponse stopIndexing(){
        IndexingResponse indexingResponse = new IndexingResponse();
        return indexingResponse;
    }

    @Override
    public IndexingResponse indexingPage() {
        return null;
    }

    private boolean isIndexed() {
        AtomicBoolean isIndexed = new AtomicBoolean(false);
        siteRepository.findAll().forEach(siteEntity -> {
            if (siteEntity.getStatus().equals(StatusEnum.INDEXING)) {
                isIndexed.set(true);
            }
        });
        return isIndexed.get();
    }

    private void indexing() {
        List<ForkJoinSiteParser> siteParsers = new ArrayList<>();
        threadsList = new ArrayList<>();
        joinPools = new ArrayList<>();
        for (Site site : sites.getSites()) {
            deleteAllInfoForSite(site.getUrl());
            saveSiteInTable(site);
            SiteEntity siteEntity = siteRepository.findByUrl(site.getUrl());
            siteParsers.add(new ForkJoinSiteParser(site.getUrl(), siteEntity, jsoupConfig,
                    siteRepository, pageRepository));
        }

        siteParsers.forEach(parser -> threadsList.add(new Thread(() -> {
            SiteEntity siteEntity = parser.getSiteEntity();
            try {
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                joinPools.add(forkJoinPool);
                forkJoinPool.invoke(parser);
                siteEntity.setStatus(StatusEnum.INDEXED);
                siteRepository.save(siteEntity);

            } catch (IllegalArgumentException | NullPointerException | RejectedExecutionException e) {
                siteEntity.setLastError(e.getMessage());
                siteEntity.setStatus(StatusEnum.FAILED);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteRepository.save(siteEntity);
            }
        })));
        threadsList.forEach(Thread::start);
    }

    private void saveSiteInTable(Site site) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatus(StatusEnum.INDEXING);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }

    private void deleteAllInfoForSite(String url) {
        siteRepository.deleteAllByUrl(url);
    }
}
