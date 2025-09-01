package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class testController {
    @GetMapping("/hello")
    public String sayHello(@RequestParam(value = "name", defaultValue = "World") String name) {
        testService.savePerson();
        return "Hello, " + name + "!";
    }
    private final TestService testService;
    public testController(TestService testService) {
        this.testService = testService;
    }
}
