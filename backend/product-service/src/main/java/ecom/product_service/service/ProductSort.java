package ecom.product_service.service;

import java.util.Arrays;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ProductSort {
    NEWEST("newest", Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC("priceAsc", Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC("priceDesc", Sort.by(Sort.Direction.DESC, "price")),
    NAME_ASC("nameAsc", Sort.by(Sort.Direction.ASC, "name"));

    private final String value;
    private final Sort sort;

    ProductSort(String value, Sort sort) {
        this.value = value;
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }

    public static ProductSort fromValue(String value) {
        return Arrays.stream(values())
                .filter(candidate -> candidate.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort value: " + value));
    }
}
