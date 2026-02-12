package ecom.product_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ecom.product_service.dto.request.ProductRequest;
import ecom.product_service.dto.request.ProductUpdateRequest;
import ecom.product_service.dto.response.ProductResponse;
import ecom.product_service.exception.ProductNotFoundException;
import ecom.product_service.exception.ProductOwnershipException;
import ecom.product_service.model.Product;
import ecom.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Page<ProductResponse> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.findAll(pageable);

        return products.map(ProductResponse::fromEntity);
    }

    public ProductResponse getProductById(String id) {
        return ProductResponse.fromEntity(findByIdOrThrow(id));
    }

    public ProductResponse createProduct(ProductRequest request, String sellerId) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .mediaIds(request.getMediaIds())
                .sellerId(sellerId)
                .build();

        return ProductResponse.fromEntity(productRepository.save(product));
    }

    public ProductResponse updateProduct(String id, ProductUpdateRequest request, String sellerId) {
        Product existingProduct = findByIdOrThrow(id);
        checkOwnership(existingProduct, sellerId);

        if (request.getName() != null) {
            existingProduct.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingProduct.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            existingProduct.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            existingProduct.setQuantity(request.getQuantity());
        }
        if (request.getMediaIds() != null) {
            existingProduct.setMediaIds(request.getMediaIds());
        }

        return ProductResponse.fromEntity(productRepository.save(existingProduct));
    }

    public void deleteProduct(String id, String sellerId) {
        Product existingProduct = findByIdOrThrow(id);
        checkOwnership(existingProduct, sellerId);
        productRepository.delete(existingProduct);
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
