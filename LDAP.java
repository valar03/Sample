package com.example.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.ldap.core.support.AbstractContextSource;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;

@Configuration
public class SpftmRestConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplateCustomizer mtlsClientRestTemplateCustomizer(
            @Qualifier("adContextSource") AbstractContextSource adContextSource) throws Exception {

        // 🔄 Trigger LDAP context initialization to ensure LDAP SSL handshake happens early
        adContextSource.getReadOnlyContext();

        System.out.println("✅ Triggered AD context source initialization (forces LDAP trust setup)");

        // 🛡️ Use default JVM SSL context (already updated by Venafi)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new SecureRandom());

        HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        return restTemplate -> restTemplate.setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(client));
    }
}
