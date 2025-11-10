// C:\project\103Team-sub\backend\src\main\java\com\team103\config\WebStaticConfig.java
package com.team103.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebStaticConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ./uploads/ 아래 파일을 /files/** 로 서빙
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:./uploads/");
    }
}
