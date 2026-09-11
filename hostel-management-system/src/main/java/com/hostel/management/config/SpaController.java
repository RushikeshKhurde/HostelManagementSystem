package com.hostel.management.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/login",
        "/register",
        "/profile",
        "/admin/**",
        "/student/**",
        "/warden/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}