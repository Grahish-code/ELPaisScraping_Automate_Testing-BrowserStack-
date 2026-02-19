import org.testng.*;

import java.util.logging.Logger;

/**
 * Session pass/fail marking is handled directly in tests.ElPaisScrapingTest.tearDown()
 * using the BrowserStack JS executor because the driver reference lives there.
 */
public class BrowserStackListener implements ISuiteListener, ITestListener {

    private static final Logger LOGGER = Logger.getLogger(BrowserStackListener.class.getName());

    // ── Suite level ──

    @Override
    public void onStart(ISuite suite) {
        LOGGER.info("\n========== Suite Starting: " + suite.getName() + " ==========");
    }

    @Override
    public void onFinish(ISuite suite) {
        LOGGER.info("\n========== Suite Finished: " + suite.getName() + " ==========");

        int passed = 0, failed = 0, skipped = 0;
        for (ISuiteResult result : suite.getResults().values()) {
            passed  += result.getTestContext().getPassedTests().size();
            failed  += result.getTestContext().getFailedTests().size();
            skipped += result.getTestContext().getSkippedTests().size();
        }

        LOGGER.info(String.format("Final Results — Passed: %d | Failed: %d | Skipped: %d",
                passed, failed, skipped));
    }

    // ── Test level ──

    @Override
    public void onTestStart(ITestResult result) {
        LOGGER.info("\n[STARTED]  " + getTestName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOGGER.info("[PASSED]   " + getTestName(result)
                + "  (" + duration(result) + "ms)");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.severe("[FAILED]   " + getTestName(result)
                + "  (" + duration(result) + "ms)"
                + "\n  Reason: " + result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOGGER.warning("[SKIPPED]  " + getTestName(result));
    }

    // ── Utility ──

    private String getTestName(ITestResult result) {
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            return result.getName() + " [" + params[0] + "]";
        }
        return result.getName();
    }

    private long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}