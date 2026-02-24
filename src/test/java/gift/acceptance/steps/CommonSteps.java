package gift.acceptance.steps;

import gift.acceptance.ScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    @Autowired
    private ScenarioContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void cleanUp() {
        jdbcTemplate.execute("TRUNCATE wish, option, product, category, member CASCADE");
    }

    @Then("응답 상태 코드는 {int}이다")
    public void 응답_상태_코드는(int statusCode) {
        assertThat(context.getResponse().statusCode()).isEqualTo(statusCode);
    }
}
