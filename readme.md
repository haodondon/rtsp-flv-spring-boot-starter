
## rtsp转flv中间件


### 1. 自行搭建nginx，并加载flv模块ngx_http_flv_module，配置如下
```
rtmp{
    server {
        listen 1935;
        chunk_size 4096;
        application live{
            live on;
        }
    }
}

server{

    listen       80;
    server_name  localhost;
    location /flv {
        flv_live on;
        chunked_transfer_encoding on;
        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Credentials' 'true';
        add_header "Access-Control-Allow-Methods" "*";
        add_header "Access-Control-Allow-Headers" "Content-Type,Access-Token";
        add_header "Access-Control-Expose-Headers" "*"; 
    }

}
```

### 2. mvn加载jar

> lib/onvif-2016-03-16.jar

>mvn安装命令：mvn install:install-file -Dfile=/path/lib/onvif-2016-03-16.jar -DgroupId=com.stream -DartifactId=onvif -Dversion=1.0 -Dpackaging=jar

```
<dependency>
    <groupId>com.stream</groupId>
    <artifactId>onvif</artifactId>
    <version>1.0</version>
</dependency>
```

### 3. 项目配置

yaml配置
```yaml
stream:
    ip: 192.168.5.211 # nginx IP地址
    rtmpPrefix: live  # rtmp配置前缀
    rtmpPort: 1935    # rtmp端口
    flvPort: 80       # flv端口
    flvPrefix: flv    # flv配置前缀
```

项目注入核心类 `VideoHandler`

### openStream()：开始推流
>同一相机支持多次调用此接口
>
>**入参**： 相机IP:cameraIp，相机用户名：username，相机密码：password；
>
>**返回**：flv地址

### close()：停止推流
>调用几次**开始推流**接口，此接口需调用几次
>
>**入参**： 相机IP:ip

### heartbeat()：心跳
>默认心跳60秒，在开始推流之后，需在60秒之内进行心跳，超时会自动停止推流
> 
>**入参**： 相机IP:ip
>
>**返回**：flv地址