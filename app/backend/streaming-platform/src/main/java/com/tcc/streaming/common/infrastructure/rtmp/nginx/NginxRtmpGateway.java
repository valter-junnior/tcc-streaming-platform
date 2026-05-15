package com.tcc.streaming.common.infrastructure.rtmp.nginx;

import com.tcc.streaming.common.infrastructure.rtmp.RtmpGateway;

public class NginxRtmpGateway implements RtmpGateway {
    @Override
    public String getName() {
        return "nginx";
    }
    // Implemente métodos específicos do Nginx RTMP aqui
}
