package adapters.driving.web.spring.configuration;

import com.example.cleanarchitectureapplication.socle.dependencyinjection.annotation.Mapper;
import com.example.cleanarchitectureapplication.socle.dependencyinjection.annotation.UseCase;
import com.example.cleanarchitectureapplication.socle.time.CurrentDateProvider;
import com.example.cleanarchitectureapplication.socle.time.DateProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(
        basePackages = {
                "com.example.cleanarchitectureapplication",
                "adapters.driven.persistence.hibernate"
        },
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {UseCase.class, Mapper.class}
        )
)
@EnableJpaRepositories(basePackages = {"adapters.driven.persistence.hibernate"})
public class CustomAnnotationScanConfiguration {

    @Bean
    public DateProvider dateProvider() {
        return new CurrentDateProvider();
    }
}

