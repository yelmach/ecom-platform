package ecom.product_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import ecom.product_service.service.MediaValidationService;

class HttpClientConfigTest {
    @Test
    void resolvesDiscoveredHostAndPortAndReturns503WhenNoInstancesRemain() throws Exception {
        HttpServer product = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> identity = new AtomicReference<>();
        product.createContext("/media/images/prod-1", exchange -> {
            calls.incrementAndGet();
            identity.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
            byte[] body = "{\"productId\":\"prod-1\",\"images\":[{\"id\":\"m1\",\"url\":\"u1\"}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        AtomicInteger directRequests = new AtomicInteger();
        product.createContext("/eureka/apps", exchange -> {
            directRequests.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        product.start();
        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>(List.of(
                new DefaultServiceInstance("media-1", "media-service", "127.0.0.1",
                        product.getAddress().getPort(), false)));

        DiscoveryClient discovery = new DiscoveryClient() {
            @Override
            public String description() {
                return "Test registry";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return "media-service".equals(serviceId) ? instances.get() : List.of();
            }

            @Override
            public List<String> getServices() {
                return List.of("media-service");
            }
        };

        try {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class,
                            org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration.class,
                            BlockingLoadBalancerClientAutoConfiguration.class))
                    .withUserConfiguration(HttpClientConfig.class)
                    .withPropertyValues("spring.cloud.loadbalancer.cache.enabled=false",
                            "spring.cloud.loadbalancer.retry.enabled=false")
                    .withBean(DiscoveryClient.class, () -> discovery)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        // An unqualified builder (as used by Eureka) must bypass discovery.
                        RestClient.Builder directBuilder = context.getBean(RestClient.Builder.class);
                        assertThat(directBuilder).isSameAs(context.getBean("restClientBuilder"));
                        directBuilder.clone().build().get()
                                .uri("http://127.0.0.1:" + product.getAddress().getPort() + "/eureka/apps")
                                .retrieve().toBodilessEntity();
                        assertThat(directRequests.get()).isEqualTo(1);

                        MediaValidationService service = new MediaValidationService(
                                context.getBean("mediaServiceRestClient", RestClient.class));
                        service.validateProductMediaReferences("prod-1", List.of("m1"));
                        assertThat(calls.get()).isEqualTo(1);


                        instances.set(List.of());
                        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                                () -> service.validateProductMediaReferences("prod-1", List.of("m1")));
                        assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                        assertThat(calls.get()).isEqualTo(1);
                    });
        } finally {
            product.stop(0);
        }
    }
}
