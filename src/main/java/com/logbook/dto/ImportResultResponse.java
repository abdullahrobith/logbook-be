package com.logbook.dto;

public class ImportResultResponse {
    private String email;
    private String status;
    private String message;

    public ImportResultResponse(String email, String status, String message) {
        this.email = email;
        this.status = status;
        this.message = message;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}