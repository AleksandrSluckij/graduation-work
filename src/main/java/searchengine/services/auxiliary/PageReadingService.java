package searchengine.services.auxiliary;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.dto.ResultOfPageReading;

import java.io.IOException;
import java.net.UnknownHostException;

@Component
@Slf4j
public class PageReadingService {
    private final static String EMPTY_CONTENT = "N/A";

    private static String agent;
    private static String referrer;

    @Value("${connection-settings.agent}")
    public void setAgent (String agent) {
        PageReadingService.agent = agent;
    }
    @Value("${connection-settings.referrer}")
    public void setReferrer (String referrer) {
        PageReadingService.referrer = referrer;
    }


    public static ResultOfPageReading readPage(String fullAddr) {
        Connection.Response response;
        int responseCode;
        String pageContent;
        String errorMessage;

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            log.info("Поток завершился нештатно при ожидании ответа от страницы: " + fullAddr);
        }

        try {
            Connection connection = Jsoup.connect(fullAddr).maxBodySize(0).userAgent(agent).referrer(referrer);
            response = connection.method(Connection.Method.GET).execute();
            responseCode = response.statusCode();
            pageContent = response.body();
            errorMessage = "";
        } catch (HttpStatusException ex) {
            responseCode = ex.getStatusCode();
            pageContent = EMPTY_CONTENT;
            log.info("Не удалось получить содержимое страницы " + fullAddr + ", код ошибки: " + responseCode + ", " + ex.getLocalizedMessage());
            errorMessage = String.format("Не удалось получить содержимое страницы %s, код ошибки %s, %s}", fullAddr, responseCode, ex.getLocalizedMessage());
        } catch (UnknownHostException ex) {
            responseCode = HttpStatus.NOT_FOUND.value();
            pageContent = EMPTY_CONTENT;
            log.info("IP-адрес хоста не может быть определен при получении содержимого страницы: " + fullAddr);
            errorMessage = String.format("IP-адрес хоста не может быть определен при получении содержимого страницы %s", fullAddr);
        } catch (IOException ex) {
            errorMessage = ex.getMessage();
            int i = errorMessage.indexOf("Status=");
            pageContent = EMPTY_CONTENT;
            if (i > 0) {
                responseCode = Integer.parseInt(errorMessage.substring(i + 7, i + 10));
            } else {
                responseCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            }
            log.info("I/O Exception occurred!!!:>> Page: " + fullAddr + ", error: " + ex.getLocalizedMessage() + ", code: " + responseCode);
        }
        return new ResultOfPageReading(pageContent, responseCode, errorMessage);
    }

}
