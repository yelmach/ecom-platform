package ecom.product_service.service;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final MediaValidationService mediaValidationService;

    public Page<ProductResponse> getProducts(String keyword, String category, Double minPrice, Double maxPrice,
            String sort,
            int page, int size) {

        validatePrices(minPrice, maxPrice);

        Pageable pageable = PageRequest.of(page, size, ProductSort.fromValue(sort).toSort());
        Page<Product> products = searchProducts(keyword, category, minPrice, maxPrice, pageable);

        return products.map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsBySeller(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, ProductSort.NEWEST.toSort());
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
                .category(requireCategory(request.getCategory()))
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

        if (request.getName() != null) {
            existingProduct.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingProduct.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            existingProduct.setCategory(requireCategory(request.getCategory()));
        }
        if (request.getPrice() != null) {
            existingProduct.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            existingProduct.setQuantity(request.getQuantity());
        }
        if (request.getMediaIds() != null) {
            mediaValidationService.validateProductMediaReferences(id, request.getMediaIds());
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

    private void validatePrices(Double minPrice, Double maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must be greater than or equal to 0");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxPrice must be greater than or equal to 0");
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot be greater than maxPrice");
        }
    }

    private String requireCategory(String category) {
        String checkedCategory = checkedCategory(category);
        if (checkedCategory.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
        return checkedCategory;
    }

    private String checkedCategory(String value) {
        return value == null ? "" : value.trim();
    }

    private Page<Product> searchProducts(String keyword, String category, Double minPrice, Double maxPrice,
            Pageable pageable) {
        String checkedKeyword = checkedCategory(keyword);
        String checkedCategory = checkedCategory(category);

        Double minP = minPrice == null ? 0.0 : minPrice;
        Double maxP = maxPrice == null ? Double.MAX_VALUE : maxPrice;

        if (!checkedKeyword.isBlank() && !checkedCategory.isBlank()) {
            return productRepository.searchByCategoryAndKeyword("^" + Pattern.quote(checkedCategory) + "$",
                    Pattern.quote(checkedKeyword), minP, maxP, pageable);
        }

        if (!checkedKeyword.isBlank()) {
            return productRepository.searchByKeyword(Pattern.quote(checkedKeyword), minP, maxP, pageable);
        }

        if (!checkedCategory.isBlank()) {
            return productRepository.searchByCategory("^" + Pattern.quote(checkedCategory) + "$", minP, maxP, pageable);
        }

        return productRepository.searchByPriceRange(minP, maxP, pageable);
    }
}
