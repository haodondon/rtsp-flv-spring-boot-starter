package com.stream.cache;

import com.stream.ConcurrentHashMapMaintenance;
import com.stream.VideoStreamProcess;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 内部缓存
 * @Author 一枚路过的程序猿
 * @Date 2024/3/13 15:03
 * @Version 1.0
 */
public class StreamCache {

    public static ConcurrentHashMap<String, VideoStreamProcess> PUSH_STREAM_CACHE = new ConcurrentHashMap<>();
    public static ConcurrentHashMapMaintenance STREAM_ACTIVE_INFO = new ConcurrentHashMapMaintenance();

}
