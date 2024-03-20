package com.stream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @Description:
 * @Author 一枚路过的程序猿
 * @Date 2024/3/15 10:10
 * @Version 1.0
 */
@Component
@ConfigurationProperties(prefix = "stream")
@Data
public class RtmpConfig {

    private String ip;
    private String rtmpPrefix;
    private String rtmpPort;
    private String flvPort;
    private String flvPrefix;

    @PostConstruct
    public void validate() {
        if (ip == null || rtmpPrefix == null || rtmpPort == null || flvPort == null || flvPrefix == null) {
            throw new IllegalStateException("RTMP configuration properties are not properly configured.");
        }
    }

    public String getRtmpUrl(String cameraIp){
        return String.format("rtmp://%s:%s/%s/%s", ip, rtmpPort, rtmpPrefix, cameraIp.replace(".",""));
    }

    public String getFlvUrl(String cameraIp){
        return String.format("http://%s:%s/%s?app=%s&stream=%s", ip, flvPort, flvPrefix, rtmpPrefix, cameraIp.replace(".",""));
    }

}
