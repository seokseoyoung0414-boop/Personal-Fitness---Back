
package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.Channel;
import com.nody.nodyspring.DB.ActivityRecord;
import com.nody.nodyspring.repository.ChannelRepository;
import com.nody.nodyspring.repository.ActivityRecordRepository;
import com.nody.nodyspring.dto.ActivityDTO.ActivityDTO.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private static final ZoneId KOREA =
            ZoneId.of("Asia/Seoul");

    private static final Set<String> SPORTS = Set.of(
            "걷기", "러닝", "등산", "사이클",
            "헬스", "요가", "필라테스", "기타"
    );

    private final ChannelRepository channelRepository;
    private final ActivityRecordRepository recordRepository;


    // ==================================================
    // 운동 기록 저장
    // 하루에 여러 번 저장 가능
    // ==================================================

    @Transactional
    public DetailResponse addRecord(
            String userid,
            Integer channelIdx,
            RecordRequest request
    ) {

        Channel channel = findMyChannel(userid, channelIdx);

        validateRecord(channel, request);

        ActivityRecord record = ActivityRecord.builder()
                .channel(channel)
                .recordedAt(request.recordedAt())
                .durationSeconds(request.durationSeconds())
                .distanceKm(request.distanceKm())
                .calories(request.calories())
                .steps(request.steps())
                .build();

        recordRepository.saveAndFlush(record);

        return getDetail(userid, channelIdx);
    }


    // ==================================================
    // 운동 기록 삭제
    // 본인 채널에 속한 기록만 삭제
    // ==================================================

    @Transactional
    public DetailResponse deleteRecord(
            String userid,
            Integer channelIdx,
            Long recordId
    ) {

        findMyChannel(userid, channelIdx);

        ActivityRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> notFound(
                        "운동 기록을 찾을 수 없습니다."
                ));

        if (!record.getChannel().getIdx().equals(channelIdx)) {
            throw notFound("운동 기록을 찾을 수 없습니다.");
        }

        recordRepository.delete(record);
        recordRepository.flush();

        return getDetail(userid, channelIdx);
    }


    // ==================================================
    // 채널별 운동 통계
    // 합계 / 운동 횟수별 달성률 / 날짜별 그래프 / 기록 목록
    // ==================================================

    public DetailResponse getDetail(
            String userid,
            Integer channelIdx
    ) {

        Channel channel = findMyChannel(userid, channelIdx);

        List<ActivityRecord> records =
                recordRepository
                        .findByChannel_IdxOrderByRecordedAtAscIdxAsc(
                                channelIdx
                        );

        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal cumulativeValue = BigDecimal.ZERO;

        long totalSeconds = 0;
        long totalSteps = 0;

        // 1회 운동 목표량
        BigDecimal goal = BigDecimal.valueOf(channel.getGoal());

        // 전체 운동 횟수
        int totalCount = records.size();

        // 목표 달성 및 미달 횟수
        int successCount = 0;
        int failCount = 0;

        Map<LocalDate, DailyTotal> dailyMap = new TreeMap<>();

        List<RecordResponse> recordResponses = new ArrayList<>();

        for (ActivityRecord record : records) {

            // 전체 운동 통계 합산
            totalDistance = totalDistance.add(
                    record.getDistanceKm()
            );

            totalCalories = totalCalories.add(
                    record.getCalories()
            );

            totalSeconds += record.getDurationSeconds();
            totalSteps += record.getSteps();

            // 해당 기록의 실제 달성량
            BigDecimal recordAchievedValue =
                    getAchievedValue(channel.getSport(), record);

            // 전체 누적 달성량
            cumulativeValue = cumulativeValue.add(
                    recordAchievedValue
            );

            // 개별 운동 목표 달성률
            BigDecimal recordAchievementRate = calculateRate(
                    recordAchievedValue,
                    goal
            );

            // 개별 운동 목표 달성 여부
            if (goal.signum() > 0) {

                if (recordAchievedValue.compareTo(goal) >= 0) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            // 개별 운동 기록 응답
            recordResponses.add(new RecordResponse(
                    record.getIdx(),
                    record.getRecordedAt(),
                    record.getDurationSeconds(),
                    record.getDistanceKm(),
                    record.getCalories(),
                    record.getSteps(),
                    recordAchievementRate
            ));

            // 날짜별 그래프 데이터
            LocalDate date =
                    record.getRecordedAt().toLocalDate();

            DailyTotal daily = dailyMap.computeIfAbsent(
                    date,
                    ignored -> new DailyTotal()
            );

            daily.distance = daily.distance.add(
                    record.getDistanceKm()
            );

            daily.calories = daily.calories.add(
                    record.getCalories()
            );

            daily.seconds += record.getDurationSeconds();
            daily.steps += record.getSteps();
        }

        // 전체 운동 횟수 기준 목표 달성률
        BigDecimal totalAchievementRate = BigDecimal.ZERO;

        if (goal.signum() > 0 && totalCount > 0) {

            totalAchievementRate = BigDecimal.valueOf(successCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(totalCount),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // 날짜별 그래프 데이터 생성
        List<ChartPoint> chart = dailyMap.entrySet()
                .stream()
                .map(entry -> {

                    DailyTotal daily = entry.getValue();

                    return new ChartPoint(
                            entry.getKey(),
                            daily.distance,
                            daily.seconds,
                            daily.calories,
                            daily.steps
                    );
                })
                .toList();

        // 기록 목록은 최근 기록부터 표시
        Collections.reverse(recordResponses);

        // 최종 상세 통계 응답
        return new DetailResponse(
                toChannelResponse(channel),
                totalDistance,
                totalSeconds,
                totalCalories,
                totalSteps,
                cumulativeValue,
                totalAchievementRate,
                totalCount,
                successCount,
                failCount,
                chart,
                recordResponses
        );
    }


    // ==================================================
    // 개인 진행 중인 채널별 운동시간 랭킹
    // 누적 운동시간이 긴 순서
    // ==================================================

    public List<ChannelActivityRankingResponse> getMyChannelActivityRanking(
            String userid
    ) {

        LocalDate today = LocalDate.now(KOREA);

        return recordRepository
                .findMyOngoingChannelRanking(userid, today)
                .stream()
                .map(ranking -> new ChannelActivityRankingResponse(
                        ranking.getChannelIdx(),
                        ranking.getChannelTitle(),
                        ranking.getSport(),
                        ranking.getTotalDurationSeconds() == null
                                ? 0L
                                : ranking.getTotalDurationSeconds()
                ))
                .toList();
    }


    // ==================================================
    // 본인 채널 조회
    // 다른 사용자의 채널 접근 방지
    // ==================================================

    private Channel findMyChannel(
            String userid,
            Integer channelIdx
    ) {

        return channelRepository
                .findByIdxAndCreator_Userid(channelIdx, userid)
                .orElseThrow(() -> notFound(
                        "운동 채널을 찾을 수 없습니다."
                ));
    }


    // ==================================================
    // 채널 응답 변환
    // ==================================================

    private GoalResponse toChannelResponse(Channel channel) {

        LocalDate today = LocalDate.now(KOREA);

        String status;

        if (today.isBefore(channel.getStartDate())) {
            status = "PENDING";
        } else if (today.isAfter(channel.getEndDate())) {
            status = "COMPLETED";
        } else {
            status = "ACTIVE";
        }

        return new GoalResponse(
                channel.getIdx().longValue(),
                channel.getChannelTitle(),
                channel.getChannelComment(),
                channel.getSport(),
                BigDecimal.valueOf(channel.getGoal()),
                getUnit(channel.getSport()),
                channel.getStartDate(),
                channel.getEndDate(),
                status,
                "ACTIVE".equals(status),
                "COMPLETED".equals(status)
        );
    }


    // ==================================================
    // 종목별 목표 단위
    // ==================================================

    private String getUnit(String sport) {

        return switch (sport) {
            case "걷기" -> "걸음";
            case "러닝", "사이클" -> "km";
            default -> "kcal";
        };
    }


    // ==================================================
    // 종목별 달성량 계산
    // ==================================================

    private BigDecimal getAchievedValue(
            String sport,
            ActivityRecord record
    ) {

        return switch (sport) {

            case "걷기" ->
                    BigDecimal.valueOf(record.getSteps());

            case "러닝", "사이클" ->
                    record.getDistanceKm();

            default ->
                    record.getCalories();
        };
    }


    // ==================================================
    // 개별 운동 달성률 계산
    // 해당 기록의 달성량 / 1회 목표량 * 100
    // ==================================================

    private BigDecimal calculateRate(
            BigDecimal achieved,
            BigDecimal goal
    ) {

        if (goal == null || goal.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return achieved
                .multiply(BigDecimal.valueOf(100))
                .divide(goal, 2, RoundingMode.HALF_UP);
    }


    // ==================================================
    // 운동 기록 입력 검사
    // ==================================================

    private void validateRecord(
            Channel channel,
            RecordRequest request
    ) {

        if (request == null || request.recordedAt() == null) {
            throw badRequest("운동 날짜와 시간을 입력해주세요.");
        }

        if (request.recordedAt().isAfter(
                LocalDateTime.now(KOREA)
        )) {
            throw badRequest("미래의 운동 기록은 저장할 수 없습니다.");
        }

        LocalDate recordDate =
                request.recordedAt().toLocalDate();

        if (recordDate.isBefore(channel.getStartDate())
                || recordDate.isAfter(channel.getEndDate())) {

            throw badRequest(
                    "목표 기간 안의 운동 기록만 저장할 수 있습니다."
            );
        }

        if (request.durationSeconds() == null
                || request.durationSeconds() <= 0
                || request.durationSeconds() > 86400) {

            throw badRequest(
                    "운동시간은 1초 이상, 24시간 이하로 입력해주세요."
            );
        }

        validateDecimal(
                request.distanceKm(),
                "운동거리",
                new BigDecimal("999999999.999"),
                3
        );

        validateDecimal(
                request.calories(),
                "칼로리",
                new BigDecimal("9999999999.99"),
                2
        );

        if (request.steps() == null
                || request.steps() < 0
                || request.steps() > 1000000) {

            throw badRequest(
                    "걸음 수는 0~1000000 사이로 입력해주세요."
            );
        }
    }


    // ==================================================
    // 숫자 검증
    // ==================================================

    private void validateDecimal(
            BigDecimal value,
            String label,
            BigDecimal maximum,
            int scale
    ) {

        if (value == null || value.signum() < 0) {
            throw badRequest(
                    label + "은 0 이상의 숫자로 입력해주세요."
            );
        }

        if (value.compareTo(maximum) > 0
                || value.stripTrailingZeros().scale() > scale) {

            throw badRequest(
                    label + "의 값 또는 소수점 자릿수를 확인해주세요."
            );
        }
    }


    // ==================================================
    // 예외 처리
    // ==================================================

    private ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(String message) {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }


    // ==================================================
    // 날짜별 합계 임시 객체
    // ==================================================

    private static class DailyTotal {

        private BigDecimal distance = BigDecimal.ZERO;

        private BigDecimal calories = BigDecimal.ZERO;

        private long seconds;

        private long steps;
    }
}
