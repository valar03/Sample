@Bean
public RestTemplateCustomizer mtlsClientRestTemplateCustomizer() throws Exception {
    System.out.println("▶ Initializing dynamic SSL trust setup...");

    // 🔐 Load default system truststore (no path needed!)
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init((KeyStore) null);
    System.out.println("✅ Loaded default system trust store");

    // ✅ Extract default certificates
    X509TrustManager defaultTm = null;
    for (TrustManager tm : tmf.getTrustManagers()) {
        if (tm instanceof X509TrustManager) {
            defaultTm = (X509TrustManager) tm;
            break;
        }
    }

    if (defaultTm == null) {
        throw new IllegalStateException("❌ No default X509TrustManager found");
    }

    // 🌐 Fetch LDAP certificate dynamically
    String ldapHost = "your.ldap.server.com"; // TODO: replace
    int ldapPort = 636;
    X509Certificate ldapCert = fetchLdapCertificate(ldapHost, ldapPort);
    System.out.println("📄 LDAP cert: " + ldapCert.getSubjectX500Principal());

    // 👷 Build new in-memory trust store that includes default certs + LDAP
    KeyStore newTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    newTrustStore.load(null); // Initialize empty

    int i = 1;
    for (X509Certificate cert : defaultTm.getAcceptedIssuers()) {
        newTrustStore.setCertificateEntry("default-" + i++, cert);
    }
    newTrustStore.setCertificateEntry("ldap-cert", ldapCert);

    // Reinitialize TrustManager with combined truststore
    TrustManagerFactory combinedTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    combinedTmf.init(newTrustStore);
    System.out.println("🔐 New TrustManager created with LDAP cert + defaults");

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, combinedTmf.getTrustManagers(), new SecureRandom());

    HttpClient client = HttpClients.custom().setSSLContext(sslContext).build();
    System.out.println("🚀 SSLContext ready and RestTemplate customized");

    return restTemplate -> restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
}
