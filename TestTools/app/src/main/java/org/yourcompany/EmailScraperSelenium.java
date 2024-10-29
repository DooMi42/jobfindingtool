package org.yourcompany;

// EmailScraperSelenium.java
import io.github.bonigarcia.wdm.WebDriverManager;
import com.opencsv.CSVWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.support.ui.*;

import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailScraperSelenium {
    public static void main(String[] args) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless"); // Run in headless mode for performance
        WebDriver driver = new FirefoxDriver(options);

        String mainUrl = "https://www.kauppalehti.fi/yritykset/toimialat/ohjelmistojen-suunnittelu-ja-valmistus/62010?page=";
        Set<String> companyUrls = new HashSet<>();
        Map<String, Set<String>> companyEmails = new HashMap<>();
        int totalPages = 840; // Adjust as needed

        try {
            // Remove implicit wait
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);

            // Step 1: Collect URLs from each main page
            for (int page = 1; page <= totalPages; page++) {
                String pageUrl = mainUrl + page;
                driver.get(pageUrl);

                // Wait for company links to be present
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("a.sc-pek81u-0.eUvIBz")));

                // Collect company links on the main page
                List<WebElement> companyLinks = driver.findElements(By.cssSelector("a.sc-pek81u-0.eUvIBz"));
                for (WebElement link : companyLinks) {
                    String companyUrl = link.getAttribute("href");
                    if (companyUrl != null && companyUrl.contains("/yritykset/yritys")) {
                        companyUrls.add(companyUrl);
                    }
                }
            }

            // Step 2: Visit each company's subpage to extract emails
            for (String companyUrl : companyUrls) {
                driver.get(companyUrl);

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));

                // Wait for page to load completely
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

                // Extract company name using the provided XPath
                String companyName = "";
                try {
                    companyName = wait.until(ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//main/header/div/h1"))).getText()
                            .trim();
                    System.out.println("Company name found: " + companyName);
                } catch (Exception e) {
                    System.out.println("Company name not found for: " + companyName);
                    continue; // Skip to next company if name not found
                }

                // Extract email if available using targeted XPath
                Set<String> emails = new HashSet<>();
                try {
                    // Locate any text node containing "Sähköposti:" and the following sibling span
                    List<WebElement> emailElements = driver
                            .findElements(By.xpath("//*[contains(text(), 'Sähköposti:')]/following-sibling::span"));
                    for (WebElement emailElement : emailElements) {
                        String emailText = emailElement.getText().trim();
                        if (emailText.contains("@")) {
                            emails.add(emailText);
                            System.out.println("Email for: " + companyName + " was found: " + emailText);
                        }
                    }

                    // Backup check using regex within the specific section if no email was found
                    if (emails.isEmpty()) {
                        WebElement businessSection = driver.findElement(By.cssSelector("section.sc-1mjuqxu-0"));
                        String sectionText = businessSection.getText();
                        Matcher emailMatcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-z]{2,}")
                                .matcher(sectionText);
                        while (emailMatcher.find()) {
                            emails.add(emailMatcher.group());
                            System.out.println("Email for: " + companyName + " was found: " + emailMatcher.group());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Email not found for " + companyName + " at URL: " + companyUrl);
                }

                if (!emails.isEmpty()) {
                    companyEmails.put(companyName, emails);
                }
            }

            // Step 3: Write emails to CSV
            writeCompanyEmailsToCSV(companyEmails);

            System.out.println("Successfully extracted email addresses from " + companyEmails.size() + " companies.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    public static void writeCompanyEmailsToCSV(Map<String, Set<String>> companyEmails) {
        // Define the folder path and file name
        String folderPath = Paths.get("app\\src\\main\\resources").toString();
        String fileName = "emails.csv";

        // Create the directory if it doesn't exist
        File directory = new File(folderPath);
        if (!directory.exists()) {
            directory.mkdirs(); // Create the 'resources' folder if it doesn't exist
        }

        // Create the CSV file in the specified folder
        String csvFilePath = Paths.get(folderPath, fileName).toString();

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath))) {
            writer.writeNext(new String[] { "Company Name", "Email" });
            for (Map.Entry<String, Set<String>> entry : companyEmails.entrySet()) {
                String companyName = entry.getKey();
                for (String email : entry.getValue()) {
                    writer.writeNext(new String[] { companyName, email });
                }
            }
            System.out.println("CSV file created at: " + csvFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
