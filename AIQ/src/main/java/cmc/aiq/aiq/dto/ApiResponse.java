package cmc.aiq.aiq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // data 필드가 null일 경우 JSON 응답에 포함하지 않음
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .data(data)
                .build();
    }

    // [추가] 실패 응답을 위한 정적 메소드
    public static <T> ApiResponse<T> failure(HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .build(); // 실패 시에는 data 필드는 null
    }
}
