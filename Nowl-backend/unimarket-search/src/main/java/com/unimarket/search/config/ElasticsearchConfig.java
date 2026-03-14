package com.unimarket.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置类
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.unimarket.search.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    /**
     * Elasticsearch REST 客户端连接池配置（避免高并发下等待连接导致排队）
     */
    @Value("${unimarket.elasticsearch.client.max-conn-total:10}")
    private int maxConnTotal;

    @Value("${unimarket.elasticsearch.client.max-conn-per-route:10}")
    private int maxConnPerRoute;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                ClientConfiguration.builder()
                        .connectedTo(elasticsearchUri);

        // 如果配置了用户名密码，则添加认证
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            builder.withBasicAuth(username, password);
        }

        return builder
                .withClientConfigurer(ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from(httpClientBuilder ->
                        httpClientBuilder
                                .setMaxConnTotal(maxConnTotal)
                                .setMaxConnPerRoute(maxConnPerRoute)
                ))
                .withConnectTimeout(5000)
                .withSocketTimeout(60000)
                .build();
    }
}
