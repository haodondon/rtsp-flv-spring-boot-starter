package com.stream;

import com.stream.config.StreamConfig;

/**
 * @Description: 校验配置类
 * @Author 一枚路过的程序猿
 * @Date 2024/3/13
 * @Version 1.0
 */
public class StreamConfigValidator {
    public static void validate(StreamConfig streamConfig) {
        requireNonNull(streamConfig.getUsername(), "username");
        requireNonNull(streamConfig.getPassword(), "password");
        requireNonNull(streamConfig.getCameraIp(), "cameraIp");
    }

    private static void requireNonNull(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " not empty");
        }
    }
}
