package com.example.shopBackend.service;

import com.example.shopBackend.dto.CategoryDto;
import com.example.shopBackend.dto.Response;
import com.example.shopBackend.entity.Category;
import com.example.shopBackend.enums.UserRole;
import com.example.shopBackend.exceptions.NotFoundException;
import jakarta.validation.ValidationException;

import com.example.shopBackend.mapper.EntityDtoMapper;
import com.example.shopBackend.repository.CategoryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private final CategoryRepo categoryRepo;
    @Autowired
    private final EntityDtoMapper entityDtoMapper;
    @Autowired
    private final UserService userService;

    private static final int MAX_NAME_LENGTH = 100;

    @Override
    public Response createCategory(CategoryDto categoryRequest) {

        var user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized");
        }

        validateCategoryRequest(categoryRequest);

        Category category = new Category();
        category.setName(StringUtils.trimWhitespace(categoryRequest.getName()));
        categoryRepo.save(category);
        return Response.builder()
                .status(200)
                .message("Category created successfully")
                .build();
    }

    @Override
    public Response updateCategory(Long categoryId, CategoryDto categoryRequest) {

        var user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized");
        }

        validateCategoryRequest(categoryRequest);

        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new NotFoundException("Category Not Found"));
        category.setName(StringUtils.trimWhitespace(categoryRequest.getName()));
        categoryRepo.save(category);
        return Response.builder()
                .status(200)
                .message("category updated successfully")
                .build();
    }

    @Override
    public Response getAllCategories() {
        List<Category> categories = categoryRepo.findAll();
        List<CategoryDto> categoryDtoList = categories.stream()
                .map(entityDtoMapper::mapCategoryToDtoBasic)
                .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .categoryList(categoryDtoList)
                .build();
    }

    @Override
    public Response getCategoryById(Long categoryId) {
        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new NotFoundException("Category Not Found"));
        CategoryDto categoryDto = entityDtoMapper.mapCategoryToDtoBasic(category);
        return Response.builder()
                .status(200)
                .category(categoryDto)
                .build();
    }

    @Override
    public Response deleteCategory(Long categoryId) {

        var user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized");
        }

        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new NotFoundException("Category Not Found"));
        categoryRepo.delete(category);
        return Response.builder()
                .status(200)
                .message("Category was deleted successfully")
                .build();
    }

    private void validateCategoryRequest(CategoryDto request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Category name is required");
        }
        if (request.getName().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Category name too long");
        }
    }
}
