package com.codingblocks.chatter;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginInstrumentedTest {
    @Rule
    public ActivityTestRule<AuthenticationActivity> mMainActivityActivityTestRule =
            new ActivityTestRule<>(AuthenticationActivity.class);

    private AuthenticationActivity mainActivity = null;


    @Before
    public void setUp() {

        mainActivity = mMainActivityActivityTestRule.getActivity();
    }

    @Test
    public void checkClientId() throws Exception {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        final int timeOut = 1000 * 60;
        mDevice.wait(Until.findObject(By.clazz(WebView.class)), timeOut);
        UiObject getstarted = mDevice.findObject(new UiSelector().instance(0).className(Button.class));
        getstarted.waitForExists(timeOut);
        getstarted.click();
        //Username edit text box
        UiObject usernameInput = mDevice.findObject(new UiSelector().instance(0).className(EditText.class));
        usernameInput.waitForExists(timeOut);
        usernameInput.setText("venomousboxer");
        //password edit text box
        UiObject passwordInput = mDevice.findObject(new UiSelector().instance(1).className(EditText.class));
        passwordInput.waitForExists(timeOut);
        passwordInput.setText("shanlong39");
        //login button
        UiObject loginButton = mDevice.findObject(new UiSelector().instance(1).className(Button.class));
        loginButton.waitForExists(timeOut);
        loginButton.clickAndWaitForNewWindow();
    }

    @After
    public void tearDown() {

        mainActivity = null;
    }
}
