package com.stream.config;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description:
 * @Author 一枚路过的程序猿
 * @Date 2024/3/13 14:26
 * @Version 1.0
 */
@Data
public class StreamConfig {

    private String username; // 摄像头用户名
    private String password; // 摄像头密码
    private String cameraIp; // 摄像头IP
    private AtomicInteger count = new AtomicInteger(0); //观看人数
    private String flvUrl;  // 动态生成的FLV地址

}
