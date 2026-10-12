package ecom.product_service.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ecom.product_service.exception.GlobalExceptionHandler;
import ecom.product_service.model.Product;
import ecom.product_service.repository.ProductRepository;
import ecom.product_service.service.MediaValidationService;
import ecom.product_service.service.ProductService;

class InternalProductControllerTest {
    private ProductRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        ProductService service = new ProductService(repository, mock(MediaValidationService.class), mock(org.springframework.data.mongodb.core.MongoTemplate.class), mock(ecom.product_service.event.ProductEventProducer.class));
        mvc = MockMvcBuilders.standaloneSetup(new InternalProductController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void ownerReceives204AndOtherSellerReceives403() throws Exception {
        when(repository.findById("prod-1")).thenReturn(Optional.of(
                Product.builder().id("prod-1").sellerId("seller-1").build()));
        mvc.perform(get("/internal/products/prod-1/ownership").header("X-User-Id", "seller-1"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/internal/products/prod-1/ownership").header("X-User-Id", "seller-2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingProductReceives404() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        mvc.perform(get("/internal/products/missing/ownership").header("X-User-Id", "seller-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingOrBlankIdentityReceives400() throws Exception {
        mvc.perform(get("/internal/products/prod-1/ownership")).andExpect(status().isBadRequest());
        mvc.perform(get("/internal/products/prod-1/ownership").header("X-User-Id", " "))
                .andExpect(status().isBadRequest());
    }
}
