package ecom.product_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import ecom.product_service.dto.request.ProductRequest;
import ecom.product_service.dto.request.ProductUpdateRequest;
import ecom.product_service.dto.response.ProductResponse;
import ecom.product_service.exception.ProductNotFoundException;
import ecom.product_service.exception.ProductOwnershipException;
import ecom.product_service.model.Product;
import ecom.product_service.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaValidationService mediaValidationService;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id("prod-1")
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(89.99)
                .quantity(5)
                .mediaIds(List.of("m1"))
                .sellerId("seller-1")
                .build();
    }

    @Test
    void getProducts_ShouldReturnMappedPage() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        Page<ProductResponse> response = productService.getProducts(0, 10);

        assertEquals(1, response.getTotalElements());
        assertEquals("prod-1", response.getContent().get(0).getId());
        assertEquals("Keyboard", response.getContent().get(0).getName());
    }

    @Test
    void getProductsBySeller_ShouldReturnMappedPage() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findBySellerId(eq("seller-1"), any(Pageable.class))).thenReturn(productPage);

        Page<ProductResponse> response = productService.getProductsBySeller("seller-1", 0, 10);

        assertEquals(1, response.getTotalElements());
        assertEquals("seller-1", response.getContent().get(0).getSellerId());
    }

    @Test
    void getProductById_ShouldReturnProduct() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById("prod-1");

        assertEquals("prod-1", response.getId());
        assertEquals("Keyboard", response.getName());
    }

    @Test
    void getProductById_ShouldThrowWhenNotFound() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById("missing"));
    }

    @Test
    void createProduct_ShouldCreateWithEmptyMediaIds() {
        ProductRequest request = new ProductRequest();
        request.setName("Mouse");
        request.setDescription("Gaming mouse");
        request.setPrice(49.99);
        request.setQuantity(10);

        Product saved = Product.builder()
                .id("prod-2")
                .name("Mouse")
                .description("Gaming mouse")
                .price(49.99)
                .quantity(10)
                .mediaIds(List.of())
                .sellerId("seller-1")
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.createProduct(request, "seller-1");

        assertNotNull(response);
        assertEquals("prod-2", response.getId());
        assertEquals("seller-1", response.getSellerId());
        assertEquals(0, response.getMediaIds().size());
    }

    @Test
    void createProduct_ShouldRejectNonEmptyMediaIds() {
        ProductRequest request = new ProductRequest();
        request.setName("Mouse");
        request.setDescription("Gaming mouse");
        request.setPrice(49.99);
        request.setQuantity(10);
        request.setMediaIds(List.of("media-1"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> productService.createProduct(request, "seller-1"));

        assertEquals(400, ex.getStatusCode().value());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldUpdateFieldsAndMedia() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Keyboard Pro");
        request.setDescription("Updated");
        request.setPrice(99.99);
        request.setQuantity(7);
        request.setMediaIds(List.of("m2", "m3"));

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct("prod-1", request, "seller-1");

        assertEquals("Keyboard Pro", response.getName());
        assertEquals("Updated", response.getDescription());
        assertEquals(99.99, response.getPrice());
        assertEquals(7, response.getQuantity());
        assertEquals(List.of("m2", "m3"), response.getMediaIds());
        verify(mediaValidationService).validateProductMediaReferences("prod-1", List.of("m2", "m3"));
    }

    @Test
    void updateProduct_ShouldThrowWhenNotOwner() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThrows(ProductOwnershipException.class, () -> productService.updateProduct("prod-1", request, "seller-2"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteWhenOwner() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        productService.deleteProduct("prod-1", "seller-1");

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_ShouldThrowWhenNotOwner() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThrows(ProductOwnershipException.class, () -> productService.deleteProduct("prod-1", "seller-2"));
        verify(productRepository, never()).delete(any(Product.class));
    }
}
