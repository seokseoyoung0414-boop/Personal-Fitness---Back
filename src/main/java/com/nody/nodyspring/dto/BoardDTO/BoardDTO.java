package com.nody.nodyspring.dto.BoardDTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class BoardDTO {
    private Integer idx;
    private String title;
    private String contents;
    private String writer;  // User의 userid (외래키)
    private int viewcount;
    private LocalDateTime regdate;

    public BoardDTO(Integer idx, String title, String contents, String writer, int viewcount, LocalDateTime regdate) {
        this.idx = idx;
        this.title = title;
        this.contents = contents;
        this.writer = writer;
        this.viewcount = viewcount;
        this.regdate = regdate;
    }
}
