package net.isucon;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {
    @Bean
    RestTemplate restTemplate() throws Exception {
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates,
                    String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates,
                    String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new X509TrustManager[] { trustManager }, new SecureRandom());

        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        HostnameVerifier verifier = (h, s) -> true;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection,
                    String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection con = (HttpsURLConnection) connection;
                    con.setSSLSocketFactory(socketFactory);
                    con.setHostnameVerifier(verifier);
                }
                super.prepareConnection(connection, httpMethod);
            }
        };
        return new RestTemplate(factory);
    }
}
