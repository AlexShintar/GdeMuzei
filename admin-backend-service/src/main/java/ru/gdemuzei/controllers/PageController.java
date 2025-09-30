package ru.gdemuzei.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    /**
     * Перенаправляет пользователя с корневого URL ("/") на основную страницу
     * приложения со списком музеев ("/museums").
     *
     * @return строка, указывающая на редирект.
     */
    @GetMapping("/")
    public String mainPageRedirect() {
        return "redirect:/museums";
    }
}
