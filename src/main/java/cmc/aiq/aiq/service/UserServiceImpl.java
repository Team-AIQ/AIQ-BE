package cmc.aiq.aiq.service;

import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.dto.UserResponseDTO;
import cmc.aiq.aiq.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getMyInfo(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));

        return UserResponseDTO.from(user);
    }
}
