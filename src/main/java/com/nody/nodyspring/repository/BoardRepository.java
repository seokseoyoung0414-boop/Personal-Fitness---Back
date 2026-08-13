package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Integer> {

    // 사용자 ID로 게시글 제목과 idx만 추출
    @Query("SELECT b.idx, b.title, b.contents, b.writer.userid, b.viewcount, b.regdate " +
            "FROM Board b WHERE b.writer.userid = :userid ORDER BY b.regdate DESC")
    List<Object[]> findAllFieldsByWriterId(String userid);

    @Query("SELECT b.title FROM Board b WHERE b.idx = :id")
    String findTitleById(@Param("id") int id);
}
