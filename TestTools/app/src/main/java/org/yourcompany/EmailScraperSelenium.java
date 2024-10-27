package org.yourcompany;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailScraperSelenium {
    public static void main(String[] args) {
        // Use WebDriverManager to handle driver setup
        WebDriverManager.chromedriver().setup();

        // Set Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode if GUI is not needed
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver(options);

        String mainUrl = "https://www.kauppalehti.fi/yritykset/toimialat/ohjelmistojen-suunnittelu-ja-valmistus/62010?page=";
        Set<String> companyUrls = new HashSet<>();
        Map<String, Set<String>> companyEmails = new HashMap<>();
        int totalPages = 5; // Update this based on the actual number of pages

        try {
            // Loop through all pages to collect company URLs
            for (int page = 1; page <= totalPages; page++) {
                String pageUrl = mainUrl + page;
                driver.get(pageUrl);

                // Wait for the page to load
                Thread.sleep(2000);

                // Update the selector based on the actual HTML structure
                List<WebElement> companyLinks = driver.findElements(By.cssSelector("a.yl-yl-company-name")); // Update
                                                                                                             // selector

                for (WebElement link : companyLinks) {
                    String companyUrl = link.getAttribute("href");
                    companyUrls.add(companyUrl);
                }

                // Be polite
                Thread.sleep(1000);
            }

            // Visit each company's subpage to extract emails
            for (String companyUrl : companyUrls) {
                driver.get(companyUrl);

                // Wait for the page to load
                Thread.sleep(2000);

                // Extract company name
                String companyName = driver.findElement(By.cssSelector("h1.yl-h1")).getText(); // Update selector

                // Get page source for email extraction
                String pageSource = driver.getPageSource();

                // Extract emails from page source
                Set<String> emails = extractEmails(pageSource);

                if (!emails.isEmpty()) {
                    companyEmails.put(companyName, emails);
                }

                // Be polite
                Thread.sleep(1000);
            }

            // Write collected emails to CSV
            writeCompanyEmailsToCSV(companyEmails);

            System.out.println("Successfully extracted email addresses from " + companyEmails.size() + " companies.");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }

    public static Set<String> extractEmails(String text) {
        Set<String> emails = new HashSet<>();
        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            emails.add(matcher.group());
        }

        return emails;
    }

    public static void writeCompanyEmailsToCSV(Map<String, Set<String>> companyEmails) {
        String csvFile = "emails.csv";

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            // Write header
            String[] header = { "Company Name", "Email" };
            writer.writeNext(header);

            // Write data
            for (Map.Entry<String, Set<String>> entry : companyEmails.entrySet()) {
                String companyName = entry.getKey();
                for (String email : entry.getValue()) {
                    String[] data = { companyName, email };
                    writer.writeNext(data);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
