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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import ecom.product_service.dto.request.ProductRequest;
import ecom.product_service.dto.request.ProductUpdateRequest;
import ecom.product_service.dto.response.ProductResponse;
import ecom.product_service.exception.GlobalExceptionHandler;
import ecom.product_service.service.ProductService;

@WebMvcTest(controllers = ProductController.class)
@ContextConfiguration(classes = { ProductController.class, GlobalExceptionHandler.class })
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
                                .category("Electronics")
                                .price(89.99)
                                .quantity(3)
                                .mediaIds(List.of())
                                .sellerId("seller-1")
                                .build();

                when(productService.getProductById("prod-1")).thenReturn(response);

                mockMvc.perform(get("/products/prod-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("prod-1"))
                                .andExpect(jsonPath("$.name").value("Keyboard"))
                                .andExpect(jsonPath("$.category").value("Electronics"));
        }

        @Test
        void getProducts_ShouldReturnFilteredPage() throws Exception {
                ProductResponse response = ProductResponse.builder()
                                .id("prod-1")
                                .name("Keyboard")
                                .category("Electronics")
                                .price(89.99)
                                .quantity(3)
                                .mediaIds(List.of())
                                .sellerId("seller-1")
                                .build();

                when(productService.getProducts("key", "Electronics", 10.0, 100.0, "priceAsc", 1, 5))
                                .thenReturn(new PageImpl<>(List.of(response)));

                mockMvc.perform(get("/products")
                                .param("keyword", "key")
                                .param("category", "Electronics")
                                .param("minPrice", "10")
                                .param("maxPrice", "100")
                                .param("sort", "priceAsc")
                                .param("page", "1")
                                .param("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].category").value("Electronics"));
        }

        @Test
        void createProduct_ShouldReturn201() throws Exception {
                ProductRequest request = new ProductRequest();
                request.setName("Mouse");
                request.setDescription("Gaming");
                request.setCategory("Accessories");
                request.setPrice(49.99);
                request.setQuantity(5);

                ProductResponse response = ProductResponse.builder()
                                .id("prod-2")
                                .name("Mouse")
                                .description("Gaming")
                                .category("Accessories")
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
                                .andExpect(jsonPath("$.name").value("Mouse"))
                                .andExpect(jsonPath("$.category").value("Accessories"));
        }

        @Test
        void createProduct_ShouldReturn400WhenCategoryMissing() throws Exception {
                ProductRequest request = new ProductRequest();
                request.setName("Mouse");
                request.setDescription("Gaming");
                request.setPrice(49.99);
                request.setQuantity(5);

                mockMvc.perform(post("/products")
                                .header("X-User-Id", "seller-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updateProduct_ShouldReturn200() throws Exception {
                ProductUpdateRequest request = new ProductUpdateRequest();
                request.setName("Keyboard Pro");
                request.setCategory("Accessories");
                request.setPrice(99.99);

                ProductResponse response = ProductResponse.builder()
                                .id("prod-1")
                                .name("Keyboard Pro")
                                .category("Accessories")
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
                                .andExpect(jsonPath("$.name").value("Keyboard Pro"))
                                .andExpect(jsonPath("$.category").value("Accessories"));
        }

        @Test
        void updateProduct_ShouldReturn400WhenCategoryIsBlank() throws Exception {
                ProductUpdateRequest request = new ProductUpdateRequest();
                request.setCategory("   ");

                mockMvc.perform(put("/products/prod-1")
                                .header("X-User-Id", "seller-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void deleteProduct_ShouldReturn204() throws Exception {
                mockMvc.perform(delete("/products/prod-1")
                                .header("X-User-Id", "seller-1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getProducts_ShouldReturn400WhenMinPriceIsNegative() throws Exception {
                mockMvc.perform(get("/products")
                                .param("minPrice", "-1"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getProducts_ShouldReturn400WhenSortIsInvalid() throws Exception {
                when(productService.getProducts("", "", null, null, "wrong", 0, 10))
                                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort value: wrong"));

                mockMvc.perform(get("/products")
                                .param("sort", "wrong"))
                                .andExpect(status().isBadRequest());
        }
}
