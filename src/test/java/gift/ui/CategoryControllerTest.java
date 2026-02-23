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
class CategoryControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/categories: 카테고리 생성")
    class Create {

        @Test
        void 카테고리를_생성하면_200을_반환한다() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "교환권"))
            .when()
                .post("/api/categories")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", equalTo("교환권"));
        }

        @Test
        void 카테고리를_생성하면_조회_시_포함된다() {
            // given
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "교환권"))
            .when()
                .post("/api/categories");

            // when & then
            given()
            .when()
                .get("/api/categories")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("name", hasItem("교환권"));
        }
    }

    @Nested
    @DisplayName("GET /api/categories: 카테고리 조회")
    class Retrieve {

        @Test
        void 카테고리가_없으면_빈_목록을_반환한다() {
            given()
            .when()
                .get("/api/categories")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", empty());
        }

        @Test
        void 여러_카테고리를_생성하면_모두_조회된다() {
            // given
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "교환권"))
            .when()
                .post("/api/categories");

            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "상품권"))
            .when()
                .post("/api/categories");

            // when & then
            given()
            .when()
                .get("/api/categories")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(2))
                .body("name", hasItem("교환권"))
                .body("name", hasItem("상품권"));
        }
    }
}
