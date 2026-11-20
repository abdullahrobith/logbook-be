package com.logbook.config;

import com.logbook.interceptor.CsrfInterceptor;
import com.logbook.interceptor.PasswordChangeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CsrfInterceptor())
                .addPathPatterns("/api/**")
                .order(1);

        registry.addInterceptor(new PasswordChangeInterceptor())
                .addPathPatterns("/api/**")
                .order(2);
    }
}
