package cmc.aiq.aiq.service.Credit;

import cmc.aiq.aiq.domain.CreditLog;
import cmc.aiq.aiq.domain.ENUM.AuthProvider;
import cmc.aiq.aiq.domain.ENUM.CreditTransactionType;
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
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.addCredits(amount.longValue());
        usersRepository.save(user);

        CreditLog creditLog = CreditLog.builder()
                .user(user)
                .amount(amount.longValue())
                .balanceAfter(user.getCurrentCredits())
                .reason(reason)
                .build();
        creditLogRepository.save(creditLog);
    }

    @Override
    @Transactional
    public void useCredit(Long userId, CreditTransactionType type) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 게스트 유저는 크레딧을 차감하지 않고 항상 통과
        if (user.getProvider() == AuthProvider.GUEST) {
            return;
        }

        // 회원인 경우 크레딧 차감
        user.deductCredits(type.getCost());
        usersRepository.save(user);

        // 크레딧 사용 로그 생성
        CreditLog creditLog = CreditLog.builder()
                .user(user)
                .amount(-type.getCost()) // 사용은 음수로 기록
                .balanceAfter(user.getCurrentCredits())
                .reason(type.getDescription()) // ENUM에 정의된 설명 사용
                .build();
        creditLogRepository.save(creditLog);
    }
}
