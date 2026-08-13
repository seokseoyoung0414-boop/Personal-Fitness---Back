package com.nody.nodyspring.service;

import com.nody.nodyspring.DB.Report;
import com.nody.nodyspring.DB.ReportType;
import com.nody.nodyspring.DB.User;
import com.nody.nodyspring.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private BoardRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ChannelRepository channelRepository;
    @Autowired
    private UserRepository userRepository;

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public String findPostTitleById(int id) {
        return postRepository.findTitleById(id); // 직접 title만 조회하도록 쿼리 정의
    }

    public String findCommentContentAndPostId(int commentId) {
        // 예: "댓글내용 (게시글 ID: 12)"
        return commentRepository.findCommentWithPostId(commentId);
    }

    public String findChannelTitleById(int id) {
        return channelRepository.findTitleById(id);
    }

    public void saveReport(ReportType type, Integer targetId, String reason, String userid) {
        User user = userRepository.findByUserid(userid)
                .orElseThrow(() -> new RuntimeException("신고자 정보 없음"));

        Report report = Report.builder()
                .targetType(type)
                .targetId(targetId)
                .reason(reason)
                .reporter(user)
                .reportDate(LocalDateTime.now())
                .build();

        reportRepository.save(report);
    }

    @Transactional
    public void reject(Integer reportId) {
        reportRepository.deleteById(reportId);
    }

    @Transactional
    public void deleteWithTarget(Integer reportId) {
        Report rpt = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 없음"));

        switch (rpt.getTargetType()) {
            case post    -> postRepository.deleteById(rpt.getTargetId());    // boards ON DELETE CASCADE → comments 자동삭제
            case comment -> commentRepository.deleteById(rpt.getTargetId());
            case channel -> channelRepository.deleteById(rpt.getTargetId());
        }
        reportRepository.deleteById(reportId);  // 마지막에 신고 레코드 제거
    }
}