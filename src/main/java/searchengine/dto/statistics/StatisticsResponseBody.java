package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponseBody {
    private boolean result;
    private StatisticsData statistics;
}
