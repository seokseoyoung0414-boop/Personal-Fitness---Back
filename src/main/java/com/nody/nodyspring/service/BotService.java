package com.nody.nodyspring.service;

import com.nody.nodyspring.dto.BotDTO.ArticleDTO;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Service
@RequiredArgsConstructor
public class BotService {

    @Value("${weather.api-key}")
    private String weatherApiKey;

    private final RestTemplate restTemplate;

    public List<ArticleDTO> crawlHealthyLife() {
        String url = "https://news.hidoc.co.kr/news/articleList.html?sc_section_code=S1N4&view_type=sm";
        List<ArticleDTO> items = new ArrayList<>();

        try {
            // Jsoup이 직접 HTML 다운로드 & 파싱
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; Bot/1.0)") // 선택
                    .timeout(5_000)                                   // 선택
                    .get();

            Elements articleEls = doc.select("ul.type2 > li");
            int num = 1;

            for (Element article : articleEls) {
                Element titleLink = article.selectFirst("div.view-cont h2.titles > a");
                if (titleLink == null) continue; // null-safe

                String headline = titleLink.text().trim();
                String href     = titleLink.absUrl("href"); // 절대경로로 변환
                String date     = article.select("div.byline > div:last-of-type")
                        .text().trim();

                items.add(new ArticleDTO(num++, headline, href, date, "조회수 정보 없음"));
            }

        } catch (IOException e) {
            // 필요하면 로깅으로 교체
            e.printStackTrace();
        }

        return items;
    }

    public Map<String, Object> processCommand(Map<String, String> commandMap, HttpServletRequest req) {
        String command = commandMap.get("command");

        try {
            switch (command) {
                case "날씨":
                    return getWeather(req);
                case "??":
                    return getTime();
                default:
                    return Map.of("message", "❓ 알 수 없는 명령어입니다.");
            }
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Map<String, Object> getWeather(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = req.getRemoteAddr();
        }

        String geoUrl = "http://ip-api.com/json/" + ip;
        Map geoData = restTemplate.getForObject(geoUrl, Map.class);
        if (geoData == null || !"success".equals(geoData.get("status"))) {
            throw new RuntimeException("위치 정보를 가져올 수 없습니다.");
        }

        double lat = (Double) geoData.get("lat");
        double lon = (Double) geoData.get("lon");

        String weatherUrl = String.format(
                "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&lang=kr&units=metric&appid=%s",
                lat, lon, weatherApiKey
        );

        Map weatherData = restTemplate.getForObject(weatherUrl, Map.class);
        Map main = (Map) weatherData.get("main");

        return Map.of(
                "temp", main.get("temp"),
                "feels_like", main.get("feels_like"),
                "temp_min", main.get("temp_min"),
                "temp_max", main.get("temp_max"),
                "humidity", main.get("humidity"),
                "pressure", main.get("pressure")
        );
    }

    private Map<String, Object> getTime() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return Map.of("time", now);
    }
}
