package com.ui.pages;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.constants.Browser;
import com.utility.BrowserUtility;
import com.utility.JsonConfigUtility;
import com.utility.LoggerUtility;

public final class HomePage extends BrowserUtility {

    Logger logger = LoggerUtility.getLogger(this.getClass());

	static final By SIGN_IN_BUTTON_LOCATOR = By.xpath("//a[contains(text(),'login')]");
    
	String ENV=null;

    // ================================================================================
    //  CONSTRUCTOR: Passes configuration up to the Parent (BrowserUtility)
    // ================================================================================
    public HomePage(Browser browserName, boolean isHeadLess, boolean isLambdaTest,String testName,String ENV) {
        // This single line starts the correct browser (Local or Cloud) based on the flags
        super(browserName, isHeadLess, isLambdaTest, testName,ENV);
        this.ENV = ENV;
        // Navigation Logic
        String url = JsonConfigUtility.getEnvData(ENV.toUpperCase()).getUrl();
        goToWebsite(url);
        maximizeWindow();
    }


    @Override
    public boolean PageLoadedSuccessfully() {
        String currentUrl = getDriver().getCurrentUrl();
        boolean isUrlCorrect = currentUrl.contains(JsonConfigUtility.getEnvData(ENV.toUpperCase()).getUrl()); 
        boolean isSignButtonVisible = isElementDisplayed(SIGN_IN_BUTTON_LOCATOR);

        if (isUrlCorrect && isSignButtonVisible) {
            logger.info("HomePage loaded successfully.");
            return true;
        } else {
            logger.error("HomePage FAILED to load!");
            return false;
        }
    }

    public LoginPage goToLoginPage() {
        logger.info("Trying to perform click to go to Sign in Page");
        if (PageLoadedSuccessfully()) {
            clickOn(SIGN_IN_BUTTON_LOCATOR);
        }
        
        return new LoginPage(); 
    }
}
