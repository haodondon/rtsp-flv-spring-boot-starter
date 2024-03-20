package com.stream;

import com.stream.cache.StreamCache;
import com.stream.config.RtmpConfig;
import com.stream.config.StreamConfig;
import com.stream.onvif.OnvifUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: 对外暴露接口
 * @Author 一枚路过的程序猿
 * @Date 2024/3/15 9:36
 * @Version 1.0
 */
@Component
@Slf4j
public class VideoHandler {

    @Autowired
    private RtmpConfig rtmpConfig;

    public String openStream(StreamConfig streamConfig){

        StreamConfigValidator.validate(streamConfig);

        StreamConfig configCache = StreamCache.STREAM_ACTIVE_INFO.get(streamConfig.getCameraIp());

        if (configCache == null) {

            configCache = streamConfig;

            String rtspUrl = OnvifUtil.getRtspUrl(configCache);

            if (rtspUrl == null) throw new IllegalArgumentException("getRtspUrl error，check camera info");

            Thread thread = new Thread(()->{

                VideoStreamProcess videoStreamProcess = new VideoStreamProcess(rtspUrl, rtmpConfig.getRtmpUrl(streamConfig.getCameraIp()), streamConfig.getCameraIp());
                StreamCache.PUSH_STREAM_CACHE.put(streamConfig.getCameraIp(), videoStreamProcess);
                videoStreamProcess.convertRtspToFlv();

            });

            thread.start();

            configCache.setFlvUrl(rtmpConfig.getFlvUrl(streamConfig.getCameraIp()));

        }

        configCache.getCount().incrementAndGet();
        StreamCache.STREAM_ACTIVE_INFO.putWithExpiration(streamConfig.getCameraIp(), configCache);

        log.info("摄像头：{}，推流成功，flv：{}", streamConfig.getCameraIp(), configCache.getFlvUrl());
        return configCache.getFlvUrl();
    }

    public void closeStream(String ip){

        StreamConfig streamConfig = StreamCache.STREAM_ACTIVE_INFO.get(ip);
        if (streamConfig == null) return;
        if (streamConfig.getCount().decrementAndGet() == 0) {

            StreamCache.STREAM_ACTIVE_INFO.remove(ip);
            StreamCache.PUSH_STREAM_CACHE.get(ip).setExitCode(1);
            StreamCache.PUSH_STREAM_CACHE.remove(ip);

        }

        StreamCache.STREAM_ACTIVE_INFO.putWithExpiration(ip, streamConfig);

        log.info("摄像头：{}，当前观看人数：{}", ip, streamConfig.getCount());

    }

    public void heartbeat(String ip){

        StreamCache.STREAM_ACTIVE_INFO.resetExpirationTime(ip);

    }

}
