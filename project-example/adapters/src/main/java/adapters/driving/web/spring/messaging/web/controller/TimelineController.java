package adapters.driving.web.spring.messaging.web.controller;

import com.example.cleanarchitectureapplication.messaging.application.dto.GetTimelineRequestDTO;
import com.example.cleanarchitectureapplication.messaging.application.dto.TimelineMessageDTO;
import com.example.cleanarchitectureapplication.messaging.application.usecases.ViewTimelineUseCaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {
    private static final Logger logger = LoggerFactory.getLogger(TimelineController.class);
    private final ViewTimelineUseCaseHandler viewTimelineUseCaseHandler;

    public TimelineController(ViewTimelineUseCaseHandler viewTimelineUseCaseHandler) {
        this.viewTimelineUseCaseHandler = viewTimelineUseCaseHandler;
    }

    @GetMapping
    public List<TimelineMessageDTO> viewTimeline(@RequestParam String author) {
        logger.debug("View timeline for author {}", author);
        GetTimelineRequestDTO getTimelineRequestDTO = new GetTimelineRequestDTO(author);
        return viewTimelineUseCaseHandler.handle(getTimelineRequestDTO);
    }
}

