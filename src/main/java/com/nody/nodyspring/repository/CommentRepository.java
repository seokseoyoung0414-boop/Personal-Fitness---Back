package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByBoardby_IdxOrderByTimesAsc(Integer boardIdx);

    @Query("SELECT CONCAT(c.texts, ' (게시글 ID: ', c.boardby, ')') FROM Comment c WHERE c.idx = :id")
    String findCommentWithPostId(@Param("id") int id);
}