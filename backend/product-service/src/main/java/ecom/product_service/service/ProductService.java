package ecom.product_service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import ecom.product_service.dto.request.ProductRequest;
import ecom.product_service.dto.request.ProductUpdateRequest;
import ecom.product_service.dto.response.ProductResponse;
import ecom.product_service.event.ProductEventProducer;
import ecom.product_service.exception.ProductNotFoundException;
import ecom.product_service.exception.ProductOwnershipException;
import ecom.product_service.model.Product;
import ecom.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MediaValidationService mediaValidationService;
    private final MongoTemplate mongoTemplate;
    private final ProductEventProducer productEventProducer;

    public Page<ProductResponse> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findAll(pageable);

        return products.map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsBySeller(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findBySellerId(sellerId, pageable);

        return products.map(ProductResponse::fromEntity);
    }

    public ProductResponse getProductById(String id) {
        return ProductResponse.fromEntity(findByIdOrThrow(id));
    }

    public ProductResponse createProduct(ProductRequest request, String sellerId) {
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "mediaIds must be empty when creating a product");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .mediaIds(new ArrayList<>())
                .sellerId(sellerId)
                .build();

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    public ProductResponse updateProduct(String id, ProductUpdateRequest request, String sellerId) {
        Product existingProduct = findByIdOrThrow(id);
        checkOwnership(existingProduct, sellerId);

        Update update = new Update().set("updatedAt", Instant.now());
        List<String> removedMediaIds = List.of();

        if (request.getName() != null) {
            update.set("name", request.getName());
        }
        if (request.getDescription() != null) {
            update.set("description", request.getDescription());
        }
        if (request.getPrice() != null) {
            update.set("price", request.getPrice());
        }
        if (request.getQuantity() != null) {
            update.set("quantity", request.getQuantity());
        }
        if (request.getMediaIds() != null) {
            mediaValidationService.validateProductMediaReferences(id, request.getMediaIds());
            removedMediaIds = existingProduct.getMediaIds().stream()
                    .filter(mediaId -> !request.getMediaIds().contains(mediaId))
                    .distinct()
                    .toList();
            update.set("mediaIds", request.getMediaIds());
        }

        Query query = Query.query(Criteria.where("id").is(id));

        Product saved = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Product.class);

        if (saved == null) {
            throw new ProductNotFoundException(id);
        }

        if (!removedMediaIds.isEmpty()) {
            productEventProducer.publishProductImagesDetached(id, removedMediaIds);
        }

        return ProductResponse.fromEntity(saved);
    }

    public void deleteProduct(String id, String sellerId) {
        Product existingProduct = findByIdOrThrow(id);
        checkOwnership(existingProduct, sellerId);

        Query query = Query.query(Criteria.where("id").is(id));

        Product deleted = mongoTemplate.findAndRemove(query, Product.class);

        if (deleted == null) {
            throw new ProductNotFoundException(id);
        }

        productEventProducer.publishProductDeleted(deleted.getId());
    }

    public void verifyOwnership(String id, String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id must not be blank");
        }
        checkOwnership(findByIdOrThrow(id), sellerId);
    }

    private Product findByIdOrThrow(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void checkOwnership(Product product, String sellerId) {
        if (!product.getSellerId().equals(sellerId)) {
            throw new ProductOwnershipException();
        }
    }
}
