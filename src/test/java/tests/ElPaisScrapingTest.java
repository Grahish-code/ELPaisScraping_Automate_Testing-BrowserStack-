package tests;

import models.ArticleData;
import models.SessionResult;
import pages.HomePage;
import pages.OpinionPage;
import utils.PdfReportUtil;
import utils.ScraperUtils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class ElPaisScrapingTest {

    private static final Properties CONFIG = loadConfig();

    private static final String BROWSERSTACK_USERNAME = CONFIG.getProperty("browserstack.username");
    private static final String BROWSERSTACK_ACCESS_KEY = CONFIG.getProperty("browserstack.access_key");
    private static final String BROWSERSTACK_URL = String.format(
            "https://%s:%s@hub-cloud.browserstack.com/wd/hub",
            BROWSERSTACK_USERNAME, BROWSERSTACK_ACCESS_KEY
    );

    private static final String API_KEY  = CONFIG.getProperty("rapidapi.key");
    private static final String API_HOST = CONFIG.getProperty("rapidapi.host");
    private static final String API_URL  = CONFIG.getProperty("rapidapi.url");
    private static final int ARTICLE_COUNT = 5;

    private final ThreadLocal<WebDriver> driverThread = new ThreadLocal<>();
    private static final List<SessionResult> GLOBAL_RESULTS = Collections.synchronizedList(new ArrayList<>());
    private static final Logger LOGGER = Logger.getLogger(ElPaisScrapingTest.class.getName());

    @DataProvider(name = "browsers", parallel = true)
    public Object[][] browsers() {
        List<DesiredCapabilities> configs = loadBrowserConfigs();
        Object[][] data = new Object[configs.size()][1];
        for (int i = 0; i < configs.size(); i++) {
            data[i][0] = configs.get(i);
        }
        return data;
    }

    @BeforeMethod
    public void setUp(Object[] params) throws Exception {
        DesiredCapabilities caps = (DesiredCapabilities) params[0];
        WebDriver driver = new RemoteWebDriver(new URL(BROWSERSTACK_URL), caps);
        driverThread.set(driver);
    }

    @Test(dataProvider = "browsers")
    public void scrapElPaisOpinion(DesiredCapabilities caps) throws Exception {
        WebDriver driver = driverThread.get();

        @SuppressWarnings("unchecked")
        Map<String, Object> bstackOpts = (Map<String, Object>) caps.getCapability("bstack:options");
        String sessionName = bstackOpts != null ? (String) bstackOpts.get("sessionName") : "Unknown";

        LOGGER.info(String.format("\n[%s] Starting test", sessionName));

        String imageDir = "downloads/" + sessionName.replaceAll("[^a-zA-Z0-9]", "_");
        Files.createDirectories(Paths.get(imageDir));

        // 1. Home Page Flow
        HomePage homePage = new HomePage(driver, sessionName);
        homePage.navigateToEspana();
        String screenshotPath = homePage.takeScreenshot(imageDir);
        homePage.verifySpanishLanguage();
        homePage.acceptCookies();

        // 2. Navigate to Opinion
        OpinionPage opinionPage = homePage.goToOpinionPage();

        // 3. Scrape Data
        List<ArticleData> scrapedArticles = opinionPage.scrapeArticles(ARTICLE_COUNT, imageDir, API_URL, API_KEY, API_HOST);

        // 4. Save Session Results
        SessionResult result = new SessionResult();
        result.sessionName = sessionName;
        result.homePageScreenshot = screenshotPath;
        result.articles = scrapedArticles;
        result.wordFreq = ScraperUtils.analyzeWordFrequency(scrapedArticles);

        GLOBAL_RESULTS.add(result);
        LOGGER.info(String.format("[%s] Added results to global list.", sessionName));
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        WebDriver driver = driverThread.get();
        if (driver != null) {
            try {
                String status  = result.isSuccess() ? "passed" : "failed";
                String reason  = result.isSuccess()
                        ? "Scraping completed successfully"
                        : (result.getThrowable() != null ? result.getThrowable().getMessage() : "Test failed");

                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        String.format("browserstack_executor: {\"action\": \"setSessionStatus\", " +
                                        "\"arguments\": {\"status\": \"%s\", \"reason\": \"%s\"}}",
                                status, reason.replace("\"", "'"))
                );
            } catch (Exception e) {
                LOGGER.warning("Could not set BrowserStack session status: " + e.getMessage());
            } finally {
                driver.quit();
                driverThread.remove();
            }
        }
    }

    @AfterSuite
    public void generateReport() {
        PdfReportUtil.generateMasterPdfReport(GLOBAL_RESULTS);
    }

    // --- Helpers for Configuration ---

    @SuppressWarnings("unchecked")
    private static List<DesiredCapabilities> loadBrowserConfigs() {
        List<DesiredCapabilities> configs = new ArrayList<>();
        try (InputStream is = ElPaisScrapingTest.class
                .getClassLoader().getResourceAsStream("browsers.yml")) {
            if (is == null) throw new RuntimeException("browsers.yml not found in classpath");

            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            List<Map<String, String>> browsers = (List<Map<String, String>>) root.get("browsers");

            for (Map<String, String> browser : browsers) {
                DesiredCapabilities caps = new DesiredCapabilities();
                Map<String, Object> bstackOptions = new HashMap<>();

                caps.setCapability("browserName", browser.get("browserName"));

                if (browser.containsKey("os")) {
                    caps.setCapability("browserVersion", browser.get("browserVersion"));
                    bstackOptions.put("os", browser.get("os"));
                    bstackOptions.put("osVersion", browser.get("osVersion"));
                }
                if (browser.containsKey("deviceName")) {
                    bstackOptions.put("deviceName", browser.get("deviceName"));
                    bstackOptions.put("osVersion", browser.get("osVersion"));
                    bstackOptions.put("realMobile", browser.get("realMobile"));
                }

                bstackOptions.put("sessionName", browser.get("sessionName"));

                caps.setCapability("bstack:options", bstackOptions);
                configs.add(caps);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load browsers.yml: " + e.getMessage());
        }
        return configs;
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = ElPaisScrapingTest.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) throw new RuntimeException("config.properties not found in classpath");
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage());
        }
        return props;
    }
}