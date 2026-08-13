package com.nody.nodyspring.controller;

import com.nody.nodyspring.dto.BotDTO.ArticleDTO;
import com.nody.nodyspring.service.BotService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bot")
public class BotController {

    private final BotService botService;


    @Autowired
    public BotController(BotService botService) {
        this.botService = botService;
    }

    @GetMapping("/healthy")
    public List<ArticleDTO> getHealthyLifeNews() {
        return botService.crawlHealthyLife();
    }

    @PostMapping("/weather")
    public ResponseEntity<?> handleCommand(@RequestBody Map<String, String> command, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(botService.processCommand(command, httpRequest));
    }
}