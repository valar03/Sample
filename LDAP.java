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
import org.springframework.ldap.core.support.DefaultDirContextValidator;

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
            @Qualifier("adContextSource") org.springframework.ldap.core.support.ContextSource adContextSourceBean
    ) throws Exception {

        // ✅ Trigger AD context early to force Venafi + truststore loading
        if (adContextSourceBean instanceof AbstractContextSource) {
            AbstractContextSource ctx = (AbstractContextSource) adContextSourceBean;
            try {
                ctx.getReadOnlyContext(); // Triggers cert loading
                System.out.println("✅ LDAP trust established with: " + ctx.getUrls()[0]);
            } catch (Exception e) {
                System.err.println("❌ Failed to init LDAP trust: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ Provided context is not an AbstractContextSource: " + adContextSourceBean.getClass().getName());
        }

        // 🔐 Use default truststore (e.g. Venafi-managed)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new SecureRandom());

        HttpClient client = HttpClients.custom()
                .setSSLContext(sslContext)
                .build();

        return restTemplate -> restTemplate.setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(client));
    }
}
