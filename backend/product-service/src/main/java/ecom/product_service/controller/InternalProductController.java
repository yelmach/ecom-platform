package ecom.product_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ecom.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {
    private final ProductService productService;

    @GetMapping("/{productId}/ownership")
    public ResponseEntity<Void> verifyOwnership(@PathVariable String productId,
            @RequestHeader("X-User-Id") String userId) {
        productService.verifyOwnership(productId, userId);
        return ResponseEntity.noContent().build();
    }
}
