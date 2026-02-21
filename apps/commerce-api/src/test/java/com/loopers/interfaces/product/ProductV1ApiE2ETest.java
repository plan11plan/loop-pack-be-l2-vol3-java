package com.loopers.interfaces.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.product.dto.ProductV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest {

    private static final String ENDPOINT_PRODUCTS = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    private BrandModel savedBrand;

    @Autowired
    public ProductV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        BrandJpaRepository brandJpaRepository,
        ProductJpaRepository productJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @BeforeEach
    void setUp() {
        brandJpaRepository.save(BrandModel.create("나이키"));
        savedBrand = brandJpaRepository.findByNameAndDeletedAtIsNull("나이키").get();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private ProductModel saveProduct(String name, int price, int stock) {
        ProductModel product = ProductModel.create(savedBrand, name, price, stock);
        return productJpaRepository.save(product);
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class List {

        @DisplayName("상품 목록을 조회하면, 페이지네이션된 목록을 반환한다.")
        @Test
        void returnsPaginatedList_whenProductsExist() {
            // arrange
            saveProduct("에어맥스", 150000, 100);
            saveProduct("에어포스", 120000, 50);
            saveProduct("조던1", 200000, 30);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ListResponse>> response =
                testRestTemplate.exchange(ENDPOINT_PRODUCTS + "?page=0&size=2", HttpMethod.GET, null, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(3),
                () -> assertThat(response.getBody().data().totalPages()).isEqualTo(2),
                () -> assertThat(response.getBody().data().items()).hasSize(2)
            );
        }

        @DisplayName("브랜드 ID로 필터링하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        void returnsFilteredList_whenBrandIdIsProvided() {
            // arrange
            brandJpaRepository.save(BrandModel.create("아디다스"));
            BrandModel adidas = brandJpaRepository.findByNameAndDeletedAtIsNull("아디다스").get();

            saveProduct("에어맥스", 150000, 100);
            productJpaRepository.save(ProductModel.create(adidas, "울트라부스트", 180000, 80));

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ListResponse>> response =
                testRestTemplate.exchange(ENDPOINT_PRODUCTS + "?brandId=" + savedBrand.getId(), HttpMethod.GET, null, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(1),
                () -> assertThat(response.getBody().data().items().get(0).name()).isEqualTo("에어맥스")
            );
        }

        @DisplayName("삭제된 상품은 목록에 포함되지 않는다.")
        @Test
        void excludesDeletedProducts() {
            // arrange
            saveProduct("에어맥스", 150000, 100);
            ProductModel toDelete = saveProduct("에어포스", 120000, 50);
            Long deleteId = productJpaRepository.findAllByDeletedAtIsNull(org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent().stream()
                .filter(p -> p.getName().equals("에어포스"))
                .findFirst().get().getId();
            ProductModel found = productJpaRepository.findByIdAndDeletedAtIsNull(deleteId).get();
            found.delete();
            productJpaRepository.save(found);

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ListResponse>> response =
                testRestTemplate.exchange(ENDPOINT_PRODUCTS, HttpMethod.GET, null, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(1),
                () -> assertThat(response.getBody().data().items().get(0).name()).isEqualTo("에어맥스")
            );
        }

        @DisplayName("상품이 없으면, 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoProductsExist() {
            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ListResponse>> response =
                testRestTemplate.exchange(ENDPOINT_PRODUCTS + "?page=0&size=20", HttpMethod.GET, null, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(0),
                () -> assertThat(response.getBody().data().items()).isEmpty()
            );
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetById {

        @DisplayName("존재하는 상품을 조회하면, 상세 정보를 반환한다.")
        @Test
        void returnsProductDetail_whenProductExists() {
            // arrange
            saveProduct("에어맥스", 150000, 100);
            Long productId = productJpaRepository.findAllByDeletedAtIsNull(org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent().get(0).getId();

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.DetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.DetailResponse>> response =
                testRestTemplate.exchange(ENDPOINT_PRODUCTS + "/" + productId, HttpMethod.GET, null, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo(productId),
                () -> assertThat(response.getBody().data().brandId()).isEqualTo(savedBrand.getId()),
                () -> assertThat(response.getBody().data().brandName()).isEqualTo("나이키"),
                () -> assertThat(response.getBody().data().name()).isEqualTo("에어맥스"),
                () -> assertThat(response.getBody().data().price()).isEqualTo(150000),
                () -> assertThat(response.getBody().data().stock()).isEqualTo(100),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(0)
            );
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        void throwsNotFound_whenProductDoesNotExist() {
            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(ENDPOINT_PRODUCTS + "/999", HttpMethod.GET, null, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
