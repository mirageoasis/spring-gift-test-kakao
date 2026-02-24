package gift.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class Config {

        @Bean
        @Scope("cucumber-glue")
        public ScenarioContext scenarioContext() {
            return new ScenarioContext();
        }

        @Bean
        @Scope("cucumber-glue")
        public ApiClient apiClient(@Value("${local.server.port}") int port) {
            return new ApiClient(port);
        }
    }
}
