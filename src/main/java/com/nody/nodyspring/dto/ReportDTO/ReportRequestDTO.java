package com.nody.nodyspring.dto.ReportDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDTO {
    private Integer targetId;
    private String reason;
}
