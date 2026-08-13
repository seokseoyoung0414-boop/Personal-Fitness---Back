package com.nody.nodyspring.dto.BotDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {
    private int num;
    private String headline;
    private String url;
    private String date;
    private String views;
}