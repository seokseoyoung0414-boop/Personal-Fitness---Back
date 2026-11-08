package com.nody.nodyspring.dto.ChannelDTO;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelDTO {
    private Integer idx;
    private String channelTitle;
    private String sport;
    private String channelComment;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean complete;
    private Boolean channelBuild;
    private int goal;
}