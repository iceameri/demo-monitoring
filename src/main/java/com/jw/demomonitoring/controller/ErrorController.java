package com.jw.demomonitoring.controller;

import jdk.jfr.Registered;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorController {

    @GetMapping("/error-test")
    public String error() {
        throw new RuntimeException("test");
    }
}
