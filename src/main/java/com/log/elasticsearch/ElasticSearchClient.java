package com.log.elasticsearch;


import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ElasticSearchClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        EmbeddedElastic embeddedElastic = EmbeddedElastic.builder()
                .withElasticVersion("6.2.4")
                .withSetting(PopularProperties.HTTP_PORT, 21121)
                .withStartTimeout(2, TimeUnit.MINUTES)
                .build();

        embeddedElastic.start();

        RestClient restClient = RestClient.builder(
                new HttpHost("localhost", 21121, "http")).build();

        HttpEntity entity = new NStringEntity(
                "{\n" +
                        "    \"company\" : \"qbox\",\n" +
                        "    \"title\" : \"Elasticsearch rest client\"\n" +
                        "}", ContentType.APPLICATION_JSON);

        Response indexResponse = restClient.performRequest(
                "PUT",
                "/blog/post/1",
                Collections.<String, String>emptyMap(),
                entity);

        System.out.println("------"+EntityUtils.toString(indexResponse.getEntity()));

        Response get = restClient.performRequest(
                "GET",
                "/blog/post/1",
                Collections.<String, String>emptyMap(),
                entity);
    }
}

