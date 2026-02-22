package cmc.aiq.aiq.service;

import cmc.aiq.aiq.dto.UserResponseDTO;

public interface UserService {
    /**
     * 현재 로그인된 사용자의 정보를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 정보 DTO
     */
    UserResponseDTO getMyInfo(Long userId);
}
