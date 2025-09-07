package com.example.shopBackend.service;

import com.example.shopBackend.dto.ProductDto;
import com.example.shopBackend.dto.Response;
import com.example.shopBackend.entity.Category;
import com.example.shopBackend.entity.Product;
import com.example.shopBackend.enums.UserRole;
import com.example.shopBackend.exceptions.NotFoundException;
import com.example.shopBackend.security.XssSanitizer;
import jakarta.validation.ValidationException;

import com.example.shopBackend.mapper.EntityDtoMapper;
import com.example.shopBackend.repository.CategoryRepo;
import com.example.shopBackend.repository.ProductRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    @Autowired
    private final ProductRepo productRepo;
    @Autowired
    private final CategoryRepo categoryRepo;
    @Autowired
    private final EntityDtoMapper entityDtoMapper;
    @Autowired
    private final UserService userService;
    @Autowired
    private final XssSanitizer xssSanitizer;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXT = {"jpg", "jpeg", "png", "webp"};
    private static final String UPLOAD_DIR = "uploads/images/";

    private String saveFileLocally(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ValidationException("Image file is required");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ValidationException("File size exceeds the allowed limit of 5MB");
            }

            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            if (ext == null || ext.isBlank()) {
                throw new ValidationException("File extension could not be determined");
            }
            ext = ext.toLowerCase();
            boolean allowed = false;
            for (String a : ALLOWED_EXT) {
                if (a.equals(ext)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new ValidationException("Invalid file type");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new ValidationException("Invalid content type");
            }

            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // sanitize filename and prevent path traversal
            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String safeFilename = System.currentTimeMillis() + "_" + Paths.get(original).getFileName().toString();
            Path filePath = uploadPath.resolve(safeFilename).normalize();

            // defensive check: ensure file path is indeed under uploadPath
            if (!filePath.startsWith(uploadPath)) {
                throw new ValidationException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return the URL to access this file — use a safe mapping in your controller
            return "/files/" + safeFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + (file != null ? file.getOriginalFilename() : "unknown"), e);
        }
    }

    private void validateProductInputs(String name, String description, BigDecimal price) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Product name is required");
        }
        if (name.length() > 200) {
            throw new ValidationException("Product name too long");
        }
        if (description != null && description.length() > 2000) {
            throw new ValidationException("Description too long");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be greater than zero");
        }
    }

    @Override
    public Response createProduct(Long categoryId, MultipartFile image, String name, String description, BigDecimal price) {
        // Only admins may create products
        var user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized");
        }

        validateProductInputs(name, description, price);

        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found"));

        Product product = new Product();
        product.setCategory(category);
        product.setPrice(price);
        product.setName(xssSanitizer.sanitize(name));
        product.setDescription(xssSanitizer.sanitize(description));
        String fileUrl = saveFileLocally(image);
        product.setImageUrl(fileUrl);

        productRepo.save(product);
        return Response.builder()
                .status(200)
                .message("Product successfully created")
                .build();
    }

    @Override
    public Response updateProduct(Long productId, Long categoryId, MultipartFile image, String name, String description, BigDecimal price) {
        // Only admins may update products
        var user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized");
        }

        Product product = productRepo.findById(productId).orElseThrow(() -> new NotFoundException("Product Not Found"));

        Category category = null;
        String productImageUrl = null;

        if (categoryId != null) {
            category = categoryRepo.findById(categoryId).orElseThrow(() -> new NotFoundException("Category not found"));
        }
        if (image != null && !image.isEmpty()) {
            productImageUrl = saveFileLocally(image);
        }

        if (category != null) product.setCategory(category);
        if (name != null) {
            if (name.length() > 200) throw new ValidationException("Product name too long");
            product.setName(xssSanitizer.sanitize(name));
        }
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) <= 0) throw new ValidationException("Price must be > 0");
            product.setPrice(price);
        }
        if (description != null) {
            if (description.length() > 2000) throw new ValidationException("Description too long");
            product.setDescription(xssSanitizer.sanitize(description));
        }
        if (productImageUrl != null) product.setImageUrl(productImageUrl);

        productRepo.save(product);
        return Response.builder()
                .status(200)
                .message("Product updated successfully")
                .build();

    }

    @Override
    public Response deleteProduct(Long productId) {
        // Only admins may delete products
        var user = userService.getLoginUser();
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new ValidationException("Unauthorized");
        }

        Product product = productRepo.findById(productId).orElseThrow(() -> new NotFoundException("Product Not Found"));
        productRepo.delete(product);

        return Response.builder()
                .status(200)
                .message("Product deleted successfully")
                .build();
    }

    @Override
    public Response getProductById(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow(() -> new NotFoundException("Product Not Found"));
        ProductDto productDto = entityDtoMapper.mapProductToDtoBasic(product);

        return Response.builder()
                .status(200)
                .product(productDto)
                .build();
    }

    @Override
    public Response getAllProducts() {
        List<ProductDto> productList = productRepo.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(entityDtoMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .productList(productList)
                .build();

    }

    @Override
    public Response getProductsByCategory(Long categoryId) {
        List<Product> products = productRepo.findByCategoryId(categoryId);
        if (products.isEmpty()) {
            throw new NotFoundException("No Products found for this category");
        }
        List<ProductDto> productDtoList = products.stream()
                .map(entityDtoMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .productList(productDtoList)
                .build();

    }

    @Override
    public Response searchProduct(String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            throw new ValidationException("Search value required");
        }
        String safeSearch = xssSanitizer.sanitize(searchValue.trim());
        List<Product> products = productRepo.findByNameContainingOrDescriptionContaining(safeSearch, safeSearch);

        if (products.isEmpty()) {
            throw new NotFoundException("No Products Found");
        }
        List<ProductDto> productDtoList = products.stream()
                .map(entityDtoMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());


        return Response.builder()
                .status(200)
                .productList(productDtoList)
                .build();
    }
}
