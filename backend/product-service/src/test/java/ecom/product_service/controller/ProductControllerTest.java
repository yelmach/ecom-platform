package ecom.product_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ecom.product_service.dto.request.ProductRequest;
import ecom.product_service.dto.request.ProductUpdateRequest;
import ecom.product_service.dto.response.ProductResponse;
import ecom.product_service.service.ProductService;

@WebMvcTest(controllers = ProductController.class)
@ContextConfiguration(classes = ProductController.class)
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ProductService productService;

        @Test
        void getProductById_ShouldReturn200() throws Exception {
                ProductResponse response = ProductResponse.builder()
                                .id("prod-1")
                                .name("Keyboard")
                                .price(89.99)
                                .quantity(3)
                                .mediaIds(List.of())
                                .sellerId("seller-1")
                                .build();

                when(productService.getProductById("prod-1")).thenReturn(response);

                mockMvc.perform(get("/products/prod-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("prod-1"))
                                .andExpect(jsonPath("$.name").value("Keyboard"));
        }

        @Test
        void createProduct_ShouldReturn201() throws Exception {
                ProductRequest request = new ProductRequest();
                request.setName("Mouse");
                request.setDescription("Gaming");
                request.setPrice(49.99);
                request.setQuantity(5);

                ProductResponse response = ProductResponse.builder()
                                .id("prod-2")
                                .name("Mouse")
                                .description("Gaming")
                                .price(49.99)
                                .quantity(5)
                                .mediaIds(List.of())
                                .sellerId("seller-1")
                                .build();

                when(productService.createProduct(any(ProductRequest.class), eq("seller-1"))).thenReturn(response);

                mockMvc.perform(post("/products")
                                .header("X-User-Id", "seller-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("prod-2"))
                                .andExpect(jsonPath("$.name").value("Mouse"));
        }

        @Test
        void updateProduct_ShouldReturn200() throws Exception {
                ProductUpdateRequest request = new ProductUpdateRequest();
                request.setName("Keyboard Pro");
                request.setPrice(99.99);

                ProductResponse response = ProductResponse.builder()
                                .id("prod-1")
                                .name("Keyboard Pro")
                                .price(99.99)
                                .quantity(3)
                                .mediaIds(List.of())
                                .sellerId("seller-1")
                                .build();

                when(productService.updateProduct(eq("prod-1"), any(ProductUpdateRequest.class), eq("seller-1")))
                                .thenReturn(response);

                mockMvc.perform(put("/products/prod-1")
                                .header("X-User-Id", "seller-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Keyboard Pro"));
        }

        @Test
        void deleteProduct_ShouldReturn204() throws Exception {
                mockMvc.perform(delete("/products/prod-1")
                                .header("X-User-Id", "seller-1"))
                                .andExpect(status().isNoContent());
        }
}
