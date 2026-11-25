package com.ecommerce.system.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
public class Email {


    @Value("${spring.email.resend.apiKey}")
    private String apiKey;

    @Bean
    public Resend resend() {
        Assert.hasText(apiKey, "Configure resend.api-key in application-local.yml or set RESEND_API_KEY");
        return new Resend(apiKey);
    }
}
