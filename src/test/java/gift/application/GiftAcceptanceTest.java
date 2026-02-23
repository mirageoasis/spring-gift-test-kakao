package gift.application;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.OptionRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GiftAcceptanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        // 카테고리, 상품: API 호출로 준비
        Category category = restTemplate.postForEntity(
                "/api/categories", Map.of("name", "테스트 카테고리"), Category.class
        ).getBody();
        Product product = restTemplate.postForEntity(
                "/api/products", Map.of("name", "테스트 상품", "price", 10000,
                        "imageUrl", "http://image.url", "categoryId", category.getId()),
                Product.class
        ).getBody();

        // 옵션, 회원: API 미제공으로 Repository 사용
        option = optionRepository.save(new Option("기본 옵션", 10, product));
        sender = memberRepository.save(new Member("보내는사람", "sender@test.com"));
        receiver = memberRepository.save(new Member("받는사람", "receiver@test.com"));
    }

    private HttpEntity<Map<String, Object>> createGiftRequest(Long optionId, int quantity, Long receiverId, String message, Long memberId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Member-Id", memberId.toString());
        Map<String, Object> body = Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "receiverId", receiverId,
                "message", message
        );
        return new HttpEntity<>(body, headers);
    }

    @Nested
    @DisplayName("POST /api/gifts: 선물 전송")
    class Give {

        @Test
        void 선물_전송_성공_시_200을_반환한다() {
            // given
            HttpEntity<Map<String, Object>> request = createGiftRequest(
                    option.getId(), 3, receiver.getId(), "선물입니다", sender.getId()
            );

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/gifts", HttpMethod.POST, request, Void.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void 선물_전송_성공_시_재고가_차감된다() {
            // given
            HttpEntity<Map<String, Object>> request = createGiftRequest(
                    option.getId(), 3, receiver.getId(), "선물입니다", sender.getId()
            );

            // when
            restTemplate.exchange("/api/gifts", HttpMethod.POST, request, Void.class);

            // then
            Option found = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(found.getQuantity()).isEqualTo(7);
        }

        @Test
        void 존재하지_않는_옵션으로_전송하면_500을_반환한다() {
            // given
            HttpEntity<Map<String, Object>> request = createGiftRequest(
                    999L, 1, receiver.getId(), "선물입니다", sender.getId()
            );

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/gifts", HttpMethod.POST, request, Void.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        void 재고_부족_시_500을_반환한다() {
            // given
            HttpEntity<Map<String, Object>> request = createGiftRequest(
                    option.getId(), 11, receiver.getId(), "선물입니다", sender.getId()
            );

            // when
            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/gifts", HttpMethod.POST, request, Void.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        void 재고_부족_시_재고가_변경되지_않는다() {
            // given
            HttpEntity<Map<String, Object>> request = createGiftRequest(
                    option.getId(), 11, receiver.getId(), "선물입니다", sender.getId()
            );

            // when
            restTemplate.exchange("/api/gifts", HttpMethod.POST, request, Void.class);

            // then
            Option found = optionRepository.findById(option.getId()).orElseThrow();
            assertThat(found.getQuantity()).isEqualTo(10);
        }
    }
}
