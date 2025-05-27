package com.duydk.ttcs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping("/home")
    public String showHome(Model model) {
        return "home";
    }

    @GetMapping("/")
    public String redirectToHome(Model model) {
        return "redirect:/home";
    }

}
