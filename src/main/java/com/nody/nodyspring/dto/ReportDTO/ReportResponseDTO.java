package com.nody.nodyspring.dto.ReportDTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponseDTO {
    private int idx;
    private String targetType;
    private int targetId;
    private String targetInfo; // 예: "게시글 제목 (id: 12)"
    private String reporter;
    private String reason;
    private String reportDate;
}
