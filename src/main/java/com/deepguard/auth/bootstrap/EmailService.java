package com.deepguard.auth.bootstrap;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendOtp(String emailTo, String otp) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

        mimeMessageHelper.setTo(emailTo);
        mimeMessageHelper.setSubject("Verify your account");
        mimeMessageHelper.setText(
                """
                <h2>Email Verification</h2>
                <p>Your OTP is:</p>
                <h1 style='color:blue'>%s</h1>
                """.formatted(otp),
                true
        );
        javaMailSender.send(mimeMessage);
    }

}
