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
    private UserRepository userRepository;

    // 전체 신고 목록
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    // 게시글 제목 조회
    public String findPostTitleById(int id) {
        return postRepository.findTitleById(id);
    }

    // 댓글 내용 및 게시글 ID 조회
    public String findCommentContentAndPostId(int commentId) {
        return commentRepository.findCommentWithPostId(commentId);
    }

    // 신고 저장 (게시글 / 댓글)
    public void saveReport(
            ReportType type,
            Integer targetId,
            String reason,
            String userid
    ) {
        if (type != ReportType.post && type != ReportType.comment) {
            throw new IllegalArgumentException("게시글 또는 댓글만 신고할 수 있습니다.");
        }

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

    // 신고 반려
    @Transactional
    public void reject(Integer reportId) {
        reportRepository.deleteById(reportId);
    }

    // 신고 대상 삭제
    @Transactional
    public void deleteWithTarget(Integer reportId) {

        Report rpt = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("신고 없음"));

        switch (rpt.getTargetType()) {

            case post -> postRepository.deleteById(rpt.getTargetId());

            case comment -> commentRepository.deleteById(rpt.getTargetId());

        }

        // 신고 레코드 제거
        reportRepository.deleteById(reportId);
    }
}