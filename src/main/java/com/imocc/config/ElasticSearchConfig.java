package com.imocc.config;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Administrator on 2018/1/14.
 *
 * es配置
 */
@Configuration
public class ElasticSearchConfig {
    @Bean
    public TransportClient esClient() throws UnknownHostException{
        Settings settings = Settings.builder().put("cluster.name", "angel")
                .put("client.transport.sniff", true)
                .build();
        InetSocketTransportAddress master=new InetSocketTransportAddress(
                InetAddress.getByName("192.168.188.34"),9300
        );

        TransportClient client=new PreBuiltTransportClient(settings)
                .addTransportAddress(master);
        return client;
    }
}
