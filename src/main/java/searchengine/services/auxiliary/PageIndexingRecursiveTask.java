package searchengine.services.auxiliary;

import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import searchengine.config.IndexingStatus;
import searchengine.dto.ResultOfPageReading;
import searchengine.model.PageEntity;
import searchengine.model.SiteStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static searchengine.services.auxiliary.CommonAddrActions.addrNormalization;
import static searchengine.services.auxiliary.CommonAddrActions.isGoodAddr;

public class PageIndexingRecursiveTask extends RecursiveAction {

    private final String pagePath;
    private final String siteUrl;
    SingleSiteIndexingProcess parentProcess;

    public PageIndexingRecursiveTask(String path, SingleSiteIndexingProcess parentProcess) {
        this.pagePath = path;
        this.parentProcess = parentProcess;
        this.siteUrl = parentProcess.getSiteRecord().getUrl();
    }

    @Override
    protected void compute() {
        if (!IndexingStatus.isAlreadyIndexing()) return;

        parentProcess.updateSiteStatus(SiteStatus.INDEXING, "");

        ResultOfPageReading readingResult = PageReadingService.readPage(siteUrl.concat(pagePath));

        if (readingResult.getCode() != HttpStatus.OK.value() && pagePath.equals("/")) {
            parentProcess.updateSiteStatus(SiteStatus.FAILED, "The main page of the site is unavailable. Error: " + readingResult.getError());
            parentProcess.getFoundPages().clear();
            return;
        }

        PageEntity page = parentProcess.getPageRepository().saveAndFlush(new PageEntity(pagePath, readingResult.getContent(), readingResult.getCode(),
                parentProcess.getSiteRecord().getId()));

        if (page.getCode() == HttpStatus.OK.value()) {
            CollectLemmasService.extractLemmasFromPage(page, parentProcess.getPreLemmaRepository());
            List<String> subLinks = getSublinksList(page.getContent());
            if (!subLinks.isEmpty()) {
                List<PageIndexingRecursiveTask> tasks = new ArrayList<>();
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

    private List<String> getSublinksList(String pageContent) {
        List<String> linksOnPage = Jsoup.parse(pageContent).getAllElements().eachAttr("href");
        return linksOnPage.stream()
                .filter(s -> isGoodAddr(s, siteUrl)).map(s -> addrNormalization(s, siteUrl)).distinct().collect(Collectors.toList());
    }


    private boolean isLinkNotInBase (String link) {
        boolean result;
        synchronized (parentProcess.foundPages) {
            result = parentProcess.foundPages.add(link);
        }
        return result;
    }

}
