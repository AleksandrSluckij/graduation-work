package searchengine.services.auxiliary;

import org.jboss.logging.Logger;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import searchengine.config.IndexingStatus;
import searchengine.dto.ResultOfPageReading;
import searchengine.model.PageEntity;
import searchengine.model.SiteStatus;
import searchengine.services.DatabaseConnService;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

import static searchengine.services.auxiliary.CommonAddrActions.addrNormalization;
import static searchengine.services.auxiliary.CommonAddrActions.isGoodAddr;

public class PageIndexingRecursiveTask extends RecursiveAction {

    private final String pagePath;
    private final DatabaseConnService dataService;
    SingleSiteIndexingProcess parentProcess;
    private final String EMPTY_MESSAGE = "";

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
        parentProcess.updateSiteStatus(SiteStatus.INDEXING, EMPTY_MESSAGE);
        ResultOfPageReading readingResult = readPage(siteUrl, pagePath);

        if (readingResult.getCode() != HttpStatus.OK.value() && pagePath.equals("/")) {
            parentProcess.updateSiteStatus(SiteStatus.FAILED, "The main page of the site is unavailable. Error: " + readingResult.getError());
            return;
        }

        PageEntity page = new PageEntity();
        page.setPath(pagePath);
        page.setContent(readingResult.getContent());
        page.setCode(readingResult.getCode());
        page.setSiteId(parentProcess.getSiteId());
        dataService.getPageRepository().saveAndFlush(page);

        if (page.getCode() == HttpStatus.OK.value()) {
            Document dom = Jsoup.parse(page.getContent());
            subLinks = dom.getAllElements().eachAttr("href");
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
        parentProcess.updateSiteStatus(SiteStatus.INDEXED, EMPTY_MESSAGE);
    }

    private ResultOfPageReading readPage(String siteUrl, String pagePath) {
        Connection.Response response;
        int responseCode;
        String pageContent;
        String errorMessage;
        String EMPTY_CONTENT = "N/A";

        String fullAddr = siteUrl.concat(pagePath);
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(this.getClass().getSimpleName()).info("Поток завершился нештатно при ожидании ответа от страницы: " + fullAddr);
        }

        try {
            Connection connection = Jsoup.connect(fullAddr).maxBodySize(0).userAgent(dataService.getUserAgent()).referrer(dataService.getReferer());
            response = connection.method(Connection.Method.GET).execute();
            responseCode = response.statusCode();
            pageContent = response.body();
            errorMessage = EMPTY_MESSAGE;
        } catch (HttpStatusException ex) {
            responseCode = ex.getStatusCode();
            pageContent = EMPTY_CONTENT;
            Logger.getLogger(this.getClass().getSimpleName()).info("Не удалось получить содержимое страницы " + fullAddr + ", код ошибки: " + responseCode);
            errorMessage = String.format("Не удалось получить содержимое страницы %s, код ошибки %s}", fullAddr, responseCode);
        } catch (UnknownHostException ex) {
            responseCode = HttpStatus.NOT_FOUND.value();
            pageContent = EMPTY_CONTENT;
            Logger.getLogger(this.getClass().getSimpleName()).info("IP-адрес хоста не может быть определен при получении содержимого страницы: " + fullAddr);
            errorMessage = String.format("IP-адрес хоста не может быть определен при получении содержимого страницы %s", fullAddr);
        } catch (IOException ex) {
            errorMessage = ex.getMessage();
            int i = errorMessage.indexOf("Status=");
            pageContent = EMPTY_CONTENT;
            if (i > 0) {
                responseCode = Integer.parseInt(errorMessage.substring(i + 7, i + 10));
            } else {
                responseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
                Logger.getLogger(this.getClass().getSimpleName()).info("I/O Exception occurred!!!");
            }
        }
       return new ResultOfPageReading(pageContent, responseCode, errorMessage);
    }

    private boolean isLinkNotInBase (String link) {
        boolean result;
        synchronized (parentProcess.foundPages) {
            result = parentProcess.foundPages.add(link);
        }
        return result;
    }

}
