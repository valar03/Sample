package com.example.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
public class SpftmRestConfiguration {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplateCustomizer mtlsClientRestTemplateCustomizer() throws Exception {
        System.out.println("▶ Initializing dynamic SSL trust setup...");

        // Step 1: Load the default system trust store
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        defaultTmf.init((KeyStore) null); // System default truststore

        // Extract X509TrustManager
        X509TrustManager defaultTm = null;
        for (TrustManager tm : defaultTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                defaultTm = (X509TrustManager) tm;
                break;
            }
        }

        if (defaultTm == null) {
            throw new IllegalStateException("❌ No X509TrustManager found in default trust store");
        }

        System.out.println("✅ Loaded default system trust store");

        // Step 2: Dynamically fetch LDAP certificate
        String ldapHost = "your.ldap.server.com"; // 🔁 Replace with actual LDAP host
        int ldapPort = 636;

        X509Certificate ldapCert = fetchLdapCertificate(ldapHost, ldapPort);
        System.out.println("📄 Fetched LDAP cert: " + ldapCert.getSubjectDN());

        // Step 3: Combine default and LDAP cert into new trust store
        KeyStore combinedTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        combinedTrustStore.load(null);

        int i = 1;
        for (X509Certificate cert : defaultTm.getAcceptedIssuers()) {
            combinedTrustStore.setCertificateEntry("default-" + i++, cert);
        }
        combinedTrustStore.setCertificateEntry("ldap-cert", ldapCert);

        // Step 4: Re-init TrustManager with combined store
        TrustManagerFactory combinedTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        combinedTmf.init(combinedTrustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, combinedTmf.getTrustManagers(), new SecureRandom());

        HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        System.out.println("🚀 Custom SSLContext initialized with combined certs");

        return restTemplate ->
                restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
    }

    private X509Certificate fetchLdapCertificate(String host, int port) throws Exception {
        try (Socket socket = SSLSocketFactory.getDefault().createSocket(host, port)) {
            SSLSession session = ((SSLSocket) socket).getSession();
            Certificate[] certs = session.getPeerCertificates();

            for (Certificate cert : certs) {
                if (cert instanceof X509Certificate) {
                    return (X509Certificate) cert;
                }
            }

            throw new RuntimeException("No X509Certificate found in SSL session");
        }
    }
}
