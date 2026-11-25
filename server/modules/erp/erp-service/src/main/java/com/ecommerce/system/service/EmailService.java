package com.ecommerce.system.service;

import com.resend.core.exception.ResendException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface EmailService {

    String sendEmail(String from, List<String> to, String subject, String content) throws ResendException;

    void sendEmailWithAttachment(String to, String subject, String content, String attachmentPath);

    void sendEmailWithAttachments(String to, String subject, String content, String[] attachmentPaths);

    void sendEmailWithInlineAttachment(String to, String subject, String content, String inlineAttachmentPath);

    void registerUser(String to, String subject, String content);

    void resetPassword(String to, String subject, String content);

}
