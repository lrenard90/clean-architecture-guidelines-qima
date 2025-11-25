package adapters.driving.web.spring.messaging.web.controller;

import adapters.driving.web.spring.e2e.configuration.E2eApiTest;
import com.example.cleanarchitectureapplication.messaging.application.dto.PostMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.ports.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import java.util.UUID;

class MessageControllerE2ETest extends E2eApiTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    private static final String BASE_URL = "/api/messages";

    @Test
    void postAMessageOK() throws Exception {
        PostMessageRequestDTO postMessageRequestDTO = new PostMessageRequestDTO(
                UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"),
                "Alice",
                "Hello world!"
        );

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(objectMapper.writeValueAsString(postMessageRequestDTO))
                .when()
                .post(BASE_URL)
                .then()
                .statusCode(HttpStatus.OK.value());

        messageRepository.findById(postMessageRequestDTO.id()).ifPresent(message -> {
            Assertions.assertThat(message.data().id()).isEqualTo(postMessageRequestDTO.id());
            Assertions.assertThat(message.data().author()).isEqualTo(postMessageRequestDTO.author());
            Assertions.assertThat(message.data().text()).isEqualTo(postMessageRequestDTO.text());
            Assertions.assertThat(message.data().publishedDate()).isNotNull();
        });
    }
}

