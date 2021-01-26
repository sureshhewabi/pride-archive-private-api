package uk.ac.ebi.pride.ws.pride.configs;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Component
public class SubmissionApiConfig {

    @Value("${submission-api.login-url}")
    @Getter
    private String loginUrl;

    @Value("${submission-api.publication-url}")
    @Getter
    private String publicationUrl;

    @Value("${submission-api.user}")
    private String user;

    @Value("${submission-api.password}")
    private String password;

    @Value("${proxy-host}")
    private String proxyHost;

    @Value("${proxy-port}")
    private Integer proxyPort;

    @Bean("proxyRestTemplate")
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (proxyHost != null && proxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            requestFactory.setProxy(proxy);
        }
        return new RestTemplate(requestFactory);
    }

    public Credentials getCredentials() {
        Credentials credentials = new Credentials();
        credentials.password = password;
        credentials.username = user;
        return credentials;
    }

    @Data
    public static class Credentials {
        String username;
        String password;
    }
}
