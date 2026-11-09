package com.logbook.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component // <--- INI WAJIB ADA BIAR SPRING BOOT OTOMATIS JALANIN
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Bikin ID unik
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        // Masukin ke MDC
        MDC.put("requestId", requestId);
        
        // (Opsional) Log buat mastiin filter ini jalan
        System.out.println("=== FILTER JALAN! ReqID: " + requestId + " ===");

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}