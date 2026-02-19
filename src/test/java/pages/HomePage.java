package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.logging.Logger;

public class HomePage {
    private WebDriver driver;
    private WebDriverWait wait;
    private String sessionName;
    private static final Logger LOGGER = Logger.getLogger(HomePage.class.getName());

    // Locators
    private By htmlTag = By.tagName("html");
    private By cookieBtn = By.id("didomi-notice-agree-button");
    private By opinionLink = By.linkText("Opinión");

    public HomePage(WebDriver driver, String sessionName) {
        this.driver = driver;
        this.sessionName = sessionName;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public void navigateToEspana() {
        driver.get("https://elpais.com/espana/");
    }

    public void verifySpanishLanguage() {
        WebElement htmlElement = driver.findElement(htmlTag);
        String pageLang = htmlElement.getAttribute("lang");
        LOGGER.info(String.format("[%s] Page language detected: %s", sessionName, pageLang));

        Assert.assertTrue(pageLang != null && pageLang.startsWith("es"),
                String.format("[%s] Critical Error: Website language is not Spanish! Found: %s", sessionName, pageLang));
    }

    public String takeScreenshot(String imageDir) {
        String screenshotPath = imageDir + "/home_screenshot.jpg";
        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(srcFile.toPath(), Paths.get(screenshotPath), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info(String.format("[%s] Screenshot taken", sessionName));
            return screenshotPath;
        } catch (Exception e) {
            LOGGER.warning("Failed to take screenshot: " + e.getMessage());
            return null;
        }
    }

    public void acceptCookies() {
        try {
            WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(cookieBtn));
            btn.click();
            LOGGER.info(String.format("[%s] Cookies accepted", sessionName));
            Thread.sleep(2000);
        } catch (Exception e) {
            LOGGER.warning(String.format("[%s] Cookie banner not found", sessionName));
        }
    }

    public OpinionPage goToOpinionPage() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement link = shortWait.until(ExpectedConditions.presenceOfElementLocated(opinionLink));
            Thread.sleep(1000);
            link.click();
            LOGGER.info(String.format("[%s] Navigated to Opinión via link", sessionName));
        } catch (Exception e) {
            //If by chance due to some responsivness of different devices we can't see Opinion then we direclty navigate it via link
            LOGGER.warning(String.format("[%s] Link not found, falling back to direct URL", sessionName));
            driver.get("https://elpais.com/opinion/");
            LOGGER.info(String.format("[%s] Navigated to Opinión via URL", sessionName));
        }

        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        return new OpinionPage(driver, sessionName);
    }
}
