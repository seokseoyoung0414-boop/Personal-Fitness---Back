package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.ActivityGoal;
import com.nody.nodyspring.DB.ActivityRecord;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.dto.ActivityDTO.ActivityDTO.*;
import com.nody.nodyspring.repository.ActivityGoalRepository;
import com.nody.nodyspring.repository.ActivityRecordRepository;
import com.nody.nodyspring.repository.UserRepository;
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

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private static final Set<String> SPORTS = Set.of(
            "걷기", "러닝", "등산", "사이클",
            "헬스", "요가", "필라테스", "기타"
    );

    private final ActivityGoalRepository goalRepository;
    private final ActivityRecordRepository recordRepository;
    private final UserRepository userRepository;

    // ==================================================
    // 내 목표 목록
    // ==================================================

    public List<GoalResponse> getGoals(String userid) {
        return goalRepository
                .findByUser_UseridOrderByCreatedAtDesc(userid)
                .stream()
                .map(this::toGoalResponse)
                .toList();
    }

    // ==================================================
    // 목표 하나 조회
    // ==================================================

    public GoalResponse getGoal(String userid, Long goalId) {
        return toGoalResponse(findMyGoal(userid, goalId));
    }

    // ==================================================
    // 목표 생성
    // ==================================================

    @Transactional
    public GoalResponse createGoal(
            String userid,
            GoalRequest request
    ) {
        validateGoal(request);

        if (request.startDate().isBefore(LocalDate.now(KOREA))) {
            throw badRequest("시작일은 오늘 이후로 선택해주세요.");
        }

        User user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "사용자를 찾을 수 없습니다."
                ));

        ActivityGoal goal = ActivityGoal.builder()
                .user(user)
                .title(request.channelTitle().trim())
                .comment(request.channelComment().trim())
                .sport(request.sport())
                .goal(request.goal())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        return toGoalResponse(goalRepository.save(goal));
    }

    // ==================================================
    // 목표 수정
    // ==================================================

    @Transactional
    public GoalResponse updateGoal(
            String userid,
            Long goalId,
            GoalRequest request
    ) {
        ActivityGoal goal = findMyGoal(userid, goalId);
        validateGoal(request);

        // 이미 저장된 기록이 있을 때 종목을 바꾸면
        // 목표 단위와 기존 기록 해석이 달라질 수 있으므로 제한
        List<ActivityRecord> records = recordRepository
                .findByGoal_IdxOrderByRecordedAtAscIdxAsc(goalId);

        if (!records.isEmpty()
                && !goal.getSport().equals(request.sport())) {
            throw badRequest(
                    "운동 기록이 있는 목표는 종목을 변경할 수 없습니다."
            );
        }

        // 수정한 기간 밖으로 기존 기록이 밀려나지 않도록 확인
        for (ActivityRecord record : records) {
            LocalDate date = record.getRecordedAt().toLocalDate();

            if (date.isBefore(request.startDate())
                    || date.isAfter(request.endDate())) {
                throw badRequest(
                        "기존 운동 기록이 포함되도록 기간을 설정해주세요."
                );
            }
        }

        goal.setTitle(request.channelTitle().trim());
        goal.setComment(request.channelComment().trim());
        goal.setSport(request.sport());
        goal.setGoal(request.goal());
        goal.setStartDate(request.startDate());
        goal.setEndDate(request.endDate());

        return toGoalResponse(goal);
    }

    // ==================================================
    // 목표 삭제
    // 해당 목표의 운동 기록도 함께 삭제
    // ==================================================

    @Transactional
    public void deleteGoal(String userid, Long goalId) {
        ActivityGoal goal = findMyGoal(userid, goalId);

        recordRepository.deleteByGoal_Idx(goalId);
        recordRepository.flush();

        goalRepository.delete(goal);
    }

    // ==================================================
    // 운동 기록 저장
    // 하루에 여러 번 저장 가능
    // ==================================================

    @Transactional
    public DetailResponse addRecord(
            String userid,
            Long goalId,
            RecordRequest request
    ) {
        ActivityGoal goal = findMyGoal(userid, goalId);

        validateRecord(goal, request);

        ActivityRecord record = ActivityRecord.builder()
                .goal(goal)
                .recordedAt(request.recordedAt())
                .durationSeconds(request.durationSeconds())
                .distanceKm(request.distanceKm())
                .calories(request.calories())
                .steps(request.steps())
                .build();

        recordRepository.saveAndFlush(record);

        return getDetail(userid, goalId);
    }

    // ==================================================
    // 운동 기록 삭제
    // 본인 목표에 속한 기록만 삭제
    // ==================================================

    @Transactional
    public DetailResponse deleteRecord(
            String userid,
            Long goalId,
            Long recordId
    ) {
        findMyGoal(userid, goalId);

        ActivityRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> notFound(
                        "운동 기록을 찾을 수 없습니다."
                ));

        if (!record.getGoal().getIdx().equals(goalId)) {
            throw notFound("운동 기록을 찾을 수 없습니다.");
        }

        recordRepository.delete(record);
        recordRepository.flush();

        return getDetail(userid, goalId);
    }

    // ==================================================
    // 상세 통계
    // 합계 / 달성률 / 날짜별 그래프 / 기록 목록
    // ==================================================

    public DetailResponse getDetail(
            String userid,
            Long goalId
    ) {
        ActivityGoal goal = findMyGoal(userid, goalId);

        List<ActivityRecord> records = recordRepository
                .findByGoal_IdxOrderByRecordedAtAscIdxAsc(goalId);

        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal totalCalories = BigDecimal.ZERO;
        BigDecimal cumulativeValue = BigDecimal.ZERO;

        long totalSeconds = 0;
        long totalSteps = 0;

        Map<LocalDate, DailyTotal> dailyMap = new TreeMap<>();
        List<RecordResponse> recordResponses = new ArrayList<>();

        for (ActivityRecord record : records) {
            totalDistance = totalDistance.add(record.getDistanceKm());
            totalCalories = totalCalories.add(record.getCalories());

            totalSeconds += record.getDurationSeconds();
            totalSteps += record.getSteps();

            cumulativeValue = cumulativeValue.add(
                    getAchievedValue(goal.getSport(), record)
            );

            recordResponses.add(new RecordResponse(
                    record.getIdx(),
                    record.getRecordedAt(),
                    record.getDurationSeconds(),
                    record.getDistanceKm(),
                    record.getCalories(),
                    record.getSteps(),
                    calculateRate(cumulativeValue, goal.getGoal())
            ));

            LocalDate date = record.getRecordedAt().toLocalDate();

            DailyTotal daily = dailyMap.computeIfAbsent(
                    date,
                    ignored -> new DailyTotal()
            );

            daily.distance = daily.distance.add(record.getDistanceKm());
            daily.calories = daily.calories.add(record.getCalories());
            daily.seconds += record.getDurationSeconds();
            daily.steps += record.getSteps();
        }

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

        // 목록은 최근 기록부터 표시
        Collections.reverse(recordResponses);

        return new DetailResponse(
                toGoalResponse(goal),
                totalDistance,
                totalSeconds,
                totalCalories,
                totalSteps,
                cumulativeValue,
                calculateRate(cumulativeValue, goal.getGoal()),
                chart,
                recordResponses
        );
    }

    // ==================================================
    // 본인 목표 조회
    // 다른 사람의 목표는 조회되지 않음
    // ==================================================

    private ActivityGoal findMyGoal(
            String userid,
            Long goalId
    ) {
        return goalRepository.findByIdxAndUser_Userid(goalId, userid)
                .orElseThrow(() -> notFound(
                        "운동 목표를 찾을 수 없습니다."
                ));
    }

    // ==================================================
    // 목표 응답 변환
    // ==================================================

    private GoalResponse toGoalResponse(ActivityGoal goal) {
        LocalDate today = LocalDate.now(KOREA);

        String status;

        if (today.isBefore(goal.getStartDate())) {
            status = "PENDING";
        } else if (today.isAfter(goal.getEndDate())) {
            status = "COMPLETED";
        } else {
            status = "ACTIVE";
        }

        return new GoalResponse(
                goal.getIdx(),
                goal.getTitle(),
                goal.getComment(),
                goal.getSport(),
                goal.getGoal(),
                getUnit(goal.getSport()),
                goal.getStartDate(),
                goal.getEndDate(),
                status,
                "ACTIVE".equals(status),
                "COMPLETED".equals(status)
        );
    }

    // ==================================================
    // 종목별 목표 단위
    // 기존 생성 화면 기준
    // ==================================================

    private String getUnit(String sport) {
        return switch (sport) {
            case "걷기" -> "걸음";
            case "러닝", "사이클" -> "km";
            default -> "kcal";
        };
    }

    private BigDecimal getAchievedValue(
            String sport,
            ActivityRecord record
    ) {
        return switch (sport) {
            case "걷기" -> BigDecimal.valueOf(record.getSteps());
            case "러닝", "사이클" -> record.getDistanceKm();
            default -> record.getCalories();
        };
    }

    // 누적 달성량 ÷ 목표량 × 100
    private BigDecimal calculateRate(
            BigDecimal achieved,
            BigDecimal goal
    ) {
        return achieved
                .multiply(BigDecimal.valueOf(100))
                .divide(goal, 2, RoundingMode.HALF_UP);
    }

    // ==================================================
    // 목표 입력 검사
    // ==================================================

    private void validateGoal(GoalRequest request) {
        if (request == null) {
            throw badRequest("목표 정보를 입력해주세요.");
        }

        if (request.channelTitle() == null
                || request.channelTitle().isBlank()
                || request.channelTitle().trim().length() > 100) {
            throw badRequest("운동 이름은 1~100자로 입력해주세요.");
        }

        if (request.channelComment() == null
                || request.channelComment().isBlank()) {
            throw badRequest("운동 소개를 입력해주세요.");
        }

        if (request.sport() == null
                || !SPORTS.contains(request.sport())) {
            throw badRequest("올바른 운동 종목을 선택해주세요.");
        }

        validateDecimal(
                request.goal(),
                "목표량",
                new BigDecimal("9999999999.99"),
                2
        );

        if (request.goal().signum() <= 0) {
            throw badRequest("목표량은 0보다 커야 합니다.");
        }

        if ("걷기".equals(request.sport())
                && request.goal().stripTrailingZeros().scale() > 0) {
            throw badRequest("걸음 목표는 정수로 입력해주세요.");
        }

        if (request.startDate() == null
                || request.endDate() == null) {
            throw badRequest("시작일과 종료일을 입력해주세요.");
        }

        if (request.endDate().isBefore(
                request.startDate().plusDays(7)
        )) {
            throw badRequest(
                    "종료일은 시작일로부터 최소 7일 이후로 선택해주세요."
            );
        }
    }

    // ==================================================
    // 기록 입력 검사
    // ==================================================

    private void validateRecord(
            ActivityGoal goal,
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

        LocalDate recordDate = request.recordedAt().toLocalDate();

        if (recordDate.isBefore(goal.getStartDate())
                || recordDate.isAfter(goal.getEndDate())) {
            throw badRequest("목표 기간 안의 운동 기록만 저장할 수 있습니다.");
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
            throw badRequest("걸음 수는 0~1000000 사이로 입력해주세요.");
        }
    }

    private void validateDecimal(
            BigDecimal value,
            String label,
            BigDecimal maximum,
            int scale
    ) {
        if (value == null || value.signum() < 0) {
            throw badRequest(label + "은 0 이상의 숫자로 입력해주세요.");
        }

        if (value.compareTo(maximum) > 0
                || value.stripTrailingZeros().scale() > scale) {
            throw badRequest(
                    label + "의 값 또는 소수점 자릿수를 확인해주세요."
            );
        }
    }

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

    private static class DailyTotal {
        private BigDecimal distance = BigDecimal.ZERO;
        private BigDecimal calories = BigDecimal.ZERO;
        private long seconds;
        private long steps;
    }
}