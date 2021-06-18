package com.dc.videojc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>Descriptions...
 *
 * @author Diamon.Shen
 * @date 2020/9/23.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public WebContextBinder webContextBinder() {
        return new WebContextBinder();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webContextBinder());
    }
}
