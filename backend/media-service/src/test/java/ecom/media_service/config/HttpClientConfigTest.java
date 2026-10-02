package ecom.media_service.config;

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

import ecom.media_service.service.ProductOwnershipService;

class HttpClientConfigTest {
    @Test
    void resolvesDiscoveredHostAndPortAndReturns503WhenNoInstancesRemain() throws Exception {
        HttpServer product = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> identity = new AtomicReference<>();
        product.createContext("/internal/products/prod-1/ownership", exchange -> {
            calls.incrementAndGet();
            identity.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        product.start();
        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>(List.of(
                new DefaultServiceInstance("product-1", "product-service", "127.0.0.1",
                        product.getAddress().getPort(), false)));

        DiscoveryClient discovery = new DiscoveryClient() {
            @Override
            public String description() {
                return "Test registry";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return "product-service".equals(serviceId) ? instances.get() : List.of();
            }

            @Override
            public List<String> getServices() {
                return List.of("product-service");
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
                        ProductOwnershipService service = new ProductOwnershipService(
                                context.getBean("productServiceRestClient", RestClient.class));
                        service.verifyOwnership("prod-1", "seller-1");
                        assertThat(calls.get()).isEqualTo(1);
                        assertThat(identity.get()).isEqualTo("seller-1");

                        instances.set(List.of());
                        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                                () -> service.verifyOwnership("prod-1", "seller-1"));
                        assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                        assertThat(calls.get()).isEqualTo(1);
                    });
        } finally {
            product.stop(0);
        }
    }
}
