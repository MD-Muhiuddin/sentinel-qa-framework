package com.utility;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.constants.Browser;

/**
 * Base Class for all Page Objects. Handles Driver lifecycle, Synchronization,
 * Actions, and Recovery.
 */

public abstract class BrowserUtility {

	// ================================================================================
	// 🧵 THREAD-SAFE STORAGE
	// These static fields belong to the THREAD, not the object instance.
	// This allows parallel execution
	// ================================================================================

	protected static ThreadLocal<WebDriver> driver = new ThreadLocal<WebDriver>();
	// Store ENV in ThreadLocal so all Page Objects (HomePage, LoginPage,... etc )
	// in this// thread can access it
	private static ThreadLocal<String> threadEnv = new ThreadLocal<>();// Stores "QA", "DEV" per thread

	protected WebDriverWait wait;
	protected int timeOutSec;

	// Logger specifically for the child class calling this utility
	Logger logger = LoggerUtility.getLogger(this.getClass());

	public abstract boolean PageLoadedSuccessfully();

	// 1. Helper to log Session ID
	private void logSessionInfo() {
		WebDriver currentDriver = driver.get();

		if (driver.get() instanceof RemoteWebDriver) {
			SessionId sessionId = ((RemoteWebDriver) currentDriver).getSessionId();
			logger.info("-------------------------------------------------------");
			logger.info("🆔   Session ID: " + sessionId);
			logger.info("-------------------------------------------------------");
		}
	}

	// ================================================================================
	// CONSTRUCTORS
	// ================================================================================

	/**
	 * 1. THE "USER" CONSTRUCTOR (Used by Page Objects like LoginPage) When you say
	 * 'new LoginPage()', this runs. It connects the new page object to the existing
	 * Driver and Environment settings.
	 */
	public BrowserUtility() {
		initEnvConfig(); // 1. Recover the Environment (e.g., "QA")
		if (getDriver() == null) {
			logger.warn("⚠️ Driver is NULL. Ensure the browser was launched by TestBase first.");
		} else {
			setupWebDriverWait(); // 2. Setup the Wait object using the correct timeout
		}
	}

	/**
	 * 2. THE "FACTORY" CONSTRUCTOR (Used by TestBase) This is the Master Startup
	 * Logic. It opens the browser and saves settings.
	 */
	public BrowserUtility(Browser browserName, boolean isHeadLess, boolean isLambdaTest, String testName, String ENV) {
		logger.info("⚙️  Initializing: [Browser: " + browserName + " | Headless: " + isHeadLess + " | LamdaTest: "
				+ isLambdaTest + " | Environment: " + ENV + "]");

		// Save ENV to ThreadLocal so future Page Objects can find it
		threadEnv.set(ENV.toUpperCase());
		initEnvConfig();

		if (isLambdaTest) {
			// CLOUD EXECUTION
			logger.info("☁️ Running Cloud Browser Instance...");
			// passing 'isHeadLess' here allows us to toggle Video/Visual logs on LambdaTest
			WebDriver cloudDriver = LamdaTestUtility.initializeLambdaTestSession(browserName.toString(), testName,
					isHeadLess);
			driver.set(cloudDriver);

		} else {
			// 💻 LOCAL EXECUTION
			logger.info("🏠💻 Running Local Browser Instance...");
			launchLocalBrowser(browserName, isHeadLess);

		}

		setupWebDriverWait();
		logSessionInfo();
	}

	/**
	 * 3. THE "DEBUG" CONSTRUCTOR (Simple Local Launch)
	 */
	public BrowserUtility(Browser browserName) {
		launchLocalBrowser(browserName, false);
		setupWebDriverWait();
	}

	// ================================================================================
	// SETUP & CONFIGURATION METHODS
	// ================================================================================

	private void launchLocalBrowser(Browser browserName, boolean isHeadLess) {
		logger.info("💻 Launching Local Browser...");
		if (browserName == Browser.CHROME) {
			ChromeOptions options = new ChromeOptions();
			if (isHeadLess) {
				options.addArguments("--headless=new");
				options.addArguments("--disable-dev-shm-usage"); // Overcomes limited resource problems
				options.addArguments("--window-size=1920,1080");
				driver.set(new ChromeDriver(options));
			} else {
				driver.set(new ChromeDriver());
			}
		} else if (browserName == Browser.FIREFOX) {
			FirefoxOptions options = new FirefoxOptions();
			if (isHeadLess) {
				options.addArguments("--headless");
				options.addArguments("--width=1920");
				options.addArguments("--height=1080");
				driver.set(new FirefoxDriver(options));
			} else {
				driver.set(new FirefoxDriver());
			}
		}
	}

