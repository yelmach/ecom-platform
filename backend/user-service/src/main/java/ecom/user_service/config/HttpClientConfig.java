package ecom.user_service.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        // Eureka must contact its configured server directly, without discovery.
        return newRestClientBuilder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return newRestClientBuilder();
    }

    private RestClient.Builder newRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    public RestClient mediaServiceRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.clone().baseUrl("http://media-service").build();
    }
}
