package adapters.driving.web.spring.messaging.web.controller;

import com.example.cleanarchitectureapplication.messaging.application.dto.EditMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.dto.PostMessageRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.usecases.EditMessageUseCaseHandler;
import com.example.cleanarchitectureapplication.messaging.application.usecases.PostMessageUseCaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    private final PostMessageUseCaseHandler postMessageUseCaseHandler;
    private final EditMessageUseCaseHandler editMessageUseCaseHandler;

    public MessageController(
            PostMessageUseCaseHandler postMessageUseCaseHandler,
            EditMessageUseCaseHandler editMessageUseCaseHandler) {
        this.postMessageUseCaseHandler = postMessageUseCaseHandler;
        this.editMessageUseCaseHandler = editMessageUseCaseHandler;
    }

    @PostMapping
    public void postMessage(@RequestBody PostMessageRequestDTO postMessageRequestDTO) {
        logger.debug("Post message {}", postMessageRequestDTO);
        postMessageUseCaseHandler.handle(postMessageRequestDTO);
    }

    @PutMapping
    public void editMessage(@RequestBody EditMessageRequestDTO editMessageRequestDTO) {
        logger.debug("Edit message {}", editMessageRequestDTO);
        editMessageUseCaseHandler.handle(editMessageRequestDTO);
    }
}