	/**
	 * Reads the ENV from the current thread and sets up timeouts. This allows 'new
	 * LoginPage()' to work without passing ENV every time.
	 */
	private void initEnvConfig() {
		String currentEnv = threadEnv.get();
		if (currentEnv == null || currentEnv.isEmpty()) {
			currentEnv = "DEV";
			logger.warn("⚠️ No Environment found in Thread! Defaulting to DEV.");
		}
		this.timeOutSec = JsonConfigUtility.getEnvData(currentEnv).getTimeout();
	}

	// Initialize Wait based on the ThreadLocal driver
	private void setupWebDriverWait() {
		if (getDriver() != null) {
			if (timeOutSec <= 0)
				timeOutSec = 30; // Defensive Check
			wait = new WebDriverWait(driver.get(), Duration.ofSeconds(timeOutSec));
		}
	}

	// ================================================================================
	// INTERACTION METHODS (
	// ================================================================================

	public void clickOn(By locator) {
		try {
			logger.info("➡️  Trying to Clicking on: " + locator);
			// 1. Wait for element to be present and scroll it into view
			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
			// 2. Scroll to element to ensure it's in the viewport (helps with 'intercepted'
			// errors)
			JavascriptExecutor js = (JavascriptExecutor) getDriver();
			js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
			// 3. Wait until it is actually clickable and perform standard click
			element = wait.until(ExpectedConditions.elementToBeClickable(locator));
			element.click();
			logger.info("🎯 Clicked successfully✅: " + locator);

		} catch (ElementClickInterceptedException e) {
			// 4. POWER UP: Fallback for Headless/Overlay issues
			logger.warn("⚠️  Click intercepted for " + locator + ". Retrying with JavaScript click...");
			JavascriptExecutor js = (JavascriptExecutor) getDriver();
			js.executeScript("arguments[0].click();", getDriver().findElement(locator));
			logger.info("🎯 Successfully clicked via JavaScript ✅.");

		} catch (StaleElementReferenceException e) {
			// 5. POWER UP: Handle elements that refresh suddenly
			logger.warn("⚠️  Element became stale for " + locator + ". Re-finding and retrying...");
			wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
			logger.info(" 🎯 Clicked successfully after re-finding element✅.");

		} catch (Exception e) {
			logger.error("❌ Failed to click on element: " + locator, e);
			throw e;
		}
	}

	public void clickON(By locator) {
		try {
			logger.info("🖱️ Clicking: " + locator);

			// 1. Wait + Scroll
			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
			scrollToElement(element);

			// 2. Interact
			wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
			logger.info("   ✅ Clicked Successfully.");

		} catch (ElementClickInterceptedException e) {
			logger.warn("   ⚠️ Intercepted! Retrying with JavaScript...");
			jsClick(locator);
		} catch (StaleElementReferenceException e) {
			logger.warn("   ⚠️ Stale Element! Refinding and retrying...");
			wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
		} catch (Exception e) {
			logger.error("   ❌ Failed to click: " + locator, e);
			throw e;
		}
	}

	public void enterText(By locator, String textToEnter) {
		try {
			logger.info("➡️  Trying to Entering text '" + textToEnter + "' into: " + locator);
			WebElement element = findElementWithHealing(locator);
			element.clear();
			element.sendKeys(textToEnter);
			logger.info(" ✔ Text entered successfully ✅.");
		} catch (Exception e) {
			logger.error("❌ Failed to enter text into " + locator, e);
			throw e;
		}
	}

	public String getVisibleText(By locator) {
		try {
			WebElement element = findElementWithHealing(locator);
			String text = element.getText();
			logger.info("👁️  Read Text: '" + text + "' from " + locator);
			return text;
		} catch (Exception e) {
			logger.error("❌ Failed to get text from element: " + locator, e);
			throw e;
		}
	}

