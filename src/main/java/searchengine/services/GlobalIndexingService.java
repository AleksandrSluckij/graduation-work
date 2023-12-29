package searchengine.services;

import org.springframework.scheduling.annotation.Async;

public interface GlobalIndexingService {
    @Async
    void startTotalIndexing();

    void stopTotalIndexing ();
}
