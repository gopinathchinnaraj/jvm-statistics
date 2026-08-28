package com.gopinath_statslab.controller;

import com.gopinath_statslab.model.ColumnStats;
import com.gopinath_statslab.service.StatisticsCollectorService;
import com.gopinath_statslab.service.StatisticsStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * StatisticsController — REST API for Phase 2 statistics.
 *
 * POST /api/statistics/collect         → run full stats collection
 * GET  /api/statistics/columns         → all column stats (for the table)
 * GET  /api/statistics/column/{name}   → one column's stats
 * GET  /api/statistics/status          → is store populated? corrupted?
 * POST /api/statistics/corrupt         → corrupt the stats (demo)
 * POST /api/statistics/restore         → restore good stats (demo)
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsCollectorService collectorService;
    private final StatisticsStore statisticsStore;

    @PostMapping("/collect")
    public ResponseEntity<Map<String, ColumnStats>> collect() {
        log.info("POST /api/statistics/collect — starting collection");
        Map<String, ColumnStats> result = collectorService.collectAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/columns")
    public ResponseEntity<Map<String, ColumnStats>> allColumns() {
        if (!statisticsStore.hasStats()) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(statisticsStore.getAll());
    }

    @GetMapping("/column/{name}")
    public ResponseEntity<?> oneColumn(@PathVariable String name) {
        ColumnStats stats = statisticsStore.get(name);
        if (stats == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
            "hasStats",    statisticsStore.hasStats(),
            "isCorrupted", statisticsStore.isCorrupted(),
            "columns",     statisticsStore.getAll().keySet()
        ));
    }

    @PostMapping("/corrupt")
    public ResponseEntity<Map<String, Object>> corrupt() {
        if (!statisticsStore.hasStats()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Collect statistics first before corrupting."));
        }
        statisticsStore.corrupt();
        log.warn("Statistics CORRUPTED via API");
        return ResponseEntity.ok(Map.of(
            "status", "CORRUPTED",
            "message", "Statistics have been deliberately corrupted. Run a query to see the bad plan."
        ));
    }

    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> restore() {
        statisticsStore.restore();
        log.info("Statistics RESTORED via API");
        return ResponseEntity.ok(Map.of(
            "status", "GOOD",
            "message", "Statistics restored to accurate state."
        ));
    }
}
