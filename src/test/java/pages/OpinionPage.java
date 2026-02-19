package pages;

import models.ArticleData;
import utils.ScraperUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OpinionPage {
    private WebDriver driver;
    private String sessionName;
    private static final Logger LOGGER = Logger.getLogger(OpinionPage.class.getName());

    // Locators
    private By articlesLocator = By.tagName("article");
    private By titleLocator = By.tagName("h2");
    private By contentLocator = By.tagName("p");
    private By imageLocator = By.tagName("img");

    public OpinionPage(WebDriver driver, String sessionName) {
        this.driver = driver;
        this.sessionName = sessionName;
    }

    public List<ArticleData> scrapeArticles(int count, String imageDir, String apiUrl, String apiKey, String apiHost) {
        List<WebElement> articleElements = driver.findElements(articlesLocator);
        LOGGER.info(String.format("[%s] Found %d articles", sessionName, articleElements.size()));

        List<ArticleData> scrapedData = new ArrayList<>();

        for (int i = 0; i < Math.min(count, articleElements.size()); i++) {
            try {
                ArticleData data = new ArticleData();
                data.index = i + 1;
                WebElement article = articleElements.get(i);

                // Force the mobile browser to scroll to the article so it renders the text
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});", article);

                // Give Safari a brief moment to render the lazy-loaded text
                Thread.sleep(800);

                // Title
                try {
                    data.title = article.findElement(titleLocator).getText();
                    LOGGER.info(String.format("[%s] Title (Spanish): %s", sessionName, data.title));
                } catch (Exception e) {
                    data.title = "Untitled";
                }

                // Translation
                if (!data.title.isEmpty() && !data.title.equals("Untitled")) {
                    data.translatedTitle = ScraperUtils.translateText(data.title, apiUrl, apiKey, apiHost);
                    LOGGER.info(String.format("[%s] Title (English): %s", sessionName, data.translatedTitle));
                }

                // Content
                try {
                    data.content = article.findElement(contentLocator).getText();
                } catch (Exception e) {
                    data.content = "N/A";
                }

                // Image
                try {
                    WebElement img = article.findElement(imageLocator);
                    String imgUrl = img.getAttribute("src");
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        data.imagePath = ScraperUtils.saveImage(imgUrl, data.title, i + 1, imageDir);
                    }
                } catch (Exception e) {
                    LOGGER.warning(String.format("[%s] Image not available for article %d", sessionName, data.index));
                    data.imagePath = null;
                }

                scrapedData.add(data);
                Thread.sleep(500);

            } catch (Exception e) {
                LOGGER.warning(String.format("[%s] Failed on article %d: %s", sessionName, i + 1, e.getMessage()));
            }
        }
        return scrapedData;
    }
}