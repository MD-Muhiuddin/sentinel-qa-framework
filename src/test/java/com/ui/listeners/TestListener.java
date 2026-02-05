package com.ui.listeners;

import java.util.Arrays;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.apache.logging.log4j.Logger;

import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.ui.test.TestBase;
import com.utility.BrowserUtility;
import com.utility.ExtentReporterUtility;
import com.utility.LoggerUtility;

public class TestListener implements ITestListener {

	Logger logger = LoggerUtility.getLogger(this.getClass());

	@Override
	public void onStart(ITestContext context) {
		logger.info("╔════════════════════════════════════════════════════════════════════════════════╗");
		logger.info("║         🌟           TEST SUITE EXECUTION STARTED          🌟                  ║");
		logger.info("╚════════════════════════════════════════════════════════════════════════════════╝");
		ExtentReporterUtility.setupSparkReporter("report.html");
	}

	@Override
	public void onTestStart(ITestResult result) {
		logger.info("----------------------------------------------------------------------------------");
		logger.info("🚀 TEST STARTED: " + result.getMethod().getMethodName());
		logger.info("   Description : " + result.getMethod().getDescription());
		logger.info("   Groups      : " + Arrays.toString(result.getMethod().getGroups()));
		logger.info("----------------------------------------------------------------------------------");

		ExtentReporterUtility.createExtentTest(result.getMethod().getMethodName());
		// 2. Automatically fetch groups from @Test(groups = {"Regression"})
		String[] groups = result.getMethod().getGroups();
		if (groups.length > 0) {
			ExtentReporterUtility.assignCategory(groups);
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		logger.info("🟢[TEST 🅿 🅰 🆂 🆂 🅴 🅳 : " + result.getMethod().getMethodName() + "🟢");

		long duration = result.getEndMillis() - result.getStartMillis();
		logger.info("   ⏱️ Time Taken: " + duration + " ms");
		// 3. Extent Report: Use a Green Label for better visibility
		ExtentReporterUtility.getTest().pass(MarkupHelper.createLabel("Test Passed Successfully", ExtentColor.GREEN));

		// 4. Extent Report: Log the duration details
		ExtentReporterUtility.log(Status.INFO, "Execution Time: <b>" + duration + " ms</b>");

		// 5. (Optional) Log Parameters if it was a data-driven test
		Object[] params = result.getParameters();
		if (params.length > 0) {
			String paramString = java.util.Arrays.deepToString(params);
			ExtentReporterUtility.log(Status.INFO, "With Data: " + paramString);
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		logger.error("❌ TEST 🅵 🅰 🅸 🅻 🅴 🅳 : " + result.getMethod().getMethodName() + "❌");
		logger.error("   Reason      : " + result.getThrowable().getMessage());

		ExtentReporterUtility.getTest().fail(MarkupHelper.createLabel("Test Failed!!", ExtentColor.RED));
		ExtentReporterUtility.getTest().log(Status.FAIL, result.getThrowable().getMessage());

		// test execution Duration
		long duration = result.getEndMillis() - result.getStartMillis();
		logger.info("   ⏱️ Time Taken: " + duration + " ms");
		ExtentReporterUtility.log(Status.INFO, "Execution Time: <b>" + duration + " ms</b>");

		// --- NEW: PARAMETER LOGGING LOGIC ---
		// 5. (Optional) Log Parameters if it was a data-driven test
		Object[] params = result.getParameters();
		if (params.length > 0) {
			String paramString = java.util.Arrays.deepToString(params);
			ExtentReporterUtility.log(Status.INFO, "With Data: " + paramString);
		}

		// ------------------------------------

		// --- SCREENSHOT LOGIC ---

		// Robust Screenshot Logic (Java 21 Pattern Matching)
		try {
			// Checks if instance is valid AND if the browser utility exists in one line
			if (result.getInstance() instanceof TestBase testBase && testBase.getInstance() != null) {

				logger.info("📸 Attempting to capture screenshot...");
				String screenshotPath = testBase.getInstance().takeScreenShot(result.getName());

				if (screenshotPath != null) {
					ExtentReporterUtility.getTest().addScreenCaptureFromPath(screenshotPath);
					logger.info("   ✅ Screenshot attached: " + screenshotPath);
				} else {
					logger.warn("   ⚠️ Screenshot skipped: Path was null (Driver might be closed).");
				}
			} else {
				logger.error("   ❌ Cannot take screenshot: BrowserUtility is NULL or Class is not TestBase.");
			}
		} catch (Exception e) {
			logger.error("   ❌ Failed to attach screenshot: " + e.getMessage());
		}

	}

	@Override
	public void onTestSkipped(ITestResult result) {
		logger.warn("⚠️ [TEST SKIPPED]: " + result.getMethod().getMethodName()
				+ "Test skipped due to an initial failure; re-executing....");

		ExtentReporterUtility.getTest().log(Status.SKIP,
				"Test marked as SKIPPED because of a prior failure; retry mechanism triggered.");

		long duration = result.getEndMillis() - result.getStartMillis();
		logger.info("   ⏱️ Time Taken: " + duration + " ms");
		ExtentReporterUtility.log(Status.INFO, "Execution Time: <b>" + duration + " ms</b>");
		// --- SCREENSHOT LOGIC ---
		try {
			// Checks if instance is valid AND if the browser utility exists in one line
			if (result.getInstance() instanceof TestBase testBase && testBase.getInstance() != null) {

				logger.info("📸 Attempting to capture screenshot...");
				String screenshotPath = testBase.getInstance().takeScreenShot(result.getName());

				if (screenshotPath != null) {
					ExtentReporterUtility.getTest().addScreenCaptureFromPath(screenshotPath);
					logger.info("   ✅ Screenshot attached: " + screenshotPath);
				} else {
					logger.warn("   ⚠️ Screenshot skipped: Path was null (Driver might be closed).");
				}
			} else {
				logger.error("   ❌ Cannot take screenshot: BrowserUtility is NULL or Class is not TestBase.");
			}
		} catch (Exception e) {
			logger.error("   ❌ Failed to attach screenshot: " + e.getMessage());
		}
	}

	@Override
	public void onFinish(ITestContext context) {
		logger.info("╔════════════════════════════════════════════════════════════════════════════════╗");
		logger.info("║   🟢🌟                 TEST SUITE EXECUTION FINISHED           🌟🟢            ║");
		logger.info("╚════════════════════════════════════════════════════════════════════════════════╝");
		ExtentReporterUtility.flushReport();
	}
}
