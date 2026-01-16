package br.com.vibetex.domain.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String supportTo;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.support-to}") String supportTo
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.supportTo = supportTo;
    }

    public void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }

    public void notifySupportNewTicket(String subject, String html) {
        sendHtml(supportTo, subject, html);
    }

    public void notifyUserStatusChange(String userEmail, String subject, String html) {
        if (userEmail == null || userEmail.isBlank()) return; // não quebra fluxo
        sendHtml(userEmail, subject, html);
    }
}
