package com.ailoganalyzer.loganalyzer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SubscriptionTestController {

    @GetMapping("/subscription-test")
    public String getSubscriptionTestPage() {
        return "redirect:/graphql-subscription.html";
    }
} 