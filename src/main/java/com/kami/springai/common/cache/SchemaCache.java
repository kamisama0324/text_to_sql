package com.kami.springai.common.cache;

import com.kami.springai.text2sql.model.DatabaseSchema;
import com.kami.springai.text2sql.service.SchemaDiscoveryService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据库结构缓存服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaCache {

    private final SchemaDiscoveryService schemaDiscoveryService;
    private final ConcurrentMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MINUTES = 30; // 缓存30分钟
    private static final long CLEANUP_INTERVAL_MINUTES = 5; // 每5分钟清理一次
    private ScheduledExecutorService cleanupExecutor;

    @PostConstruct
    public void init() {
        // 初始化清理任务
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("schema-cache-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        // 每5分钟执行一次清理
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 
            CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        log.info("Schema cache cleanup scheduled every {} minutes", CLEANUP_INTERVAL_MINUTES);
    }
    
    @PreDestroy
    public void destroy() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 定期清理过期缓存条目
     */
    @Scheduled(fixedDelay = 300000) // 5分钟执行一次
    public void cleanupExpiredEntries() {
        int removedCount = 0;
        for (var entry : localCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                localCache.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            log.info("Cleaned up {} expired cache entries", removedCount);
        }
    }

    /**
     * 获取数据库结构（带缓存）
     */
    @Cacheable(value = "database-schema", key = "#dataSourceId")
    public DatabaseSchema getSchema(String dataSourceId) {
        log.debug("获取数据源[{}]的数据库结构", dataSourceId);
        
        // 先检查本地缓存
        CacheEntry entry = localCache.get(dataSourceId);
        if (entry != null && !entry.isExpired()) {
            log.debug("从本地缓存获取数据源[{}]的数据库结构", dataSourceId);
            return entry.getSchema();
        }
        
        // 缓存未命中，从数据库获取
        try {
            DatabaseSchema schema = schemaDiscoveryService.discoverSchema(dataSourceId);
            
            // 更新本地缓存
            localCache.put(dataSourceId, CacheEntry.builder()
                    .schema(schema)
                    .createTime(LocalDateTime.now())
                    .build());
            
            log.info("数据源[{}]的数据库结构已缓存", dataSourceId);
            return schema;
            
        } catch (Exception e) {
            log.error("获取数据库结构失败: {}", e.getMessage(), e);
            
            // 如果有过期缓存，返回过期缓存作为备用
            if (entry != null) {
                log.warn("使用过期缓存作为备用: {}", dataSourceId);
                return entry.getSchema();
            }
            
            throw e;
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache(String dataSourceId) {
        localCache.remove(dataSourceId);
        log.info("已清除数据源[{}]的数据库结构缓存", dataSourceId);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        localCache.clear();
        log.info("已清除所有数据库结构缓存");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        long expiredCount = localCache.values().stream()
                .filter(CacheEntry::isExpired)
                .count();
        
        return CacheStats.builder()
                .localCacheSize(localCache.size())
                .expiredCount((int) expiredCount)
                .cacheEnabled(true)
                .redisConnected(false) // 简化版本暂不支持Redis
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class CacheEntry {
        private DatabaseSchema schema;
        private LocalDateTime createTime;
        
        public boolean isExpired() {
            return createTime.plusMinutes(CACHE_TTL_MINUTES).isBefore(LocalDateTime.now());
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class CacheStats {
        private int localCacheSize;
        private int expiredCount;
        private boolean cacheEnabled;
        private boolean redisConnected;
        
        @Override
        public String toString() {
            return String.format("CacheStats{localSize=%d, expired=%d, enabled=%s, redis=%s}", 
                    localCacheSize, expiredCount, cacheEnabled, redisConnected);
        }
    }
}