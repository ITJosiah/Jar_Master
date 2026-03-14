package com.example.aling_jar.utils;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class GmailSender {

    private static final String SENDER_EMAIL = "asismichael143@gmail.com";
    private static final String SENDER_PASSWORD = "jybf fhlz qvoz ekgc";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }


    public static void sendVerificationEmail(String recipientEmail, String code,
                                              EmailCallback callback,
                                              android.app.Activity activity) {
        executor.execute(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "Aling Jar"));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
                message.setSubject("Your Aling Jar Verification Code");


                String htmlBody = "<!DOCTYPE html>"
                        + "<html>"
                        + "<head><meta charset='UTF-8'></head>"
                        + "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;'>"
                        + "<div style='max-width: 480px; margin: 30px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);'>"
                        + "  <div style='background-color: #2E7D32; padding: 24px; text-align: center;'>"
                        + "    <h1 style='color: #ffffff; margin: 0; font-size: 22px;'>Aling Jar</h1>"
                        + "  </div>"
                        + "  <div style='padding: 32px 24px; text-align: center;'>"
                        + "    <h2 style='color: #333333; margin-top: 0;'>Email Verification</h2>"
                        + "    <p style='color: #666666; font-size: 14px; line-height: 1.6;'>"
                        + "      Use the verification code below to complete your sign-up. "
                        + "      This code is valid for 10 minutes.</p>"
                        + "    <div style='background-color: #E8F5E9; border-radius: 8px; padding: 20px; margin: 24px 0;'>"
                        + "      <span style='font-size: 36px; font-weight: bold; letter-spacing: 12px; color: #2E7D32;'>"
                        + code
                        + "      </span>"
                        + "    </div>"
                        + "    <p style='color: #999999; font-size: 12px;'>"
                        + "      If you didn't request this code, please ignore this email.</p>"
                        + "  </div>"
                        + "  <div style='background-color: #f9f9f9; padding: 16px; text-align: center;'>"
                        + "    <p style='color: #aaaaaa; font-size: 11px; margin: 0;'>"
                        + "      &copy; 2026 Aling Jar. All rights reserved.</p>"
                        + "  </div>"
                        + "</div>"
                        + "</body></html>";

                message.setContent(htmlBody, "text/html; charset=UTF-8");

                Transport.send(message);

                // Callback on UI thread
                activity.runOnUiThread(callback::onSuccess);

            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Failed to send email";
                activity.runOnUiThread(() -> callback.onFailure(error));
            }
        });
    }
}
