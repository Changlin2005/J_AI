package com.lcl.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射音频目录，禁用缓存
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("classpath:/static/audio/")
                // 核心：设置缓存为0，强制每次请求都获取最新文件
                .setCacheControl(CacheControl.noCache()
                        .mustRevalidate()
                        .noStore());
    }
}