/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.framework.junit.dynpkgimport;



import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.test.cases.framework.dynpkgimport.exported.TestService;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;
import org.osgi.test.common.wiring.FrameworkWirings;

public class DynPkgImportTests implements
		FrameworkListener {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	@BeforeEach
	public void setUp() throws Exception {
		bundleContext.addFrameworkListener(this);
	}

	@AfterEach
	public void tearDown() throws Exception {
		bundleContext.removeFrameworkListener(this);
		FrameworkWirings.refreshBundles(bundleContext);
	}

	@Test
	public void testInitial() throws Exception {
		try {
			Bundle tlx = bundleInstaller.installBundle("dynpkgimport.tlx.jar", false);
			tlx.start();
			Bundle tb0 = bundleInstaller.installBundle("dynpkgimport.tb0.jar", false);
			tb0.start();
			ServiceReference<TestService> tsR = bundleContext
					.getServiceReference(TestService.class);
			if (tsR == null) {
				fail("failed to get TestService reference");
			}
			TestService ts = bundleContext.getService(tsR);
			if (ts == null) {
				fail("failed to get TestService");
			}
			ts.test1();
			bundleContext.ungetService(tsR);
			tb0.stop();
			tb0.uninstall();
			tlx.uninstall();
		}
		catch (BundleException be) {
			Throwable t = be.getNestedException();
			t.printStackTrace();
			be.printStackTrace();
			fail(be.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testBasicImport1() throws Exception {
		try {
			Bundle tlx = bundleInstaller.installBundle("dynpkgimport.tlx.jar", false);
			tlx.start();
			Bundle tb1 = bundleInstaller.installBundle("dynpkgimport.tb1.jar", false);
			tb1.start();
			ServiceReference<TestService> tsR = bundleContext
					.getServiceReference(TestService.class);
			if (tsR == null) {
				fail("failed to get TestService reference");
			}
			TestService ts = bundleContext.getService(tsR);
			if (ts == null) {
				fail("failed to get TestService");
			}
			ts.test1();
			bundleContext.ungetService(tsR);
			tb1.stop();
			tb1.uninstall();
			tlx.uninstall();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void testBasicImport2() throws Exception {
		try {
			Bundle tlx = bundleInstaller.installBundle("dynpkgimport.tlx.jar", false);
			tlx.start();
			Bundle tb2 = bundleInstaller.installBundle("dynpkgimport.tb2.jar", false);
			tb2.start();
			ServiceReference<TestService> tsR = bundleContext
					.getServiceReference(TestService.class);
			if (tsR == null) {
				fail("failed to get TestService reference");
			}
			TestService ts = bundleContext.getService(tsR);
			if (ts == null) {
				fail("failed to get TestService");
			}
			ts.test1();
			bundleContext.ungetService(tsR);
			tb2.stop();
			tb2.uninstall();
			tlx.uninstall();
		}
		catch (Throwable t) {
			t.printStackTrace();
			fail(t.toString());
		}
	}

	@Test
	public void testPrecedence() throws Exception {
		Bundle tlx = bundleInstaller.installBundle("dynpkgimport.tlx.jar", false);
		tlx.start();
		Bundle tb3 = bundleInstaller.installBundle("dynpkgimport.tb3.jar", false);
		tb3.start();
		ServiceReference<TestService> tsR = bundleContext
				.getServiceReference(TestService.class);
		if (tsR == null) {
			fail("failed to get TestService reference");
		}
		TestService ts = bundleContext.getService(tsR);
		if (ts == null) {
			fail("failed to get TestService");
		}
		ts.test1();
		bundleContext.ungetService(tsR);
		tb3.stop();
		tb3.uninstall();
		tlx.uninstall();
	}

	@Test
	public void testUninstall() throws Exception {
		Bundle tlx = bundleInstaller.installBundle("dynpkgimport.tlx.jar", false);
		tlx.start();
		tlx.stop();
		tlx.uninstall();
		FrameworkWirings.refreshBundles(bundleContext);
		Bundle tb2 = bundleInstaller.installBundle("dynpkgimport.tb2.jar", false);
		tb2.start();
		ServiceReference<TestService> tsR = bundleContext
				.getServiceReference(TestService.class);
		if (tsR == null) {
			fail("failed to get TestService reference");
		}
		TestService ts = bundleContext.getService(tsR);
		if (ts == null) {
			fail("failed to get TestService");
		}
		try {
			ts.test1();
		}
		catch (NoClassDefFoundError e) {
			bundleContext.ungetService(tsR);
			tb2.stop();
			tb2.uninstall();
			return;
		}
//		System.out.println("ts class " + ts.getClass());
//		System.out.println("tlx state" + tlx.getState());
		// Write some wiring API code to see what bundle exports:
		// "org.osgi.test.cases.framework.dynpkgimport.tlx");
//		System.out.println("tlx pack " + ep.getExportingBundle().getLocation());
		fail("got no NoClassDefFoundError");
	}

	public void frameworkEvent(FrameworkEvent event) {
		switch (event.getType()) {
			case FrameworkEvent.ERROR :
				System.out.println("got framework event " + event.getType() + ": "
						+ event.getThrowable().getClass().getName());
				System.out.println("stack trace:");
				event.getThrowable().printStackTrace();
				break;
			default :
				//System.out.println("got framework event " + event.getType() + ": ");
				break;
		}
	}
}
