package org.yourcompany;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {
        // Email server configuration
        String smtpServer = "";
        String smtpPort = "587"; // Use "465" for SSL
        final String username = "";
        final String password = "";

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // Use "mail.smtp.ssl.enable" for SSL
        properties.put("mail.smtp.host", smtpServer);
        properties.put("mail.smtp.port", smtpPort);

        // Create a session with an authenticator
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Read the CSV file
        List<String[]> data = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(
                "email_list.csv"))) {
            data = reader.readAll();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Check if data is not empty
        if (data.isEmpty()) {
            System.out.println("The CSV file is empty or not found.");
            return;
        }

        // Get the indices of relevant columns
        String[] headers = data.get(0);
        int emailIndex = -1;
        int nameIndex = -1;

        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equalsIgnoreCase("email")) {
                emailIndex = i;
            } else if (headers[i].equalsIgnoreCase("name")) {
                nameIndex = i;
            }
        }

        if (emailIndex == -1 || nameIndex == -1) {
            System.out.println("CSV file must contain 'email' and 'name' columns.");
            return;
        }

        // Email template
        String emailTemplate = "Dear {name},\n\nThis is a personalized message for you.\n\nBest regards,\nYour Company";

        // Send emails
        for (int i = 1; i < data.size(); i++) { // Start from 1 to skip headers
            String[] row = data.get(i);
            String recipientEmail = row[emailIndex].trim();
            String recipientName = row[nameIndex].trim();

            String personalizedEmail = emailTemplate.replace("{name}", recipientName);

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username));
                message.setRecipients(
                        Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject("Job Application");
                message.setText(personalizedEmail);

                Transport.send(message);

                System.out.println("Email sent to " + recipientEmail);

                // Throttle emails
                TimeUnit.SECONDS.sleep(1);

            } catch (Exception e) {
                System.out.println("Failed to send email to " + recipientEmail + ": " + e.getMessage());
            }
        }
    }
}
