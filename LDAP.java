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
            @Qualifier("adContextSource") Object adContextSourceObj  // generic to allow type casting
    ) throws Exception {

        // ✅ Cast to AbstractContextSource only if it's compatible
        if (adContextSourceObj instanceof AbstractContextSource abstractContextSource) {
            // Trigger LDAP handshake early to make sure Venafi loads cert
            abstractContextSource.getReadOnlyContext();
            System.out.println("✅ AD context source initialized successfully");
        } else {
            System.out.println("⚠️ Could not cast adContextSource to AbstractContextSource");
        }

        // 🔐 Use default SSL context (with Venafi-managed truststore)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new SecureRandom());

        HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        return restTemplate -> restTemplate.setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(client));
    }
}
