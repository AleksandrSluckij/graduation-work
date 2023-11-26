package searchengine.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.ErrorResponseBody;
import searchengine.dto.SuccessResponseBody;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.DatabaseConnService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@Api(tags = "Internal application requests")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final DatabaseConnService dataService;


    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, DatabaseConnService dataService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.dataService = dataService;
    }

    @GetMapping("/statistics")
    @ApiOperation("Retrieving statistic info")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    @ApiOperation("Command to start total indexing/reindexing process")
    public ResponseEntity startIndexing () {
        String message = indexingService.startTotalIndexing(dataService);
        if (message == null) {
            return ResponseEntity.ok(new SuccessResponseBody());
        } else {
            return ResponseEntity.ok(new ErrorResponseBody(message));
        }
    }

    @GetMapping("/stopIndexing")
    @ApiOperation("Command to stop total indexing/reindexing process")
    public ResponseEntity stopIndexing () {
        String message = indexingService.stopTotalIndexing();
        if (message == null) {
            return ResponseEntity.ok(new SuccessResponseBody());
        } else {
            return ResponseEntity.ok(new ErrorResponseBody(message));
        }
    }
}
