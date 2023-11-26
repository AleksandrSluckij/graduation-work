package searchengine.services;

import org.springframework.scheduling.annotation.Async;

public interface IndexingService {
    @Async
    String startTotalIndexing(DatabaseConnService dataService);

    String stopTotalIndexing ();
}