	public boolean isElementDisplayed(By locator) {
		try {
			WebElement element = findElementWithHealing(locator);
			return element.isDisplayed();
		} catch (TimeoutException e) {
			logger.warn("⚠️  Element was not displayed (Timeout): " + locator);
			return false;
		} catch (Exception e) {
			logger.warn("⚠️  Could not check visibility for " + locator + ". Cause: " + e.getMessage());
			return false;
		}
	}
	
	public String takeScreenShot(String name) {
		Date date = new Date();

		// ✅ CHANGED: Added "yyyy-MM-dd" to the format
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String timeStamp = format.format(date);

		String folderPath = System.getProperty("user.dir") + File.separator + "screenshots";
		File folder = new File(folderPath);

		if (!folder.exists()) {
			folder.mkdirs();
		}

		// Result Example: loginTest-2026-02-01_14-30-05.png
		String path = folderPath + File.separator + name + "-" + timeStamp + ".png";

		try {
			TakesScreenshot screenshot = (TakesScreenshot) getDriver();
			File srcFile = screenshot.getScreenshotAs(OutputType.FILE);
			File destFile = new File(path);
			FileUtils.copyFile(srcFile, destFile);
			return path;
		} catch (Exception e) {
			logger.error("❌ Critical Failure: Could not take screenshot. Browser might be unresponsive.", e);
			return null;
		}
	}

	// ================================================================================
	// 🛡️ RECOVERY & HEALING MECHANISMS
	// ================================================================================
	private WebElement findElementWithHealing(By locator) {
		// Attempts to find element, retrying once if it goes stale immediately
		try {
			return waitForElementToAppear(locator);
		} catch (StaleElementReferenceException e) {
			logger.warn("🩹 Stale Element detected. Healing...");
			return waitForElementToAppear(locator);
		}
	}

	public WebElement waitForElementToAppear(By locator) {
		long start = System.nanoTime();

		// Using Log4j2 syntax {}
		logger.debug("⏳ Waiting for element → {} (timeout: {}s)", locator, timeOutSec);

		try {
			WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

			long elapsedMs = (System.nanoTime() - start) / 1_000_000;

			// Log only if wait was "slow" (greater than 3 seconds)
			if (elapsedMs > 3000) {
				logger.warn("🐢 Slow wait: element visible after {} ms → {}", elapsedMs, locator);
			} else {
				logger.info("✅ Element visible after {} ms → {}", elapsedMs, locator);
			}

			return element;

		} catch (TimeoutException e) {
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			logger.error("⏰ Timeout after {} ms waiting for element → {}", elapsedMs, locator);
			throw e;
		} catch (Exception e) {
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			logger.error("❌ Error after {} ms while waiting for element → {}", elapsedMs, locator, e);
			throw e;
		}
	}

	// ================================================================================
	// UTILITIES & HELPERS
	// ================================================================================
	public WebDriver getDriver() {
		return driver.get();
	}

	public void goToWebsite(String url) {
		try {
			logger.info("➡️  Navigating to URL: " + url);
			driver.get().get(url);
			// logger.info("✅ Navigated successfully."); // Optional: Keep it quiet unless
			// error

		} catch (TimeoutException e) {
			logger.error("❌ Navigation timed out for URL: " + url);
			throw e;

		} catch (WebDriverException e) {
			logger.error("❌ Critical failure navigating to URL: " + url + ". Cause: " + e.getMessage());
			throw e;
		}
	}

	public void maximizeWindow() {
		try {
			driver.get().manage().window().maximize();
			logger.info("🔲 Window Maximized");

		} catch (Exception e) {
			logger.error("❌ Failed to maximize browser window.", e);
		}
	}

	public void scrollToElement(WebElement element) {
		((JavascriptExecutor) getDriver())
				.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
	}

	public void jsClick(By locator) {
		WebElement el = getDriver().findElement(locator);
		((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", el);
	}

	public void quitBrowser() {
		if (driver.get() != null) {
			driver.get().quit(); // Close Browser
			driver.remove(); // Clean Driver Memory
			threadEnv.remove(); // Clean Env Memory
			logger.info("🛑 Session Closed & Resources Cleaned.");
		}
	}



}