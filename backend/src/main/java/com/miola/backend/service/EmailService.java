package com.miola.backend.service;

import static com.miola.backend.constant.EmailConstant.CC_EMAIL;
import static com.miola.backend.constant.EmailConstant.DEFAULT_PORT;
import static com.miola.backend.constant.EmailConstant.EMAIL_SUBJECT;
import static com.miola.backend.constant.EmailConstant.FROM_EMAIL;
import static com.miola.backend.constant.EmailConstant.GMAIL_SMTP_SERVER;
import static com.miola.backend.constant.EmailConstant.PASSWORD;
import static com.miola.backend.constant.EmailConstant.SIMPLE_MAIL_TRANSFER_PROTOCOL;
import static com.miola.backend.constant.EmailConstant.SMTP_AUTH;
import static com.miola.backend.constant.EmailConstant.SMTP_HOST;
import static com.miola.backend.constant.EmailConstant.SMTP_PORT;
import static com.miola.backend.constant.EmailConstant.SMTP_STARTTLS_ENABLE;
import static com.miola.backend.constant.EmailConstant.SMTP_STARTTLS_REQUIRED;
import static com.miola.backend.constant.EmailConstant.USERNAME;

import com.sun.mail.smtp.SMTPTransport;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendNewPasswordEmail(String firstname, String password, String email)
            throws MessagingException {
        Message message = createEmail(firstname, password, email);
        SMTPTransport smtpTransport = (SMTPTransport) getEmailSession().getTransport(SIMPLE_MAIL_TRANSFER_PROTOCOL);
        smtpTransport.connect(GMAIL_SMTP_SERVER, USERNAME, PASSWORD);
        smtpTransport.sendMessage(message, message.getAllRecipients());
        smtpTransport.close();
    }

    public Session getEmailSession() {
        Properties properties = System.getProperties();
        properties.put(SMTP_HOST, GMAIL_SMTP_SERVER);
        properties.put(SMTP_AUTH, true);
        properties.put(SMTP_PORT, DEFAULT_PORT);
        properties.put(SMTP_STARTTLS_ENABLE, true);
        properties.put(SMTP_STARTTLS_REQUIRED, true);
        return Session.getInstance(properties, null);
    }

    private Message createEmail(String firstname, String password, String email)
            throws MessagingException {
        Message message = new MimeMessage(getEmailSession());
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(RecipientType.TO, InternetAddress.parse(email, false));
        message.setRecipients(RecipientType.CC, InternetAddress.parse(CC_EMAIL, false));
        message.setSubject(EMAIL_SUBJECT);
        message.setText("Hello " + firstname + ", \n \n Your new account password is " + password + "\n \n The Support Team");
        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }
}
