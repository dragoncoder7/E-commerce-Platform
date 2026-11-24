package com.ecommerce.system.service.impl;

import com.ecommerce.system.service.EmailService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private Resend resend;


    /**
     * Send an email
     *
     * @param from The email address of the sender
     * @param to The email address of the recipient
     * @param subject The subject of the email
     * @param content The content of the email
     * @throws ResendException if the email fails to send
     */
    @Override
    public String sendEmail(String from, List<String> to, String subject, String content) throws ResendException {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .html(content)
                .build();

        CreateEmailResponse data = resend.emails().send(params);
        log.info("Email sent successfully: {}", data.getId());
        return data.getId(); // Return the email ID
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String content, String attachmentPath) {

    }

    @Override
    public void sendEmailWithAttachments(String to, String subject, String content, String[] attachmentPaths) {

    }

    @Override
    public void sendEmailWithInlineAttachment(String to, String subject, String content, String inlineAttachmentPath) {

    }

    @Override
    public void registerUser(String to, String subject, String content) {

    }

    @Override
    public void resetPassword(String to, String subject, String content) {

    }
}
