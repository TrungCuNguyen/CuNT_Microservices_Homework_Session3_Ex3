package com.example.productservice.controller;

import com.example.productservice.dto.ProductRequestDTO;
import com.example.productservice.exception.GlobalExceptionHandler;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProductControllerTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductController productController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/products with price = -500 should return 400 Bad Request with ApiResponseError and not save to DB")
    void testCreateProduct_NegativePrice_Returns400ApiResponseError() throws Exception {
        String invalidPayload = "{\"name\":\"Bàn phím cơ\",\"price\":-500,\"stockQuantity\":10}";

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Giá sản phẩm phải lớn hơn 0")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        // Kiểm tra chắc chắn DB không được lưu bản ghi này
        assertEquals(0, productRepository.count());
    }

    @Test
    @DisplayName("POST /api/v1/products with blank name should return 400 Bad Request")
    void testCreateProduct_BlankName_Returns400() throws Exception {
        String invalidPayload = "{\"name\":\"\",\"price\":150000,\"stockQuantity\":5}";

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Tên sản phẩm không được để trống")));

        assertEquals(0, productRepository.count());
    }

    @Test
    @DisplayName("POST /api/v1/products with negative stockQuantity should return 400 Bad Request")
    void testCreateProduct_NegativeStock_Returns400() throws Exception {
        String invalidPayload = "{\"name\":\"Chuột không dây\",\"price\":250000,\"stockQuantity\":-1}";

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Số lượng tồn kho không được âm")));

        assertEquals(0, productRepository.count());
    }

    @Test
    @DisplayName("POST /api/v1/products with valid data should return 201 Created and save to DB")
    void testCreateProduct_ValidData_Returns201() throws Exception {
        String validPayload = "{\"name\":\"Tai nghe Sony WH-1000XM5\",\"price\":6500000,\"stockQuantity\":15}";

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Tai nghe Sony WH-1000XM5")))
                .andExpect(jsonPath("$.price", is(6500000.0)))
                .andExpect(jsonPath("$.stockQuantity", is(15)));

        assertEquals(1, productRepository.count());
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} should return 200 with product details or 404 when not found")
    void testGetProductById() throws Exception {
        // Tạo sản phẩm
        var responseDTO = productService.createProduct(ProductRequestDTO.builder()
                .name("Màn hình Dell Ultrasharp")
                .price(8500000.0)
                .stockQuantity(8)
                .build());

        Long id = responseDTO.getId();

        // Lấy chi tiết với ID hợp lệ
        mockMvc.perform(get("/api/v1/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.name", is("Màn hình Dell Ultrasharp")))
                .andExpect(jsonPath("$.price", is(8500000.0)))
                .andExpect(jsonPath("$.stockQuantity", is(8)));

        // Lấy với ID không tồn tại
        mockMvc.perform(get("/api/v1/products/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Không tìm thấy sản phẩm với id: 99999")));
    }

    @Test
    @DisplayName("GET /api/v1/products should return list of all products")
    void testGetAllProducts() throws Exception {
        productService.createProduct(ProductRequestDTO.builder()
                .name("Sản phẩm A")
                .price(100000.0)
                .stockQuantity(10)
                .build());

        productService.createProduct(ProductRequestDTO.builder()
                .name("Sản phẩm B")
                .price(200000.0)
                .stockQuantity(20)
                .build());

        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Sản phẩm A")))
                .andExpect(jsonPath("$[1].name", is("Sản phẩm B")));
    }
}
