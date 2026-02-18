package com.wishkart.grpc.service;

import com.wishkart.dto.CategoryDTO;
import com.wishkart.grpc.category.*;
import com.wishkart.grpc.util.GrpcExceptionHandler;
import com.wishkart.grpc.util.ProtoConverter;
import com.wishkart.service.CategoryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * gRPC service implementation for Categories.
 * Delegates to existing CategoryService for business logic.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcCategoryServiceImpl extends CategoryServiceGrpc.CategoryServiceImplBase {

    private final CategoryService categoryService;

    @Override
    public void getAllCategories(Empty request,
                                 StreamObserver<CategoryListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetAllCategories request");

            List<CategoryDTO> categories = categoryService.getAllCategories();

            CategoryListResponse.Builder builder = CategoryListResponse.newBuilder()
                .setSuccess(true);
            categories.forEach(c -> builder.addCategories(ProtoConverter.toProto(c)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getRootCategories(Empty request,
                                  StreamObserver<CategoryListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetRootCategories request");

            List<CategoryDTO> categories = categoryService.getRootCategories();

            CategoryListResponse.Builder builder = CategoryListResponse.newBuilder()
                .setSuccess(true);
            categories.forEach(c -> builder.addCategories(ProtoConverter.toProto(c)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getCategoryById(CategoryIdRequest request,
                                StreamObserver<CategoryResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetCategoryById request for id: {}", request.getId());

            CategoryDTO category = categoryService.getCategoryById(request.getId());

            CategoryResponse response = ProtoConverter.toCategoryResponse(category,
                "Category retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getCategoryBySlug(CategorySlugRequest request,
                                  StreamObserver<CategoryResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetCategoryBySlug request for slug: {}", request.getSlug());

            CategoryDTO category = categoryService.getCategoryBySlug(request.getSlug());

            CategoryResponse response = ProtoConverter.toCategoryResponse(category,
                "Category retrieved successfully");
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }, responseObserver);
    }

    @Override
    public void getSubcategories(CategoryIdRequest request,
                                 StreamObserver<CategoryListResponse> responseObserver) {
        GrpcExceptionHandler.handle(() -> {
            log.debug("gRPC GetSubcategories request for parentId: {}", request.getId());

            List<CategoryDTO> categories = categoryService.getSubcategories(request.getId());

            CategoryListResponse.Builder builder = CategoryListResponse.newBuilder()
                .setSuccess(true);
            categories.forEach(c -> builder.addCategories(ProtoConverter.toProto(c)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }, responseObserver);
    }
}
