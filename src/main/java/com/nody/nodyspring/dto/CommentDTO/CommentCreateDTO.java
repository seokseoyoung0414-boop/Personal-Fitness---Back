package com.nody.nodyspring.dto.CommentDTO;

import lombok.Getter;

@Getter
public class CommentCreateDTO {
    private Integer boardIdx;
    private String commentText;
}
