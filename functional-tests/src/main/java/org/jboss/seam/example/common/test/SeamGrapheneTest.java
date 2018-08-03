/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.example.common.test;

import static org.jboss.arquillian.graphene.Graphene.guardAjax;
import static org.jboss.arquillian.graphene.Graphene.guardHttp;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Base class for all Graphene tests of Seam.
 *
 * @author jbalunas
 * @author jharting
 *
 */
public abstract class SeamGrapheneTest {

    @Rule
    public MethodRule watchman = new TestWatchman() {
        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            BufferedOutputStream bos = null;
            BufferedWriter bw = null;
            File testOutput = new File("target/test-output");
            if (!testOutput.exists()) {
                testOutput.mkdirs();
            }
            try {// HTMLUnit can't maximize a window or take a screenshot
                browser.manage().window().maximize();
                //WebDriver augmentedDriver = new Augmenter().augment(browser);
                byte[] screenshot = ((TakesScreenshot) browser).getScreenshotAs(OutputType.BYTES);
                //byte[] screenshot = ((TakesScreenshot) browser).getScreenshotAs(OutputType.BYTES);
                bos = new BufferedOutputStream(new FileOutputStream(testOutput.getAbsolutePath() + "/" + method.getName() + ".png"));
                bos.write(screenshot);
                bos.close();

                bw = new BufferedWriter(new FileWriter(testOutput.getAbsolutePath() + "/" + method.getName() + ".html"));
                bw.write(browser.getPageSource());
                bw.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    bos.close();
                    bw.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    };

    private static String PROPERTY_FILE = "/ftest.properties";
    private static boolean propertiesLoaded = false;
    private static boolean propertiesExist = false;
    private static Properties properties = new Properties();

    @Drone
    public WebDriver browser;

    @ArquillianResource
    protected URL contextPath;

    @Before
    public void beforeTest() throws MalformedURLException {
        open(contextPath.toString());
    }

    public static String getProperty(String key, Object... args) {
        if (!propertiesLoaded) {
            try {
                InputStream is = SeamGrapheneTest.class.getResourceAsStream(PROPERTY_FILE);
                if (is == null) {
                    propertiesLoaded = true;
                    propertiesExist = false;
                } else {
                    properties.load(is);
                    propertiesLoaded = true;
                    propertiesExist = true;
                }
            } catch (IOException e) {
                // ?
            }
        }
        String propValue = propertiesExist ? properties.getProperty(key) : null;
        if (propValue != null && args.length != 0) {
            return MessageFormat.format(propValue, args);
        }
        return propValue;
    }

    public By getBy(String seleniumLocatorProperty, Object... args) {
        String seleniumLocator = getProperty(seleniumLocatorProperty);
        if (seleniumLocator == null) {
            seleniumLocator = seleniumLocatorProperty;
        }
        String locator = seleniumLocator.substring(seleniumLocator.indexOf("=") + 1);
        if (args.length != 0) {
            locator = MessageFormat.format(locator, args);
        }
        if (seleniumLocator.startsWith("id")) {
            return By.id(locator);
        } else if (seleniumLocator.startsWith("xpath")) {
            return By.xpath(locator);
        } else if (seleniumLocator.startsWith("css")) {
            return By.cssSelector(locator);
        } else if (seleniumLocator.startsWith("link")) {
            return By.linkText(locator);
        } else if (seleniumLocator.startsWith("name")) {
            return By.name(locator);
        } else {
            return null;
        }
    }

    public boolean isElementPresent(By by) {
        try {
            return browser.findElement(by).isDisplayed();
        } catch (NotFoundException nfe) {
            return false;
        }
    }

    public String getText(By by) {
        return browser.findElement(by).getText();
    }

    public void check(By by) {
        WebElement checkbox = browser.findElement(by);
        if (!checkbox.isSelected()) {
            checkbox.click();
        }
    }

    public void click(By by) {
        browser.findElement(by).click();
    }

    public void clickAndWaitHttp(By by) {
        guardHttp(browser.findElement(by)).click();
    }

    public void clickAndWaitAjax(By by) {
        guardAjax(browser.findElement(by)).click();
    }

    public void type(By by, CharSequence text, boolean clear) {
        WebElement elem = browser.findElement(by);
        if (clear) {
            elem.clear();
        }
        elem.sendKeys(text);
    }

    public void type(By by, CharSequence text) {
        type(by, text, true);
    }

    public void setTextInputValue(By by, String value) {
        ((JavascriptExecutor) browser).executeScript("document.getElementById(arguments[0]).value = arguments[1]", browser.findElement(by).getAttribute("id"), value);
    }

    public boolean isTextInSource(String text) {
        //return (Boolean) Graphene.element(By.tagName("body")).text().contains(text).apply(browser);
        return browser.getPageSource().contains(text);
    }

    public boolean isTextOnPage(String text) {
        return (Boolean) browser.findElement(By.tagName("body")).getText().contains(text);
    }

    public void selectByValue(By by, Object value) {
        new Select(browser.findElement(by)).selectByValue(String.valueOf(value));
    }

    public void selectByText(By by, Object value) {
        new Select(browser.findElement(by)).selectByVisibleText(String.valueOf(value));
    }

    public void selectByIndex(By by, int index) {
       new Select(browser.findElement(by)).selectByIndex(index);
    }

    public void openWindow(String url, String name) {
        ((JavascriptExecutor) browser).executeScript(MessageFormat.format("window.open(\"{0}\", \"{1}\")", url, name));
    }

    public void selectWindow(String windowNameOrHandle) {
        selectWindow(windowNameOrHandle, true);
    }

    public void selectWindow(String windowNameOrHandle, boolean refresh) {
        browser.switchTo().window(windowNameOrHandle);
        if (refresh) {
            browser.navigate().refresh();
        }
    }

    public void open(String url) {
        browser.navigate().to(url);
        sleep(1000);// the Navigation.to doesn't seem to block as well as it should
    }

    public int getXpathCount(By by) {
        return browser.findElements(by).size();
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
        }
    }

    public boolean isRealBrowser() {
        String webDriverClass = browser.getClass().toString().toLowerCase();
        return !webDriverClass.contains("phantom") && !webDriverClass.contains("htmlunit");
    }
}
