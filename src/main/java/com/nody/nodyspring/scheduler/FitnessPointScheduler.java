package com.nody.nodyspring.scheduler;

import com.nody.nodyspring.DB.*;
import com.nody.nodyspring.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FitnessPointScheduler {

    private final FitnessAuthRepository fitnessAuthRepository;
    private final ChannelRepository     channelRepository;
    private final UserRepository        userRepository;
    private final ChannelUserRepository channelUserRepository;

    /** 매일 00:30(KST) 정산 */
    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void distributePoints() {

        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (Channel channel : channelRepository.findAll()) {

            List<FitnessAuth> auths =
                    fitnessAuthRepository.findByChannelAndAuthDate(channel, yesterday);

            if (auths.isEmpty()) continue;

            switch (channel.getModes()) {
                case "협력" -> handleCoopMode(channel, auths);
                case "경쟁" -> handleCompetitiveMode(auths);
                default     -> { /* 기타 모드는 포인트 미지급 */ }
            }
        }
    }

    /* ────────────────────────────────────  협력  ─────────────────────────────────── */

    private void handleCoopMode(Channel channel, List<FitnessAuth> auths) {

        long totalUsers = channelUserRepository.countByChannel(channel);
        long  successCount = auths.stream()
                .filter(a -> Boolean.TRUE.equals(a.getGoalMet()))
                .count();

        double successRate = totalUsers == 0 ? 0 : (double) successCount / totalUsers; // 0.0 ~ 1.0
        int    point       = (int) Math.floor(successRate * 10);                       // 0 ~ 10P

        if (point == 0) return; // 0포인트면 지급 의미 없음

        auths.stream()
                .filter(a -> Boolean.TRUE.equals(a.getGoalMet()))
                .forEach(a -> addPoint(a.getUser(), point));
    }

    /* ────────────────────────────────────  경쟁  ─────────────────────────────────── */

    private void handleCompetitiveMode(List<FitnessAuth> auths) {

        int[] pointTable = {10, 9, 8, 7, 6};           // 1~5등 점수

        List<FitnessAuth> ranking = auths.stream()
                .filter(a -> Boolean.TRUE.equals(a.getGoalMet()))
                .sorted((a, b) -> Double.compare(
                        b.getAchievedValue(), a.getAchievedValue())) // 높은 값이 1등
                .limit(5)                                            // 상위 5명
                .collect(Collectors.toList());

        for (int i = 0; i < ranking.size(); i++) {
            addPoint(ranking.get(i).getUser(), pointTable[i]);
        }
    }

    /* ──────────────────────────  공통 포인트 지급 & 저장  ────────────────────────── */

    private void addPoint(User user, int p) {
        user.setPoint(user.getPoint() + p);
        userRepository.save(user);
    }
}
