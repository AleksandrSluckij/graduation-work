package searchengine.config;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public final class IndexingStatus {
    @Getter
    private static boolean alreadyIndexing;
    public static void setIndexingTrue () {
        alreadyIndexing = true;
    }
    public static void setIndexingFalse () {
        alreadyIndexing = false;
    }
}
