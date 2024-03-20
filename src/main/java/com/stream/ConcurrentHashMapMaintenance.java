package com.stream;

import com.stream.cache.StreamCache;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @Description: 自定义 有效性线程安全Map
 * @Author 一枚路过的程序猿
 * @Date 2024/3/13
 * @Version 1.0
 */
public class ConcurrentHashMapMaintenance {
    private final Map<String, Object> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long defaultExpirationTime; // 默认键的过期时间

    public ConcurrentHashMapMaintenance() {
        this.defaultExpirationTime = 1000 * 60;
        scheduler.scheduleAtFixedRate(this::cleanupExpiredKeys, 0, 1000 * 5, TimeUnit.MILLISECONDS);
    }

    public ConcurrentHashMapMaintenance(long defaultExpirationTime) {
        this.defaultExpirationTime = defaultExpirationTime;
    }

    public void startMaintenanceTask(long checkInterval) {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredKeys, 0, checkInterval, TimeUnit.MILLISECONDS);
    }

    private void cleanupExpiredKeys() {
        long currentTime = System.currentTimeMillis();
        for (String key : map.keySet()) {
            long expirationTime = getExpirationTime(key);
            if (expirationTime > 0 && expirationTime < currentTime) {

                map.remove(key);
                map.remove(key + "_expiration");

                StreamCache.PUSH_STREAM_CACHE.get(key).setExitCode(1);
                StreamCache.PUSH_STREAM_CACHE.remove(key);

            }
        }
    }

    private long getExpirationTime(String key) {
        return Long.parseLong((String) map.getOrDefault(key + "_expiration", "0"));
    }

    public void putWithExpiration(String key, Object value) {
        putWithExpiration(key, value, defaultExpirationTime);
    }

    public void putWithExpiration(String key, Object value, long expirationTime) {
        map.put(key, value);
        if (expirationTime > 0) {
            putWithExpiration(key, expirationTime);
        }
    }

    private void putWithExpiration(String key, long expirationTime) {
        long expiration = System.currentTimeMillis() + expirationTime;
        map.put(key + "_expiration", String.valueOf(expiration));
    }

    public <T> T get(String key){
        return (T) map.get(key);
    }

    public void resetExpirationTime(String key) {
        if (map.containsKey(key)) {
            putWithExpiration(key, defaultExpirationTime);
        }
    }

    public void remove(String key){
        map.remove(key);
        map.remove(key + "_expiration");
    }

    public void stopMaintenanceTask() {
        scheduler.shutdown();
    }
}
