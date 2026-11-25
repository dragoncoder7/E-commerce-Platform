package com.ecommerce.system.controller;

import com.ecommerce.system.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
public class EmailController {



    @Autowired
    private EmailService emailService;

    public static void main(String[] args) {
        EmailController EmailController = new EmailController();
        System.out.println();
    }



    // todo sendEmail
    public void sendEmail() {

    }
}
