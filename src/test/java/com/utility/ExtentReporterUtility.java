//package com.utility;
//
//import com.aventstack.extentreports.ExtentReports;
//import com.aventstack.extentreports.ExtentTest;
//import com.aventstack.extentreports.reporter.ExtentSparkReporter;
//
//public class ExtentReporterUtility {
//
//	private static ExtentReports extentReports;
//	private static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<ExtentTest>();
//
//	public static void setupSparkReporter(String reporterName) {
//
//		ExtentSparkReporter extentSparkReporter = new ExtentSparkReporter(
//				System.getProperty("user.dir") + "//" + reporterName);
//		extentReports = new ExtentReports();
//		extentReports.attachReporter(extentSparkReporter);
//	}
//
//	public static void createExtentTest(String testName) {
//		ExtentTest test = extentReports.createTest(testName);
//		extentTest.set(test);
//	}
//
//	public static ExtentTest getTest() {
//		return extentTest.get();
//	}
//
//	public static void flushReport() {
//		extentReports.flush();
//	}
//
//}
package com.utility;

import java.io.File;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class ExtentReporterUtility {

	// 1. Use Constants to avoid "Magic Strings"
	private static final String REPORT_FOLDER = "reports";
	private static final String REPORT_TITLE = "SentinelQA Automation Report";
	private static final String REPORT_NAME = "Regression Test Suite Results";

	private static ExtentReports extentReports;
	private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

	/**
	 * Initializes the report with cross-platform path handling.
	 * 
	 * @param reportFileName The name of the HTML file (e.g., "TestReport.html")
	 */
	public static void setupSparkReporter(String reportFileName) {
		if (extentReports == null) {

			// 2. Use File.separator for OS Compatibility (Windows vs Linux)
			String reportPath = System.getProperty("user.dir") + File.separator + REPORT_FOLDER + File.separator
					+ reportFileName;

			ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

			// 3. Chain configuration methods for better readability
			spark.config().setTheme(Theme.STANDARD);
			spark.config().setDocumentTitle(REPORT_TITLE);
			spark.config().setReportName(REPORT_NAME);
			spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

			extentReports = new ExtentReports();
			extentReports.attachReporter(spark);

			// 4. Clean System Info Injection
			extentReports.setSystemInfo("OS", System.getProperty("os.name"));
			extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
			extentReports.setSystemInfo("User", System.getProperty("user.name"));
			extentReports.setSystemInfo("Environment", System.getProperty("env", "QA"));
		}
	}

	public static void createExtentTest(String testName) {
		ExtentTest test = extentReports.createTest(testName);
		extentTest.set(test);
	}

	public static ExtentTest getTest() {
		return extentTest.get();
	}

	/**
	 * Enhanced Logging with readable Status checks
	 */
	public static void log(Status status, String message) {
		if (getTest() == null)
			return; // Null safety check

		// 5. Use formatted labels for visual clarity in the report
		ExtentColor color = switch (status) {
		case PASS -> ExtentColor.GREEN;
		case FAIL -> ExtentColor.RED;
		case SKIP -> ExtentColor.ORANGE;
		default -> ExtentColor.INDIGO;
		};

		getTest().log(status, MarkupHelper.createLabel(message, color));
	}

	/**
	 * Assigns groups/categories to the current test for report filtering.
	 * 
	 * @param categories e.g., "Smoke", "Regression", "Login"
	 */
	public static void assignCategory(String... categories) {
		if (getTest() != null) {
			for (String category : categories) {
				getTest().assignCategory(category);
			}
		}
	}

	/**
	 * Assigns the author name to the test.
	 */
	public static void assignAuthor(String author) {
		if (getTest() != null) {
			getTest().assignAuthor(author);
		}
	}

	public static void flushReport() {
		if (extentReports != null) {
			extentReports.flush();
		}
	}
}