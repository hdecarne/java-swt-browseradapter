/*
 * Copyright (c) 2020 Holger de Carne and contributors, All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.carne.swt.browseradapter.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.AuthenticationEvent;
import org.eclipse.swt.browser.AuthenticationListener;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Assertions;

import de.carne.swt.browseradapter.BrowserAdapter;
import de.carne.test.swt.tester.MainFunction;
import de.carne.util.Late;

class BrowserTestApplication implements MainFunction {

	static final String TITLE_RUNNING = "Running...";
	static final String TITLE_COMPLETED = "Completed";

	private final Late<Shell> shellHolder = new Late<>();
	private final Late<BrowserAdapter> browserHolder = new Late<>();

	private static final String TEST_URL = "https://www.google.com";
	private static final String TEST_COOKIE = "test=true;path=/";
	private static final String TEST_COOKIE_NAME = "test";
	private static final String TEST_COOKIE_VALUE = "true";
	private static final String LISTENER_LOG = "";

	private final AuthenticationListener authenticationListener = this::onAuthenticate;
	private final CloseWindowListener closeWindowListener = this::onWindowClose;
	private final OpenWindowListener openWindowListener = this::onWindowOpen;
	private final VisibilityWindowListener visibilityWindowListener = VisibilityWindowListener
			.hideAdapter(this::onWindowHide);
	private final LocationListener locationListener = LocationListener.changedAdapter(this::onLocationChanged);
	private final ProgressListener progressListener = ProgressListener.changedAdapter(this::onProgressChanged);
	private final StatusTextListener statusTextListener = this::onStatusTextChanged;
	private final TitleListener titleListener = this::onTitleChanged;

	private final StringBuilder listenerLogBuffer = new StringBuilder();

	@Override
	public void main(String[] args) {
		Display display = new Display();
		Shell shell = this.shellHolder.set(new Shell(display));
		BrowserAdapter browser = this.browserHolder.set(BrowserAdapter.getInstance(shell, SWT.NONE, args));

		addBrowserListeners();

		shell.setText(TITLE_RUNNING);
		shell.setLayout(new FillLayout());
		shell.open();

		assertBrowserProvider(args);
		assertBrowserType();
		assertBrowserWidget(shell);
		assertBrowserNavigation();
		assertBrowserCookies();
		assertBrowserJavascript();
		browser.setUrl(TEST_URL);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();

		Assertions.assertEquals(LISTENER_LOG, this.listenerLogBuffer.toString());
	}

	private void addBrowserListeners() {
		BrowserAdapter browser = this.browserHolder.get();

		browser.addAuthenticationListener(this.authenticationListener);
		browser.addCloseWindowListener(this.closeWindowListener);
		browser.addOpenWindowListener(this.openWindowListener);
		browser.addVisibilityWindowListener(this.visibilityWindowListener);
		browser.addLocationListener(this.locationListener);
		browser.addProgressListener(this.progressListener);
		browser.addStatusTextListener(this.statusTextListener);
		browser.addTitleListener(this.titleListener);
	}

	private void removeBrowserListeners() {
		BrowserAdapter browser = this.browserHolder.get();

		browser.removeAuthenticationListener(this.authenticationListener);
		browser.removeCloseWindowListener(this.closeWindowListener);
		browser.removeOpenWindowListener(this.openWindowListener);
		browser.removeVisibilityWindowListener(this.visibilityWindowListener);
		browser.removeLocationListener(this.locationListener);
		browser.removeProgressListener(this.progressListener);
		browser.removeStatusTextListener(this.statusTextListener);
		browser.removeTitleListener(this.titleListener);
	}

	private void onAuthenticate(AuthenticationEvent event) {
		this.listenerLogBuffer.append("onAuthenticate;");
	}

	private void onWindowClose(WindowEvent event) {
		this.listenerLogBuffer.append("onWindowClose;");
		removeBrowserListeners();
	}

	private void onWindowOpen(WindowEvent event) {
		this.listenerLogBuffer.append("onWindowOpen;");
	}

	private void onWindowHide(WindowEvent event) {
		this.listenerLogBuffer.append("onWindowHide;");
	}

	private void onLocationChanged(LocationEvent event) {
		this.listenerLogBuffer.append("onLocationChanged;");
	}

	private void onProgressChanged(ProgressEvent event) {
		this.listenerLogBuffer.append("onProgressChanged;");
		if (event.current >= event.total) {
			this.shellHolder.get().setText(TITLE_COMPLETED);
		}
	}

	private void onStatusTextChanged(StatusTextEvent event) {
		this.listenerLogBuffer.append("onStatusTextChanged;");
	}

	private void onTitleChanged(TitleEvent event) {
		this.listenerLogBuffer.append("onTitleChanged;");
	}

	private void assertBrowserProvider(String[] args) {
		@SuppressWarnings("null") Set<String> expectedProviders = new HashSet<>(Arrays.asList(args));
		String actualProvider = this.browserHolder.get().provider().name();

		Assertions.assertTrue(expectedProviders.contains(actualProvider), "Unepexted browser type: " + actualProvider);
	}

	private void assertBrowserWidget(Shell shell) {
		Assertions.assertEquals(shell, this.browserHolder.get().getBrowserWidget().getParent());
	}

	private void assertBrowserType() {
		Set<String> expectedTypes = new HashSet<>(Arrays.asList("webkit", "ie"));
		String actualType = this.browserHolder.get().getBrowserType();

		Assertions.assertTrue(expectedTypes.contains(actualType), "Unepexted browser type: " + actualType);
	}

	private void assertBrowserNavigation() {
		BrowserAdapter browser = this.browserHolder.get();

		Assertions.assertFalse(browser.isBackEnabled());
		Assertions.assertFalse(browser.back());
		Assertions.assertFalse(browser.isForwardEnabled());
		Assertions.assertFalse(browser.forward());
	}

	private void assertBrowserCookies() {
		BrowserAdapter browser = this.browserHolder.get();

		browser.setCookie(TEST_COOKIE, TEST_URL);

		Assertions.assertEquals(TEST_COOKIE_VALUE, browser.getCookie(TEST_COOKIE_NAME, TEST_URL));
	}

	private void assertBrowserJavascript() {
		BrowserAdapter browser = this.browserHolder.get();

		Assertions.assertTrue(browser.getJavascriptEnabled());

		browser.setJavascriptEnabled(false);

		Assertions.assertFalse(browser.getJavascriptEnabled());

		browser.setJavascriptEnabled(true);

		Assertions.assertTrue(browser.getJavascriptEnabled());
	}

}
