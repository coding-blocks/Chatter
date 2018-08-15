package com.codingblocks.chatter;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class WebViewActivityTest {


    @Rule
    public ActivityTestRule<AuthenticationActivity> mActivityRule = new ActivityTestRule<AuthenticationActivity>(
            AuthenticationActivity.class, false, false) {
        @Override
        protected void afterActivityLaunched() {
            // Technically we do not need to do this - WebViewActivity has javascript turned on.
            // Other WebViews in your app may have javascript turned off, however since the only way
            // to automate WebViews is through javascript, it must be enabled.
            onWebView().forceJavascriptEnabled();
        }
    };

    @Test
    public void typeTextInInput_clickButton_SubmitsForm() throws InterruptedException, UiObjectNotFoundException {
        // Lazily launch the Activity with a custom start Intent per test
        mActivityRule.launchActivity(withWebFormIntent());

        // Selects the WebView in your layout. If you have multiple WebViews you can also use a
        // matcher to select a given WebView, onWebView(withId(R.id.web_view)).
        onWebView().withElement(findElement(Locator.XPATH, "/html/body/div/div/section/p/a")).perform(webClick());
        onWebView().withElement(findElement(Locator.XPATH, "//*[@id=\"modal-body-region\"]/div/section/div[1]/a[2]")).perform(webClick());
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;


        //Username edit text box
        UiObject usernameInput = mDevice.findObject(new UiSelector().instance(0).className(EditText.class));
        usernameInput.waitForExists(timeOut);
        usernameInput.setText("aggarwalpulkit596@gmail.com");

        //password edit text box
        UiObject passwordInput = mDevice.findObject(new UiSelector().instance(1).className(EditText.class));
        passwordInput.waitForExists(timeOut);
        passwordInput.setText("PULkit9582*");

        //login button
        UiObject loginButton = mDevice.findObject(new UiSelector().instance(0).className(Button.class));
        loginButton.waitForExists(timeOut);
        loginButton.clickAndWaitForNewWindow();

        UiObject approveBtn = mDevice.findObject(new UiSelector().instance(0).className(Button.class));
        approveBtn.waitForExists(timeOut);
        approveBtn.clickAndWaitForNewWindow();


    }

    /**
     * @return start {@link Intent} for the simple web form URL.
     */
    private static Intent withWebFormIntent() {
        return new Intent();
    }

}
