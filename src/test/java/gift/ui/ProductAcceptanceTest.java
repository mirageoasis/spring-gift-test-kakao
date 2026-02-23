package gift.ui;

import gift.model.Category;
import gift.model.CategoryRepository;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // 카테고리는 API 호출로 준비
        category = restTemplate.postForEntity(
                "/api/categories", Map.of("name", "테스트 카테고리"), Category.class
        ).getBody();
    }

    @Nested
    @DisplayName("POST /api/products: 상품 생성")
    class Create {

        @Test
        void 상품을_생성하면_200을_반환한다() {
            // given
            var request = Map.of(
                    "name", "커피",
                    "price", 5000,
                    "imageUrl", "http://image.url",
                    "categoryId", category.getId()
            );

            // when
            ResponseEntity<Product> response = restTemplate.postForEntity(
                    "/api/products", request, Product.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("커피");
            assertThat(response.getBody().getPrice()).isEqualTo(5000);
        }

        @Test
        void 상품을_생성하면_조회_시_포함된다() {
            // given
            restTemplate.postForEntity(
                    "/api/products",
                    Map.of("name", "커피", "price", 5000, "imageUrl", "http://image.url",
                            "categoryId", category.getId()),
                    Product.class);

            // when
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    "/api/products", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // then
            assertThat(response.getBody())
                    .extracting(Product::getName)
                    .contains("커피");
        }

        @Test
        void 존재하지_않는_카테고리로_상품을_생성하면_500_에러() {
            // given
            var request = Map.of(
                    "name", "커피",
                    "price", 5000,
                    "imageUrl", "http://image.url",
                    "categoryId", 99999
            );

            // when
            ResponseEntity<Product> response = restTemplate.postForEntity(
                    "/api/products", request, Product.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("GET /api/products: 상품 조회")
    class Retrieve {

        @Test
        void 상품이_없으면_빈_목록을_반환한다() {
            // when
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    "/api/products", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        void 여러_상품을_생성하면_모두_조회된다() {
            // given
            restTemplate.postForEntity("/api/products",
                    Map.of("name", "커피", "price", 5000, "imageUrl", "http://image1.url",
                            "categoryId", category.getId()),
                    Product.class);
            restTemplate.postForEntity("/api/products",
                    Map.of("name", "케이크", "price", 15000, "imageUrl", "http://image2.url",
                            "categoryId", category.getId()),
                    Product.class);

            // when
            ResponseEntity<List<Product>> response = restTemplate.exchange(
                    "/api/products", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // then
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody())
                    .extracting(Product::getName)
                    .containsExactlyInAnyOrder("커피", "케이크");
        }
    }
}
