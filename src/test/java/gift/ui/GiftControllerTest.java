package gift.ui;

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
class GiftControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Option option;
    private Member sender;
    private Member receiver;

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
    @DisplayName("POST /api/gifts: 선물 전송 API")
    class GiveGift {

        @Test
        void 선물_전송_API_성공() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", option.getId(), "quantity", 3,
                        "receiverId", receiver.getId(), "message", "축하해"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.OK.value());

            // 재고 감소 확인 (다음 행동 검증)
            Option updated = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(updated.getQuantity()).isEqualTo(7);
        }

        @Test
        void Member_Id_헤더가_없으면_400_에러() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(Map.of("optionId", option.getId(), "quantity", 1,
                        "receiverId", receiver.getId(), "message", "축하해"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        void 존재하지_않는_옵션이면_500_에러() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", 99999, "quantity", 1,
                        "receiverId", receiver.getId(), "message", "축하해"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        @Test
        void 재고가_부족하면_500_에러() {
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Member-Id", sender.getId())
                .body(Map.of("optionId", option.getId(), "quantity", 100,
                        "receiverId", receiver.getId(), "message", "축하해"))
            .when()
                .post("/api/gifts")
            .then()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

            // 재고 변경 없음 확인
            Option updated = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(updated.getQuantity()).isEqualTo(10);
        }
    }
}
