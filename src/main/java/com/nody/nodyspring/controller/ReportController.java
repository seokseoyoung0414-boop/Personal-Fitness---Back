package com.nody.nodyspring.controller;

import com.nody.nodyspring.DB.Report;
import com.nody.nodyspring.DB.ReportType;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.config.JwtConfig;
import com.nody.nodyspring.dto.ReportDTO.ReportRequestDTO;
import com.nody.nodyspring.dto.ReportDTO.ReportResponseDTO;
import com.nody.nodyspring.repository.UserRepository;
import com.nody.nodyspring.service.ReportService;
import com.nody.nodyspring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private JwtConfig jwtConfig;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;


    @GetMapping("/get")
    public List<ReportResponseDTO> getAllReports() {
        List<Report> reports = reportService.getAllReports();
        List<ReportResponseDTO> responseList = new ArrayList<>();

        for (Report report : reports) {
            String info = "";
            switch (report.getTargetType()) {
                case post -> {
                    String title = reportService.findPostTitleById(report.getTargetId());
                    info = title != null ? title + " (id: " + report.getTargetId() + ")" : "(게시글 없음)";
                }
                case comment -> {
                    String content = reportService.findCommentContentAndPostId(report.getTargetId());
                    info = content != null ? content : "(댓글 없음)";
                }
                case channel -> {
                    String title = reportService.findChannelTitleById(report.getTargetId());
                    info = title != null ? title + " (id: " + report.getTargetId() + ")" : "(채널 없음)";
                }
            }

            responseList.add(new ReportResponseDTO(
                    report.getIdx(),
                    report.getTargetType().name(),          // ReportType → String
                    report.getTargetId(),
                    info,
                    report.getReporter().getUserid(),       // User → ID 문자열
                    report.getReason(),
                    report.getReportDate().toString()
            ));
        }

        return responseList;
    }

    @PostMapping("/post")
    public ResponseEntity<?> reportPost(@RequestBody ReportRequestDTO dto,
                                        @CookieValue(name = "token", required = false) String token) {
        String userid = jwtConfig.getUserIdFromToken(token); // 유저 인증
        reportService.saveReport(ReportType.post, dto.getTargetId(), dto.getReason(), userid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comment")
    public ResponseEntity<?> reportComment(@RequestBody ReportRequestDTO dto,
                                           @CookieValue(name = "token", required = false) String token) {
        String userid = jwtConfig.getUserIdFromToken(token); // 유저 인증
        reportService.saveReport(ReportType.comment, dto.getTargetId(), dto.getReason(), userid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/channel")
    public ResponseEntity<?> reportChannel(@RequestBody ReportRequestDTO dto,
                                           @CookieValue(name = "token", required = false) String token) {
        String userid = jwtConfig.getUserIdFromToken(token);
        reportService.saveReport(ReportType.channel, dto.getTargetId(), dto.getReason(), userid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectReport(@PathVariable Integer id,
                                          @CookieValue(name = "token", required = false) String token) {
        String userid = jwtConfig.getUserIdFromToken(token);
        System.out.println("관리자 요청자 userid: " + userid);
        if (!"root".equals(userid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자만 수행 가능합니다.");
        }
        reportService.reject(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteReportedTarget(@PathVariable Integer id,
                                                  @CookieValue(name = "token", required = false) String token) {
        String userid = jwtConfig.getUserIdFromToken(token);
        System.out.println("관리자 요청자 userid: " + userid);
        if (!"root".equals(userid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자만 수행 가능합니다.");
        }
        reportService.deleteWithTarget(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ban")
    public ResponseEntity<?> banUser(@RequestBody BanRequest req,
                                     @CookieValue(name = "token", required = false) String token) {

        /* 1) 인증·권한 체크 */
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("토큰 없음");
        }
        String adminId = jwtConfig.getUserIdFromToken(token);
        if (!"root".equals(adminId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("관리자만 수행 가능합니다.");
        }

        /* 2) 입력값 검증 (days 1~14 또는 -1) */
        if (req.days() != -1 && (req.days() < 1 || req.days() > 14)) {
            return ResponseEntity.badRequest().body("days 값은 1~14 또는 -1(무기한)만 허용");
        }

        /* 3) 대상 유저 조회 */
        User user = userService.findByUserid(req.userid());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 유저가 존재하지 않습니다.");
        }

        /* 4) 제재 기간 계산 */
        LocalDateTime until = req.days() == -1
                ? LocalDateTime.of(9999, 12, 31, 23, 59)
                : LocalDateTime.now().plusDays(req.days());

        /* 5) 저장 */
        user.setBannedUntil(until);
        userRepository.save(user);

        /* 6) 응답 */
        return ResponseEntity.ok(Map.of(
                "userid", user.getUserid(),
                "bannedUntil", until
        ));
    }
    public record BanRequest(String userid, int days) { } // days: 1~14, -1(무기한)
}