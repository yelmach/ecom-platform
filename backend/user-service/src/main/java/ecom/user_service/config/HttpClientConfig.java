package ecom.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient mediaServiceRestClient(@Value("${media.service.base-url}") String mediaServiceBaseUrl) {
        return RestClient.builder().baseUrl(mediaServiceBaseUrl).build();
    }
}
