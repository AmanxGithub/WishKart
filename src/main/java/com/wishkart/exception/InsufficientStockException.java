package com.wishkart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientStockException(String productName, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient stock for product '%s'. Requested: %d, Available: %d",
            productName, requestedQuantity, availableQuantity));
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getProductName() {
        return productName;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
