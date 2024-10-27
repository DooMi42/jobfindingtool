package org.yourcompany;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

import java.util.Date;

public class EmailReceiver {
    public static void main(String[] args) {
        // Email account credentials
        final String username = "";
        final String password = "";

        // IMAP server configuration
        String host = ""; // e.g., imap.gmail.com
        String port = "993"; // Default port for IMAP over SSL

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", host);
        properties.put("mail.imaps.port", port);
        properties.put("mail.imaps.ssl.enable", "true");

        try {
            // Get the session object
            Session emailSession = Session.getDefaultInstance(properties);

            // Create the IMAP store object and connect to the email server
            Store store = emailSession.getStore("imaps");
            store.connect(host, username, password);

            // Open the inbox folder
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);

            // Retrieve messages from the folder
            Message[] messages = emailFolder.getMessages();

            System.out.println("Total Messages: " + messages.length);

            // Process messages
            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];
                String subject = message.getSubject();
                Address[] fromAddresses = message.getFrom();

                // Check if the email is a reply to your sent email
                if (subject != null && subject.contains("Job Application")) {
                    System.out.println("---------------------------------");
                    System.out.println("Email Number: " + (i + 1));
                    System.out.println("Subject: " + subject);
                    System.out.println("From: " + fromAddresses[0].toString());
                    System.out.println("Received Date: " + message.getReceivedDate());

                    // Get the email content
                    String content = Utils.getTextFromMessage(message);
                    System.out.println("Content: \n" + content);

                    // TODO: Process and store the data as needed

                    // Mark the email as seen
                    message.setFlag(Flags.Flag.SEEN, true);
                }
            }

            // Close connections
            emailFolder.close(false);
            store.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
