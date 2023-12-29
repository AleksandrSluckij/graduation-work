package searchengine.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.IndexingStatus;
import searchengine.dto.ErrorResponseBody;
import searchengine.dto.SuccessResponseBody;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.GlobalIndexingService;
import searchengine.services.SinglePageIndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@Api(tags = "Internal application requests")
public class ApiController {

    private final StatisticsService statisticsService;
    private final GlobalIndexingService globalIndexingService;
    private final SinglePageIndexingService singlePageIndexingService;


    @Autowired
    public ApiController(StatisticsService statisticsService, GlobalIndexingService globalIndexingService,
                         SinglePageIndexingService singlePageIndexingService) {
        this.statisticsService = statisticsService;
        this.globalIndexingService = globalIndexingService;
        this.singlePageIndexingService = singlePageIndexingService;
    }

    @GetMapping("/statistics")
    @ApiOperation("Retrieving statistic info")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    @ApiOperation("Command to start total indexing/reindexing process")
    public ResponseEntity startIndexing () {
        if (IndexingStatus.isAlreadyIndexing()) {
            return ResponseEntity.ok(new ErrorResponseBody("Индексация уже запущена"));
        } else {
            globalIndexingService.startTotalIndexing();
            return ResponseEntity.ok(new SuccessResponseBody());
        }
    }

    @GetMapping("/stopIndexing")
    @ApiOperation("Command to stop total indexing/reindexing process")
    public ResponseEntity stopIndexing () {
        if (!IndexingStatus.isAlreadyIndexing()) {
            return ResponseEntity.ok(new ErrorResponseBody("Индексация не запущена"));
        } else {
            globalIndexingService.stopTotalIndexing();
            return ResponseEntity.ok(new SuccessResponseBody());
        }
    }

    @PostMapping("/indexPage")
    @ApiOperation("Command to index single page")
    public ResponseEntity indexPage (@RequestParam("url") String pageAddr) {
        if (IndexingStatus.isAlreadyIndexing()) {
            return ResponseEntity.ok(new ErrorResponseBody("Индексация уже запущена"));
        } else {
            String error = singlePageIndexingService.indexSinglePage(pageAddr);
            return error == null ? ResponseEntity.ok(new SuccessResponseBody()) : ResponseEntity.ok(new ErrorResponseBody(error));
        }
    }
}
