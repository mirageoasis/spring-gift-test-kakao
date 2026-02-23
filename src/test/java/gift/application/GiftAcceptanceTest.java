package gift.application;

import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
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
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member sender;
    private Member receiver;
    private Option option;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        // 카테고리, 상품: API 호출로 준비
        Long categoryId = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "테스트 카테고리"))
            .when()
                .post("/api/categories")
            .then()
                .extract().jsonPath().getLong("id");

        Long productId = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("name", "테스트 상품", "price", 10000,
                        "imageUrl", "http://image.url", "categoryId", categoryId))
            .when()
                .post("/api/products")
            .then()
                .extract().jsonPath().getLong("id");

        Product product = productRepository.findById(productId).orElseThrow();

        // 옵션, 회원: API 미제공으로 Repository 사용
        option = optionRepository.save(new Option("기본 옵션", 10, product));
        sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
    }

    @Nested
    @DisplayName("POST /api/gifts: 선물 전송")
    class Give {

        @Test
        void 선물_전송_성공_시_200을_반환한다() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", option.getId(), "quantity", 3,
                        "receiverId", receiver.getId(), "message", "선물입니다"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.OK.value());
        }

        @Test
        void 선물_전송_성공_시_재고가_차감된다() {
            // given & when
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", option.getId(), "quantity", 3,
                        "receiverId", receiver.getId(), "message", "선물입니다"))
            .when()
                .post("/api/gifts");

            // then
            Option found = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(found.getQuantity()).isEqualTo(7);
        }

        @Test
        void 존재하지_않는_옵션으로_전송하면_500을_반환한다() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", 999, "quantity", 1,
                        "receiverId", receiver.getId(), "message", "선물입니다"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void 재고_부족_시_500을_반환한다() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", option.getId(), "quantity", 11,
                        "receiverId", receiver.getId(), "message", "선물입니다"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void 재고_부족_시_재고가_변경되지_않는다() {
            // given & when
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", option.getId(), "quantity", 11,
                        "receiverId", receiver.getId(), "message", "선물입니다"))
            .when()
                .post("/api/gifts");

            // then
            Option found = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(found.getQuantity()).isEqualTo(10);
        }
    }
}
