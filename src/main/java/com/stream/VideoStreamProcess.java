package com.stream;

import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import java.util.HashMap;
import java.util.Map;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;

/**
 * @Description:
 * @Author 一枚路过的程序猿
 * @Date 2024/3/13 14:24
 * @Version 1.0
 */
@Data
@Slf4j
public class VideoStreamProcess {

    private String rtmpUrl;
    private String rtspUrl;
    private String cameraIp;
    private int exitCode = 0;
    private FFmpegFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;

    public VideoStreamProcess(String rtspUrl, String rtmpUrl, String cameraIp) {
        this.rtspUrl = rtspUrl;
        this.rtmpUrl = rtmpUrl;
        this.cameraIp = cameraIp;
    }

    public void convertRtspToFlv() {

        try {

            // 设置FFmpeg日志级别为信息级，便于调试和监控
            avutil.av_log_set_level(avutil.AV_LOG_INFO);
            FFmpegLogCallback.set();

            // 初始化一个新的FFmpegFrameGrabber来捕获RTSP视频流
            grabber = new FFmpegFrameGrabber(rtspUrl);

            // 配置grabber使用TCP传输协议，以提高稳定性
            grabber.setOption("rtsp_transport", "tcp");

            // 设置RTSP标志为“prefer_tcp”，即优先使用TCP进行RTP传输
            grabber.setOption("rtsp_flags", "prefer_tcp");

            // 设置缓冲区大小为1024000，以便于处理大量数据
            grabber.setOption("buffer_size", "1024000");

            // 设置处理视频流的线程数为1
            grabber.setOption("threads", "1");

            // 启动grabber开始捕获视频流
            grabber.start();

            // 调用startRecorder方法，准备将捕获的流推送到RTMP URL
            startRecorder(rtmpUrl);

            // 清空grabber的缓冲区
            grabber.flush();

            // 初始化用于处理视频帧的变量
            AVPacket pkt;
            long dts = 0;
            long pts = 0;

            log.info("摄像头：{}，开始推流", cameraIp);

            // 循环捕获和推送视频帧
            for (int noFrameIndex = 0 ,timebase = 0, errIndex = 0; noFrameIndex < 5 && errIndex < 5; ) {
                long startTime = System.currentTimeMillis();

                // 检查退出标志
                if (exitCode == 1) {
                    log.info("摄像头：{}，停止推流，心跳超时", cameraIp);
                    break;
                }

                // 捕获一个视频包
                pkt = grabber.grabPacket();
                if (pkt == null || pkt.size() == 0 || pkt.data() == null) {
                    // 空包记录次数跳过
                    noFrameIndex++;
                    continue;
                }

                // 过滤掉音频数据包
                if (pkt.stream_index() == 1) {
                    av_packet_unref(pkt);
                    continue;
                }

                // 校正DTS和PTS值，以解决播放器续播问题
                pkt.pts(pts);
                pkt.dts(dts);

                // 尝试推送视频包，记录错误次数
                errIndex += (recorder.recordPacket(pkt) ? 0 : 1);

                // 计算下一帧的PTS和DTS
                timebase = grabber.getFormatContext().streams(pkt.stream_index()).time_base().den();
                pts += timebase / (int) grabber.getFrameRate();
                dts += timebase / (int) grabber.getFrameRate();

                // 释放捕获的视频包资源
                av_packet_unref(pkt);

                // 控制推流速度，以匹配原视频帧率
                long endTime = System.currentTimeMillis();
                if ((long) (1000 / grabber.getFrameRate()) - (endTime - startTime) > 0) {
                    Thread.sleep((long) (1000 / grabber.getFrameRate()) - (endTime - startTime));
                }
            }
        } catch (Exception e){
            log.error("摄像头：{}，推流异常：{}",cameraIp, e.getMessage(), e);
        }finally {
            log.info("摄像头：{}，推流结束", cameraIp);
            release();
        }
    }

    private void startRecorder(String rtmpUrl) throws FFmpegFrameRecorder.Exception {

        // 初始化FFmpegFrameRecorder，设置推流地址、视频宽度和高度
        recorder = new FFmpegFrameRecorder(rtmpUrl, grabber.getImageWidth(), grabber.getImageHeight());

        // 设置是否交错处理帧，有助于改善直播流的播放质量
        recorder.setInterleaved(true);

        // 设置视频帧率，这里直接使用grabber的帧率保持一致
        recorder.setFrameRate(grabber.getFrameRate());

        // 设置关键帧间隔（GOP大小），这里设置为帧率的两倍，每两秒一个I帧
        recorder.setGopSize((int) (grabber.getFrameRate() * 2));

        // 设置视频的比特率，这里直接使用grabber的视频比特率
        recorder.setVideoBitrate(grabber.getVideoBitrate());

        // 设置录制的媒体封装格式为FLV，适用于流媒体
        recorder.setFormat("flv");

        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        // 设置像素格式为YUV420P，H.264编码通常使用的像素格式
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

        // 定义并设置视频编码选项，用于优化编码过程
        Map<String, String> videoOption = new HashMap<>();

        // 设置tune选项为zerolatency，优化编码过程以减少延迟
        videoOption.put("tune", "zerolatency");

        // 设置preset选项为ultrafast，提高编码速度，降低延迟，可能牺牲一些视频质量
        videoOption.put("preset", "ultrafast");

        // 设置crf选项控制视频的常量质量模式，18~28是合理范围，这里选择28以优化速度和质量的平衡
        videoOption.put("crf", "28");

        // 设置编码使用的线程数为1，可以根据实际情况调整
        recorder.setVideoOption("threads", "1");

        // 设置GOP的最小间隔，有助于确保视频流的稳定性
        videoOption.put("keyint_min", "25");
        recorder.setOptions(videoOption);

        // 设置RDOQ算法级别为1，优化视频质量和码率之间的平衡
        recorder.setTrellis(1);

        // 设置最大延迟为0，尽量减少推流的延迟
        recorder.setMaxDelay(0);

        // 使用grabber的AVFormatContext启动recorder，这可以确保录制设置与视频源相匹配
        AVFormatContext fc = grabber.getFormatContext();
        recorder.start(fc);

    }

    public void release() {
        try {
            grabber.stop();
            grabber.close();
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}