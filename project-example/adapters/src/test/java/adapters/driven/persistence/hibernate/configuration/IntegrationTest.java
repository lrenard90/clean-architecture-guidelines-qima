package adapters.driven.persistence.hibernate.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@EntityScan("adapters.driven.persistence.hibernate")
@ComponentScan(basePackages = {"adapters.driven.persistence.hibernate"})
@EnableJpaRepositories("adapters.driven.persistence.hibernate")
public abstract class IntegrationTest extends ContainerizedTest {
}

