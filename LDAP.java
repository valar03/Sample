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
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.support.PooledContextSource;

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
            @Qualifier("adContextSource") ContextSource adContextSourceBean // ✅ FIX: correct type
    ) throws Exception {

        // ✅ Try initializing the LDAP context to ensure certs are loaded
        if (adContextSourceBean instanceof AbstractContextSource ctx) {
            ctx.getReadOnlyContext(); // Triggers trust handshake (Venafi etc.)
            System.out.println("✅ LDAP context initialized via: " + ctx.getUrls()[0]);
        } else {
            System.out.println("⚠️ LDAP bean is not an AbstractContextSource, skipping init");
        }

        // 🔐 Use default SSL context (with Venafi/OS-managed truststore)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new SecureRandom());

        HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        return restTemplate -> restTemplate.setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(client));
    }
}
