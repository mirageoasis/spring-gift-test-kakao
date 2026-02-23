package gift.ui;

import gift.model.CategoryRepository;
import gift.model.OptionRepository;
import gift.model.ProductRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 카테고리는 API 호출로 준비
        categoryId = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "테스트 카테고리"))
            .when()
                .post("/api/categories")
            .then()
                .extract().jsonPath().getLong("id");
    }

    @Nested
    @DisplayName("POST /api/products: 상품 생성")
    class Create {

        @Test
        void 상품을_생성하면_200을_반환한다() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "커피", "price", 5000,
                        "imageUrl", "http://image.url", "categoryId", categoryId))
            .when()
                .post("/api/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("커피"))
                .body("price", equalTo(5000));
        }

        @Test
        void 상품을_생성하면_조회_시_포함된다() {
            // given
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "커피", "price", 5000,
                        "imageUrl", "http://image.url", "categoryId", categoryId))
            .when()
                .post("/api/products");

            // when & then
            given()
            .when()
                .get("/api/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", hasItem("커피"));
        }

        @Test
        void 존재하지_않는_카테고리로_상품을_생성하면_500_에러() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "커피", "price", 5000,
                        "imageUrl", "http://image.url", "categoryId", 99999))
            .when()
                .post("/api/products")
            .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Nested
    @DisplayName("GET /api/products: 상품 조회")
    class Retrieve {

        @Test
        void 상품이_없으면_빈_목록을_반환한다() {
            given()
            .when()
                .get("/api/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", empty());
        }

        @Test
        void 여러_상품을_생성하면_모두_조회된다() {
            // given
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "커피", "price", 5000,
                        "imageUrl", "http://image1.url", "categoryId", categoryId))
            .when()
                .post("/api/products");

            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "케이크", "price", 15000,
                        "imageUrl", "http://image2.url", "categoryId", categoryId))
            .when()
                .post("/api/products");

            // when & then
            given()
            .when()
                .get("/api/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("name", hasItem("커피"))
                .body("name", hasItem("케이크"));
        }
    }
}
