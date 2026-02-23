package gift.ui;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.OptionRepository;
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
class CategoryControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/categories: 카테고리 생성")
    class Create {

        @Test
        void 카테고리를_생성하면_200을_반환한다() {
            // given
            var request = Map.of("name", "교환권");

            // when
            ResponseEntity<Category> response = restTemplate.postForEntity(
                    "/api/categories", request, Category.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getName()).isEqualTo("교환권");
        }

        @Test
        void 카테고리를_생성하면_조회_시_포함된다() {
            // given
            restTemplate.postForEntity(
                    "/api/categories", Map.of("name", "교환권"), Category.class);

            // when
            ResponseEntity<List<Category>> response = restTemplate.exchange(
                    "/api/categories", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // then
            assertThat(response.getBody())
                    .extracting(Category::getName)
                    .contains("교환권");
        }
    }

    @Nested
    @DisplayName("GET /api/categories: 카테고리 조회")
    class Retrieve {

        @Test
        void 카테고리가_없으면_빈_목록을_반환한다() {
            // when
            ResponseEntity<List<Category>> response = restTemplate.exchange(
                    "/api/categories", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        void 여러_카테고리를_생성하면_모두_조회된다() {
            // given
            restTemplate.postForEntity("/api/categories", Map.of("name", "교환권"), Category.class);
            restTemplate.postForEntity("/api/categories", Map.of("name", "상품권"), Category.class);

            // when
            ResponseEntity<List<Category>> response = restTemplate.exchange(
                    "/api/categories", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // then
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody())
                    .extracting(Category::getName)
                    .containsExactlyInAnyOrder("교환권", "상품권");
        }
    }
}
