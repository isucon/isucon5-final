package net.isucon;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttpClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.squareup.okhttp.OkHttpClient;

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

        OkHttpClient client = new OkHttpClient();
        client.setSslSocketFactory(socketFactory);
        client.setHostnameVerifier(verifier);
        return new RestTemplate(new OkHttpClientHttpRequestFactory(client));
    }
}
