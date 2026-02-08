package cmc.aiq.aiq.service.ChatService;

import cmc.aiq.aiq.dto.ChatDTO.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ChatService {
    List<String> initiateChatRoundZero(Long queryId, String modelName);
    ChatResponse processUserChat(Long queryId, String userQuestion);
}
