package com.wishkart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    // Shipping Address
    @NotBlank(message = "Shipping name is required")
    private String shippingName;

    @NotBlank(message = "Address line 1 is required")
    private String shippingAddressLine1;

    private String shippingAddressLine2;

    @NotBlank(message = "City is required")
    private String shippingCity;

    @NotBlank(message = "State is required")
    private String shippingState;

    @NotBlank(message = "Postal code is required")
    private String shippingPostalCode;

    @NotBlank(message = "Country is required")
    private String shippingCountry;

    private String shippingPhone;

    // Billing
    private boolean billingSameAsShipping = true;
    private String billingAddressLine1;
    private String billingCity;
    private String billingState;
    private String billingPostalCode;
    private String billingCountry;

    // Payment
    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    private String paymentIntentId;
    private String couponCode;
    private String customerNotes;

    // For saving address
    private boolean saveAddress = false;
    private Long shippingAddressId;
}
