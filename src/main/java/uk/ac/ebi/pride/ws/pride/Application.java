package uk.ac.ebi.pride.ws.pride;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Retrieve the projects {@link uk.ac.ebi.pride.archive.dataprovider.project.ProjectProvider} from PRIDE Archive and the corresponding information.
 *
 * @author ypriverol
 */

@EnableSwagger2
@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Component
    @Primary
    private class CustomObjectMapper extends ObjectMapper {
        public CustomObjectMapper() {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
            configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
            enable(SerializationFeature.INDENT_OUTPUT);

        }
    }


}
