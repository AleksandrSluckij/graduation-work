package searchengine.services.auxiliary;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionSettings;
import searchengine.dto.ResultOfPageReading;

import java.io.IOException;
import java.net.UnknownHostException;

import static searchengine.config.LoggingConfig.LOGGER;
import static searchengine.config.LoggingConfig.MARKER;

@Component
public class PageReadingService {
    private static ConnectionSettings connectionSettings;

    @Autowired
    public PageReadingService(ConnectionSettings connectionSettings) {
        PageReadingService.connectionSettings = connectionSettings;
    }

    public static ResultOfPageReading readPage(String fullAddr) {
        Connection.Response response;
        int responseCode;
        String pageContent;
        String errorMessage;
        String EMPTY_CONTENT = "N/A";

        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            LOGGER.info(MARKER, "Поток завершился нештатно при ожидании ответа от страницы: " + fullAddr);
        }

        try {
            Connection connection = Jsoup.connect(fullAddr).maxBodySize(0).userAgent(connectionSettings.getAgent()).referrer(connectionSettings.getReferrer());
            response = connection.method(Connection.Method.GET).execute();
            responseCode = response.statusCode();
            pageContent = response.body();
            String EMPTY_MESSAGE = "";
            errorMessage = EMPTY_MESSAGE;
        } catch (HttpStatusException ex) {
            responseCode = ex.getStatusCode();
            pageContent = EMPTY_CONTENT;
            LOGGER.info(MARKER, "Не удалось получить содержимое страницы " + fullAddr + ", код ошибки: " + responseCode + ", " + ex.getLocalizedMessage());
            errorMessage = String.format("Не удалось получить содержимое страницы %s, код ошибки %s, %s}", fullAddr, responseCode, ex.getLocalizedMessage());
        } catch (UnknownHostException ex) {
            responseCode = HttpStatus.NOT_FOUND.value();
            pageContent = EMPTY_CONTENT;
            LOGGER.info(MARKER, "IP-адрес хоста не может быть определен при получении содержимого страницы: " + fullAddr);
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
            LOGGER.info(MARKER, "I/O Exception occurred!!!:>> Page: " + fullAddr + ", error: " + ex.getLocalizedMessage() + ", code: " + responseCode);
        }
        return new ResultOfPageReading(pageContent, responseCode, errorMessage);
    }

}
