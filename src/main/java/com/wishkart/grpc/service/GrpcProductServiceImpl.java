package com.wishkart.grpc.service;

import com.wishkart.dto.ProductDTO;
import com.wishkart.grpc.product.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.ProductService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

/**
 * gRPC service implementation for Products.
 * Delegates to existing ProductService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcProductServiceImpl extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductService productService;

    @Override
    public void getAllProducts(GetProductsRequest request,
                               StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetAllProducts request");

            PageRequest pageRequest = buildPageRequest(request.getPagination());
            Page<ProductDTO> products = productService.getAllProducts(pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getProductById(ProductIdRequest request,
                               StreamObserver<ProductResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetProductById request for id: {}", request.getId());

            ProductDTO product = productService.getProductById(request.getId());

            ProductResponse response = ProtoConverter.toProductResponse(product, "Product retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getProductBySlug(ProductSlugRequest request,
                                 StreamObserver<ProductResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetProductBySlug request for slug: {}", request.getSlug());

            ProductDTO product = productService.getProductBySlug(request.getSlug());

            ProductResponse response = ProtoConverter.toProductResponse(product, "Product retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getProductsByCategory(GetProductsByCategoryRequest request,
                                      StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetProductsByCategory request for categoryId: {}", request.getCategoryId());

            PageRequest pageRequest = buildPageRequest(request.getPagination());
            Page<ProductDTO> products = productService.getProductsByCategory(request.getCategoryId(), pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void searchProducts(SearchProductsRequest request,
                               StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC SearchProducts request for query: {}", request.getQuery());

            PageRequest pageRequest = buildPageRequest(request.getPagination());
            Page<ProductDTO> products = productService.searchProducts(request.getQuery(), pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getProductsByPriceRange(PriceRangeRequest request,
                                        StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetProductsByPriceRange request: {} - {}",
                request.getMinPrice(), request.getMaxPrice());

            BigDecimal minPrice = ProtoConverter.stringToBigDecimal(request.getMinPrice());
            BigDecimal maxPrice = ProtoConverter.stringToBigDecimal(request.getMaxPrice());
            PageRequest pageRequest = buildPageRequest(request.getPagination());

            Page<ProductDTO> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getFeaturedProducts(PaginationRequest request,
                                    StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetFeaturedProducts request");

            PageRequest pageRequest = buildPageRequest(request);
            Page<ProductDTO> products = productService.getFeaturedProducts(pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getNewArrivals(PaginationRequest request,
                               StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetNewArrivals request");

            PageRequest pageRequest = buildPageRequest(request);
            Page<ProductDTO> products = productService.getNewArrivals(pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getBestSellers(PaginationRequest request,
                               StreamObserver<ProductListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetBestSellers request");

            PageRequest pageRequest = buildPageRequest(request);
            Page<ProductDTO> products = productService.getBestSellers(pageRequest);

            ProductListResponse response = buildProductListResponse(products);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    // Helper methods

    private PageRequest buildPageRequest(PaginationRequest pagination) {
        if (pagination == null) {
            return PageRequest.of(0, 12);
        }

        int page = pagination.getPage() >= 0 ? pagination.getPage() : 0;
        int size = pagination.getSize() > 0 ? pagination.getSize() : 12;

        if (pagination.getSortBy() != null && !pagination.getSortBy().isEmpty()) {
            Sort sort = "desc".equalsIgnoreCase(pagination.getSortDirection())
                ? Sort.by(pagination.getSortBy()).descending()
                : Sort.by(pagination.getSortBy()).ascending();
            return PageRequest.of(page, size, sort);
        }

        return PageRequest.of(page, size);
    }

    private ProductListResponse buildProductListResponse(Page<ProductDTO> products) {
        ProductListResponse.Builder builder = ProductListResponse.newBuilder()
            .setSuccess(true)
            .setPagination(ProtoConverter.toProductPaginationInfo(products));

        products.getContent().forEach(p -> builder.addProducts(ProtoConverter.toProto(p)));

        return builder.build();
    }
}
