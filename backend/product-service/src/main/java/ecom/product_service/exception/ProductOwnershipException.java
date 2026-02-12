package ecom.product_service.exception;

public class ProductOwnershipException extends RuntimeException {
    public ProductOwnershipException() {
        super("You can only manage your own products");
    }
}
