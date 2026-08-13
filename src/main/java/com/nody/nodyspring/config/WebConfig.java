package com.nody.nodyspring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//전역 CORS 설정을 담당하는 Java 설정 클래스
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //origin 설정
        String[] allowedOrigins = {
                "http://localhost:5173",
                "https://localhost:5173",
                "https://port-0-nody-back-may14pnqe9e9e164.sel4.cloudtype.app",
                "https://www.rubia.shop",
                "http://www.rubia.shop",
                "https://rubia.shop",
                "http://rubia.shop",
                "http://43.200.54.76",
                "https://43.200.54.76",
                "capacitor://localhost",
                "http://localhost",
                "https://localhost",
                "http://10.0.2.2",
                "https://10.0.2.2",
                "file://"
        };

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
