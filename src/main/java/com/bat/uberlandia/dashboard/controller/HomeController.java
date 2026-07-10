package com.bat.uberlandia.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {


    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("nome", "SENAI");
        model.addAttribute("empresa", "BAT Brasil");
        model.addAttribute("ano", 2026);
        model.addAttribute("cidade", "Uberlandia");
        return "hello";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
