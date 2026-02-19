import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class ElPaisScraper_locally {

    private static final Properties CONFIG = loadConfig();
    private static final String API_KEY = CONFIG.getProperty("rapidapi.key");
    private static final String API_HOST = CONFIG.getProperty("rapidapi.host");
    private static final String API_URL = CONFIG.getProperty("rapidapi.url");
    private static final int ARTICLE_COUNT = Integer.parseInt(CONFIG.getProperty("article.count", "5"));
    private static final String IMAGE_DIR = CONFIG.getProperty("image.directory", "downloads/images");

    private static final Logger LOGGER = Logger.getLogger(ElPaisScraper_locally.class.getName());
    private static final List<ArticleData> scrapedArticles = new ArrayList<>();
    private static int successCount = 0;
    private static int failureCount = 0;

    public static void main(String[] args) {
        //Setting up Logger
        setupLogger();

        LOGGER.info("=== El País Opinion Scraper Started ===\n");

        WebDriver driver = null;
        try {
            driver = initializeDriver();

            //Created File for storing images
            Files.createDirectories(Paths.get(IMAGE_DIR));

            navigateToOpinionSection(driver);
            List<WebElement> articles = findArticles(driver);

            LOGGER.info(String.format("Found %d articles. Processing first %d...\n",
                    articles.size(), ARTICLE_COUNT));

            List<String> translatedTitles = scrapeAndTranslateArticles(articles);

            analyzeWordFrequency(translatedTitles);

            generateReport();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                driver.quit();
                LOGGER.info("\nBrowser closed successfully");
            }
        }

        LOGGER.info(String.format("\n=== Scraper Completed ===\nSuccess: %d | Failures: %d", successCount, failureCount));
    }

    // Initializing the driver
    private static WebDriver initializeDriver() {
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        // Taking in consideration the load time of the browser wait for 10 sec
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        return driver;
    }

    // Opening the opinion section
    private static void navigateToOpinionSection(WebDriver driver) {
        driver.get("https://elpais.com/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            WebElement cookieButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("didomi-notice-agree-button"))
            );
            cookieButton.click();
        } catch (Exception e) {
            throw new RuntimeException("Cookie banner not Found", e);
        }

        try {
            WebElement opinionLink = wait.until(
                    ExpectedConditions.elementToBeClickable(By.linkText("Opinión"))
            );
            opinionLink.click();
        } catch (Exception e) {
            throw new RuntimeException("Cannot access Opinion section", e);
        }
    }

    // Finding all the article present in opinion section and adding them to list
    private static List<WebElement> findArticles(WebDriver driver) {
        List<WebElement> articles = driver.findElements(By.tagName("article"));
        if (articles.isEmpty()) {
            throw new RuntimeException("No articles found on page");
        }
        return articles;
    }

    // Scrapping the article listing,  logging all the content to console
    private static List<String> scrapeAndTranslateArticles( List<WebElement> articles) {
        List<String> translatedTitles = new ArrayList<>();

        for (int i = 0; i < Math.min(ARTICLE_COUNT, articles.size()); i++) {
            try {
                LOGGER.info(String.format("--- Article %d ---", i + 1));

                ArticleData article = scrapeArticle(articles.get(i), i + 1);

                scrapedArticles.add(article);

                if (article.title != null && !article.title.isEmpty()) {
                    String translated = translateText(article.title);
                    article.translatedTitle = translated;
                    translatedTitles.add(translated);

                    // Print in exact format requested
                    LOGGER.info(String.format("Title (Spanish): %s", article.title));
                    LOGGER.info(String.format("Title (English): %s", translated));
                    LOGGER.info(String.format("Content: %s", article.content != null ? article.content : "N/A"));
                    LOGGER.info(String.format("Image: %s\n", article.imagePath != null ? article.imagePath : "No image available for this Article"));
                }

                successCount++;

            } catch (Exception e) {
                LOGGER.warning(String.format("Failed to process article %d: %s", i + 1, e.getMessage()));
                failureCount++;
            }
        }

        return translatedTitles;
    }

    // Finding the article title, content, img
    private static ArticleData scrapeArticle(WebElement article, int index) {
        ArticleData data = new ArticleData();
        data.index = index;

        try {
            WebElement titleElement = article.findElement(By.tagName("h2"));
            data.title = titleElement.getText();
        } catch (Exception e) {
            data.title = "Untitled";
        }

        try {
            WebElement contentElement = article.findElement(By.tagName("p"));
            data.content = contentElement.getText();
        } catch (Exception e) {
            data.content = null;
        }

        try {
            WebElement imgElement = article.findElement(By.tagName("img"));
            String imgUrl = imgElement.getAttribute("src");

            if (imgUrl != null && !imgUrl.isEmpty()) {
                data.imagePath = saveImage(imgUrl, data.title, index);
            }
        } catch (Exception e) {
            data.imagePath = null;
        }

        return data;
    }

    // Saving the image of the article
    private static String saveImage(String imageUrl, String title, int index) throws IOException {
        String safeTitle = title.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]", "").trim();
        String filename = String.format("%s/article_%02d_%s.jpg", IMAGE_DIR, index, safeTitle);

        java.net.URL url = URI.create(imageUrl).toURL();
        try (InputStream is = url.openStream()) {
            Files.copy(is, Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);
        }

        return filename;
    }

    // Translating the article Title from spanish to english via RapidApi
    private static String translateText(String text) {
        try {
            String jsonBody = String.format(
                    "{\"from\":\"es\",\"to\":\"en\",\"json\":{\"title\":\"%s\"}}",
                    escapeJson(text)
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-rapidapi-key", API_KEY)
                    .header("x-rapidapi-host", API_HOST)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                String body = response.body();
                int start = body.indexOf("\"title\":\"") + 9;
                int end = body.indexOf("\"", start);
                if (start > 9 && end > start) {
                    return body.substring(start, end);
                }
            }
            return text;

        } catch (Exception e) {
            return text;
        }
    }

    // Analyzing the English translated Title for finding words that appear more than 2
    private static void analyzeWordFrequency(List<String> titles) {
        LOGGER.info("=== Word Frequency Analysis ===");

        Map<String, Integer> wordCount = new HashMap<>();

        for (String title : titles) {
            String[] words = title.toLowerCase().split("\\s+");
            for (String word : words) {
                word = word.replaceAll("[^a-zA-Z]", "");
                if (word.length() > 2) {
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                }
            }
        }

        List<Map.Entry<String, Integer>> repeated = wordCount.entrySet().stream()
                .filter(e -> e.getValue() > 2)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .toList();

        if (repeated.isEmpty()) {
            LOGGER.info("No words repeated more than twice\n");
        } else {
            for (Map.Entry<String, Integer> entry : repeated) {
                LOGGER.info(String.format("'%s' appeared %d times", entry.getKey(), entry.getValue()));
            }
            LOGGER.info("");
        }
    }

    // Creating the Report for all the information
    private static void generateReport() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String reportFile = String.format("report_%s.txt", timestamp);

        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("=== El País Scraper Report ===");
            writer.println("Generated: " + timestamp);
            writer.println("Total Articles: " + scrapedArticles.size());
            writer.println("Success: " + successCount);
            writer.println("Failures: " + failureCount);
            writer.println("\n=== Articles ===");

            for (ArticleData article : scrapedArticles) {
                writer.println(String.format("\nArticle %d:", article.index));
                writer.println("Title(Spanish): " + article.title);
                writer.println("Title(English): " + article.translatedTitle);
                writer.println("Content: " + (article.content != null ? article.content : "N/A"));
                writer.println("Image: " + (article.imagePath != null ? article.imagePath : "N/A"));
            }
        }

        LOGGER.info("Report generated: " + reportFile);
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream input = ElPaisScraper_locally.class.getResourceAsStream("/config.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                props.setProperty("rapidapi.key", "d7f580b68fmsh704e395cf461d1bp13dcd9jsne47f9357f7a6");
                props.setProperty("rapidapi.host", "google-translate113.p.rapidapi.com");
                props.setProperty("rapidapi.url", "https://google-translate113.p.rapidapi.com/api/v1/translator/json");
                props.setProperty("article.count", "5");
                props.setProperty("image.directory", "downloads/images");
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration");
        }
        return props;
    }

    private static void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("scraper.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getMessage() + "\n";
                }
            });
            consoleHandler.setLevel(Level.INFO);

            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(fileHandler);
            LOGGER.addHandler(consoleHandler);
            LOGGER.setLevel(Level.INFO);

        } catch (IOException e) {
            System.err.println("Logger setup failed: " + e.getMessage());
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    static class ArticleData {
        int index;
        String title;
        String content;
        String imagePath;
        String translatedTitle;
    }
}
