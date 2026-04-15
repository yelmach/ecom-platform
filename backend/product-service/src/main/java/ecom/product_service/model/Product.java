package ecom.product_service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    @TextIndexed
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @TextIndexed
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 60, message = "Category cannot exceed 60 characters")
    @Indexed
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Indexed
    private Double price;

    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotBlank(message = "Seller ID is required")
    @Indexed
    private String sellerId;

    @Builder.Default
    @NotNull(message = "Media IDs cannot be null")
    private List<@NotBlank(message = "Media ID cannot be blank") String> mediaIds = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
