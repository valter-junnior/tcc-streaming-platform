package com.tcc.streaming.consumer.infrastructure.scheduler;

import com.tcc.streaming.stream.core.repositories.StreamEventRepository;
import com.tcc.streaming.stream.core.repositories.StreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task para agregar métricas diárias
 * TODO: Implementação pendente — este scheduler está registrado mas os métodos são no-ops.
 */
@Component
public class MetricsAggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsAggregationScheduler.class);
    private final StreamRepository streamRepository;
    private final StreamEventRepository streamEventRepository;

    public MetricsAggregationScheduler(
            StreamRepository streamRepository,
            StreamEventRepository streamEventRepository) {
        this.streamRepository = streamRepository;
        this.streamEventRepository = streamEventRepository;
    }

    @Scheduled(cron = "${streaming.cleanup.metrics-aggregation-cron:0 0 4 * * *}")
    public void aggregateDailyMetrics() {
        log.info("[MetricsAggregationScheduler] Starting daily metrics aggregation");
        
        try {
            // TODO: Implementar agregação de métricas
            // 1. Buscar todas as streams encerradas nas últimas 24h
            // 2. Calcular médias (viewers, duração, etc)
            // 3. Persistir em tabela de estatísticas agregadas
            // 4. Limpar eventos muito antigos (> 30 dias) para economizar espaço
            
            log.info("[MetricsAggregationScheduler] Metrics aggregation completed");
            
        } catch (Exception e) {
            log.error("[MetricsAggregationScheduler] Error during metrics aggregation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Limpa eventos muito antigos (> 30 dias) para economizar espaço
     * Executa semanalmente aos domingos às 5h
     */
    @Scheduled(cron = "${streaming.cleanup.old-events-cron:0 0 5 * * SUN}")
    public void cleanupOldEvents() {
        log.info("[MetricsAggregationScheduler] Starting cleanup of old events (> 30 days)");
        
        try {
            // TODO: Implementar limpeza de eventos antigos
            // streamEventRepository.deleteEventsOlderThan(LocalDateTime.now().minusDays(30));
            
            log.info("[MetricsAggregationScheduler] Old events cleanup completed");
            
        } catch (Exception e) {
            log.error("[MetricsAggregationScheduler] Error cleaning old events: {}", e.getMessage(), e);
        }
    }
}
