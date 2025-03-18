package com.ailoganalyzer.loganalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @Autowired
    public HealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/system-health")
    public String healthDashboard(Model model) {
        HealthComponent health = healthEndpoint.health();
        model.addAttribute("overallStatus", health.getStatus().getCode());
        
        Map<String, Object> componentStatuses = new HashMap<>();
        if (health instanceof CompositeHealth) {
            CompositeHealth compositeHealth = (CompositeHealth) health;
            compositeHealth.getComponents().forEach((name, component) -> {
                componentStatuses.put(name, component.getStatus().getCode());
            });
        }
        
        model.addAttribute("componentStatuses", componentStatuses);
        return "health-dashboard";
    }
    
    @GetMapping(value = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> healthJson() {
        HealthComponent health = healthEndpoint.health();
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", health.getStatus().getCode());
        
        Map<String, Object> components = new HashMap<>();
        if (health instanceof CompositeHealth) {
            CompositeHealth compositeHealth = (CompositeHealth) health;
            compositeHealth.getComponents().forEach((name, component) -> {
                components.put(name, mapComponentToJson(component));
            });
        }
        
        result.put("components", components);
        return result;
    }
    
    private Map<String, Object> mapComponentToJson(HealthComponent component) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", component.getStatus().getCode());
        
        if (component instanceof Health) {
            Health health = (Health) component;
            result.put("details", health.getDetails());
        } else if (component instanceof CompositeHealth) {
            CompositeHealth compositeHealth = (CompositeHealth) component;
            Map<String, Object> subComponents = new HashMap<>();
            compositeHealth.getComponents().forEach((name, subComponent) -> {
                subComponents.put(name, mapComponentToJson(subComponent));
            });
            result.put("components", subComponents);
        }
        
        return result;
    }
} 