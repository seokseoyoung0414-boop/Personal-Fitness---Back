package com.nody.nodyspring.dto.CommentDTO;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDTO {
    private Integer idx;
    private String writeby; // 유저 ID만 전달
    private String texts;
    private LocalDateTime times;
}
