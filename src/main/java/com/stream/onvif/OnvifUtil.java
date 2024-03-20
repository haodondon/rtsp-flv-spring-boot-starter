package com.stream.onvif;

import com.stream.config.StreamConfig;
import de.onvif.soap.OnvifDevice;
import lombok.extern.slf4j.Slf4j;
import org.onvif.ver10.schema.Profile;
import java.util.List;

/**
 * @Description:
 * @Author 一枚路过的程序猿
 * @Date 2024/3/13 16:26
 * @Version 1.0
 */
@Slf4j
public class OnvifUtil {

    /**
     * 使用onvif获取摄像头RTSP地址
     * @param streamConfig
     * @return
     */
    public static String getRtspUrl(StreamConfig streamConfig){
        try {

            OnvifDevice nvt = new OnvifDevice(streamConfig.getCameraIp(), streamConfig.getUsername(), streamConfig.getPassword());
            List<Profile> profiles=nvt.getDevices().getProfiles();
            String profileToken=profiles.get(0).getToken();
            String rtmpUrl= nvt.getMedia().getRTSPStreamUri(profileToken);
            return rtmpUrl.replace("rtsp://", "rtsp://".concat(streamConfig.getUsername()).concat(":").concat(streamConfig.getPassword()).concat("@"));
        } catch (Exception e) {
            log.error("onvif get rtspUrl fail：{}", e.getMessage(), e);
        }
        return null;
    }

}
