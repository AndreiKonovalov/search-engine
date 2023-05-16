package searchengine.siteparser;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.JsoupConfig;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusEnum;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static java.lang.Thread.sleep;

public class ForkJoinSiteParser extends RecursiveAction {

    static {
        sitesSet = new CopyOnWriteArraySet<>();
    }

    private static final Set<String> sitesSet;
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static JsoupConfig jsoupConfig;
    private String rootUrlOfSite = "";
    @Getter
    private final SiteEntity siteEntity;
    private List<ForkJoinSiteParser> allTasks;
    private String path;

    public ForkJoinSiteParser(String path, SiteEntity siteEntity,
                              JsoupConfig jsoupConfig, SiteRepository siteRepository,
                              PageRepository pageRepository) {
        ForkJoinSiteParser.pageRepository = pageRepository;
        ForkJoinSiteParser.siteRepository = siteRepository;
        ForkJoinSiteParser.jsoupConfig = jsoupConfig;
        allTasks = new ArrayList<>();
        this.path = path;
        sitesSet.add(path);
        sitesSet.add(path + "/");
        if (this.rootUrlOfSite.equals("")) {
            if (!path.endsWith("/")) {
                path += "/";
            }
            this.rootUrlOfSite = path;
        }
        this.siteEntity = siteEntity;
    }

    public ForkJoinSiteParser(String path, SiteEntity siteEntity, String rootUrlOfSite) {
        allTasks = new ArrayList<>();
        this.path = path;
        sitesSet.add(path);
        if (this.rootUrlOfSite.equals("")) {
            this.rootUrlOfSite = rootUrlOfSite;
        }
        this.siteEntity = siteEntity;
        try {
            sleep(150);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void compute() {

        try {
            if (!path.endsWith("/")) {
                path += "/";
            }
            sleep(150);
            Connection.Response response = Jsoup.connect(path)
                    .ignoreHttpErrors(true)
                    .userAgent(jsoupConfig.getUseragent())
                    .referrer(jsoupConfig.getReferrer())
                    .execute();
            Document doc = response.parse();
            savePage(response, doc);
            updateSiteStatusTime(rootUrlOfSite);
            Elements elements = doc.getElementsByTag("a");
            elements.stream()
                    .map(el -> el.absUrl("href"))
                    .filter(u -> !u.isEmpty())
                    .filter(p -> !p.equals(path))
                    .filter(s -> s.startsWith(rootUrlOfSite))
                    .filter(c -> !c.contains("#"))
                    .filter(c -> !c.contains("?method="))
                    .filter(r -> !sitesSet.contains(r))
                    .filter(m -> !m.matches("([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf|zip|tar|jar|svg|php))$)"))
                    .forEach(this::addChild);
        } catch (IOException | InterruptedException | NullPointerException ex) {
            siteEntity.setStatus(StatusEnum.FAILED);
            siteEntity.setLastError(ex.getMessage());
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
        }
        allTasks.forEach(ForkJoinTask::join);
    }

    private void addChild(String element) {
        sitesSet.add(element);
        ForkJoinSiteParser child = new ForkJoinSiteParser(element, siteEntity, rootUrlOfSite);
        child.fork();
        allTasks.add(child);
    }

    private void savePage(Connection.Response response, Document doc) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSite(siteEntity);
        pageEntity.setPath(checkPath(path));
        pageEntity.setCode(response.statusCode());
        pageEntity.setContent(doc.html());
        pageRepository.save(pageEntity);
    }

    private String checkPath(String path) {
        return path.equals(rootUrlOfSite) ? "/" : path.substring(rootUrlOfSite.length() - 1);
    }

    private void updateSiteStatusTime(String rootUrlOfSite) {
        String url;
        SiteEntity site = siteRepository.findByUrl(rootUrlOfSite);
        if (site == null) {
            url = rootUrlOfSite.substring(0, rootUrlOfSite.length() - 1);
            site = siteRepository.findByUrl(url);
        }
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
}
