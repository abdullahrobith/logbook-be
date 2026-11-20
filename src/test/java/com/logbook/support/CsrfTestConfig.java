package com.logbook.support;

import com.logbook.interceptor.CsrfInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Menambahkan header CSRF pada request MockMvc dari test integrasi lama.
 *
 * <p>Test yang menguji LBK-AUT-04 sendiri tidak meng-import konfigurasi ini,
 * sehingga tetap bisa memverifikasi penolakan ketika header tidak dikirim.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class CsrfTestConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public OncePerRequestFilter csrfTestHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {

                if ("GET".equalsIgnoreCase(request.getMethod())
                        || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                filterChain.doFilter(new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if (CsrfInterceptor.HEADER_NAME.equalsIgnoreCase(name)
                                && super.getHeader(name) == null) {
                            return CsrfInterceptor.HEADER_VALUE;
                        }
                        return super.getHeader(name);
                    }
                }, response);
            }
        };
    }
}
