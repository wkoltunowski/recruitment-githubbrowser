package com.falco.recruitment.githubbrowser;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Factory {

    private ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(cachingClient());
    }

    private CloseableHttpClient cachingClient() {
        CacheConfig cacheConfig = CacheConfig.custom()
                .setMaxCacheEntries(5000)
                .setMaxObjectSize(8192)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(30000)
                .setStaleConnectionCheckEnabled(false)
                .build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
        poolingConnectionManager.setMaxTotal(120);

        return CachingHttpClients.custom()
                .setCacheConfig(cacheConfig)
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory());
        restTemplate.setErrorHandler(new ErrorStatusInResponseErrorHandler());
        return restTemplate;
    }
}
