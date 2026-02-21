package cmc.aiq.aiq.service.Credit;

import cmc.aiq.aiq.domain.CreditLog;
import cmc.aiq.aiq.domain.Users;
import cmc.aiq.aiq.repository.CreditLogRepository;
import cmc.aiq.aiq.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final UsersRepository usersRepository;
    private final CreditLogRepository creditLogRepository;

    @Override
    @Transactional
    public void grantCredit(Long userId, BigDecimal amount, String reason) {
        // 1. 사용자 정보 조회
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 2. 사용자 크레딧 업데이트
        user.addCredits(amount.longValue()); // Users 엔티티에 addCredits 메소드 필요
        usersRepository.save(user);

        // 3. 크레딧 지급 로그 생성
        CreditLog creditLog = CreditLog.builder()
                .user(user)
                .amount(amount.longValue())
                .balanceAfter(user.getCurrentCredits())
                .reason(reason)
                .build();
        creditLogRepository.save(creditLog);
    }
}
