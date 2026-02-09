package cmc.aiq.aiq.controller;

import cmc.aiq.aiq.dto.ApiResponse;
import cmc.aiq.aiq.dto.ChatDTO.ChatResponse;
import cmc.aiq.aiq.service.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Log4j2
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/{queryId}/start")
    public ApiResponse<List<String>> startChat(
            @PathVariable Long queryId,
            @RequestParam(defaultValue = "GPT") String modelName) {

        log.info("채팅 시작 요청 - QueryId: {}, Model: {}", queryId, modelName);
        List<String> initialQuestions = chatService.initiateChatRoundZero(queryId, modelName);

        return ApiResponse.success(HttpStatus.OK, "채팅 가이드 질문이 생성되었습니다.", initialQuestions);
    }
    @PostMapping("/{queryId}/send")
    public ApiResponse<ChatResponse> sendChatMessage(
            @PathVariable Long queryId,
            @RequestBody ChatRequest request) {

        log.info("채팅 메시지 전송 - QueryId: {}, Question: {}", queryId, request.userQuestion());
        ChatResponse response = chatService.processUserChat(queryId, request.userQuestion());

        return ApiResponse.success(HttpStatus.OK, "AI 응답이 완료되었습니다.", response);
    }

    /**
     * 요청 데이터를 받기 위한 내부 DTO
     */
    public record ChatRequest(String userQuestion) {}
}
