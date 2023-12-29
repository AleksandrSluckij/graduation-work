package searchengine.services.auxiliary;

import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import searchengine.config.IndexingStatus;
import searchengine.dto.ResultOfPageReading;
import searchengine.model.PageEntity;
import searchengine.model.SiteStatus;
import searchengine.services.DataBaseConnectionService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static searchengine.services.auxiliary.CommonAddrActions.addrNormalization;
import static searchengine.services.auxiliary.CommonAddrActions.isGoodAddr;

public class PageIndexingRecursiveTask extends RecursiveAction {

    private final String pagePath;
    private final DataBaseConnectionService dataService;
    SingleSiteIndexingProcess parentProcess;

    public PageIndexingRecursiveTask(String path, SingleSiteIndexingProcess parentProcess) {
        this.pagePath = path;
        this.dataService = parentProcess.getDataService();
        this.parentProcess = parentProcess;
    }

    @Override
    protected void compute() {
        if (!IndexingStatus.isAlreadyIndexing()) return;
        List<String> subLinks = new ArrayList<>();
        List<PageIndexingRecursiveTask> tasks = new ArrayList<>();

        String siteUrl = parentProcess.getSiteRecord().getUrl();
        parentProcess.updateSiteStatus(SiteStatus.INDEXING, "");
        ResultOfPageReading readingResult = PageReadingService.readPage(siteUrl.concat(pagePath));

        if (readingResult.getCode() != HttpStatus.OK.value() && pagePath.equals("/")) {
            parentProcess.updateSiteStatus(SiteStatus.FAILED, "The main page of the site is unavailable. Error: " + readingResult.getError());
            parentProcess.getFoundPages().clear();
            return;
        }

        PageEntity page = new PageEntity();
        page.setPath(pagePath);
        page.setContent(readingResult.getContent());
        page.setCode(readingResult.getCode());
        page.setSiteId(parentProcess.getSiteId());
        page = dataService.getPageRepository().saveAndFlush(page);

        if (page.getCode() == HttpStatus.OK.value()) {
            CollectLemmasService.processLemmasOnPageCollection(page.getContent(), parentProcess.getSiteId(), page.getId());
            subLinks = Jsoup.parse(page.getContent()).getAllElements().eachAttr("href");
        }
        if (!subLinks.isEmpty()) {
            subLinks = subLinks.stream().filter(s -> isGoodAddr(s, siteUrl)).map(s -> addrNormalization(s, siteUrl)).distinct().collect(Collectors.toList());
            if (!subLinks.isEmpty()) {
                for (String link : subLinks) {
                    if (isLinkNotInBase(link)) {
                        PageIndexingRecursiveTask task = new PageIndexingRecursiveTask(link, parentProcess);
                        task.fork();
                        tasks.add(task);
                    }
                }
                for (PageIndexingRecursiveTask task : tasks) {
                    task.join();
                }
            }
        }
        parentProcess.updateSiteStatus(SiteStatus.INDEXED, "");

    }


    private boolean isLinkNotInBase (String link) {
        boolean result;
        synchronized (parentProcess.foundPages) {
            result = parentProcess.foundPages.add(link);
        }
        return result;
    }

}
