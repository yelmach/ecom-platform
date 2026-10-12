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

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Mock
    private ecom.product_service.event.ProductEventProducer productEventProducer;

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
        when(mongoTemplate.findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class), eq(Product.class)))
                .thenAnswer(invocation -> {
                    var changes = ((org.springframework.data.mongodb.core.query.Update) invocation.getArgument(1))
                            .getUpdateObject().get("$set", org.bson.Document.class);
                    product.setName(changes.getString("name"));
                    product.setDescription(changes.getString("description"));
                    product.setPrice(changes.getDouble("price"));
                    product.setQuantity(changes.getInteger("quantity"));
                    product.setMediaIds(changes.getList("mediaIds", String.class));
                    return product;
                });

        ProductResponse response = productService.updateProduct("prod-1", request, "seller-1");

        assertEquals("Keyboard Pro", response.getName());
        assertEquals("Updated", response.getDescription());
        assertEquals(99.99, response.getPrice());
        assertEquals(7, response.getQuantity());
        assertEquals(List.of("m2", "m3"), response.getMediaIds());
        verify(mediaValidationService).validateProductMediaReferences("prod-1", List.of("m2", "m3"));
        verify(productEventProducer).publishProductImagesDetached("prod-1", List.of("m1"));
    }

    @Test
    void clearingImagesPublishesAllRemovedIdsAfterUpdate() {
        product.setMediaIds(List.of("m1", "m2"));
        var request = new ProductUpdateRequest();
        request.setMediaIds(List.of());
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(mongoTemplate.findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class), eq(Product.class)))
                .thenReturn(product);
        productService.updateProduct("prod-1", request, "seller-1");
        var order = org.mockito.Mockito.inOrder(mongoTemplate, productEventProducer);
        order.verify(mongoTemplate).findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class), eq(Product.class));
        order.verify(productEventProducer).publishProductImagesDetached("prod-1", List.of("m1", "m2"));
    }

    @Test
    void keepingReorderingOrAddingImagesDoesNotPublishCleanup() {
        product.setMediaIds(List.of("m1", "m2"));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(mongoTemplate.findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class), eq(Product.class)))
                .thenReturn(product);
        for (var images : List.of(List.of("m1", "m2"), List.of("m2", "m1"), List.of("m1", "m2", "m3"))) {
            var request = new ProductUpdateRequest();
            request.setMediaIds(images);
            productService.updateProduct("prod-1", request, "seller-1");
        }
        productService.updateProduct("prod-1", new ProductUpdateRequest(), "seller-1");
        org.mockito.Mockito.verifyNoInteractions(productEventProducer);
    }

    @Test
    void failedImageUpdateDoesNotPublishCleanup() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        var request = new ProductUpdateRequest();
        request.setMediaIds(List.of());
        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct("prod-1", request, "seller-1"));
        org.mockito.Mockito.verifyNoInteractions(productEventProducer);
    }

    @Test
    void updateProduct_ShouldThrowWhenNotOwner() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThrows(ProductOwnershipException.class,
                () -> productService.updateProduct("prod-1", request, "seller-2"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteWhenOwner() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        when(mongoTemplate.findAndRemove(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(Product.class))).thenReturn(product);

        productService.deleteProduct("prod-1", "seller-1");

        var order = org.mockito.Mockito.inOrder(mongoTemplate, productEventProducer);
        var query = org.mockito.ArgumentCaptor.forClass(org.springframework.data.mongodb.core.query.Query.class);
        order.verify(mongoTemplate).findAndRemove(query.capture(), eq(Product.class));
        assertEquals("prod-1", query.getValue().getQueryObject().getString("id"));
        order.verify(productEventProducer).publishProductDeleted("prod-1");
    }

    @Test
    void deleteProduct_ShouldThrowWhenNotOwner() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThrows(ProductOwnershipException.class, () -> productService.deleteProduct("prod-1", "seller-2"));
        org.mockito.Mockito.verifyNoInteractions(mongoTemplate, productEventProducer);
    }

    @Test
    void concurrentDeletionCannotBeUndoneByStaleUpdate() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        var request = new ProductUpdateRequest();
        request.setName("New name");
        // The product was deleted after it was read; the conditional update matches
        // nothing.
        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct("prod-1", request, "seller-1"));
        var query = org.mockito.ArgumentCaptor.forClass(org.springframework.data.mongodb.core.query.Query.class);
        verify(mongoTemplate).findAndModify(query.capture(),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class), eq(Product.class));
        assertEquals("prod-1", query.getValue().getQueryObject().getString("id"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void missingProductCannotProduceDeletionEvent() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct("missing", "seller-1"));
        org.mockito.Mockito.verifyNoInteractions(mongoTemplate, productEventProducer);
    }

    @Test
    void databaseDeletionFailureDoesNotPublish() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        org.mockito.Mockito.doThrow(new IllegalStateException("database offline"))
                .when(mongoTemplate)
                .findAndRemove(any(org.springframework.data.mongodb.core.query.Query.class), eq(Product.class));
        assertThrows(IllegalStateException.class, () -> productService.deleteProduct("prod-1", "seller-1"));
        org.mockito.Mockito.verifyNoInteractions(productEventProducer);
    }

    @Test
    void concurrentDeletionDoesNotPublishAnExtraEvent() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct("prod-1", "seller-1"));
        org.mockito.Mockito.verifyNoInteractions(productEventProducer);
    }

}
