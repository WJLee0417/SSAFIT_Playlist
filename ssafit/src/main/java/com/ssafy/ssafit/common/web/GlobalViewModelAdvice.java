package com.ssafy.ssafit.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModelAdvice {

    private final String frontendOrigin;

    public GlobalViewModelAdvice(
            @Value("${ssafit.frontend.origin}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @ModelAttribute("frontendOrigin")
    public String frontendOrigin() {
        return frontendOrigin;
    }
}
