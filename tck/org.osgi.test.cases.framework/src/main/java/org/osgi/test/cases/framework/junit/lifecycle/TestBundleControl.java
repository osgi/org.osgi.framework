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
package org.osgi.test.cases.framework.junit.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

/**
 * This is the bundle initially installed and started by the TestCase when
 * started. It performs the various tests and reports back to the TestCase.
 * 
 * 
 * The testPermissions test is currently removed because of unknown state. It
 * uses tb10 but this seems to be removed from the upper makefile.
 * 
 * @author Ericsson Radio Systems AB
 */
public class TestBundleControl implements
		BundleListener, ServiceListener {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;
	private Vector<Object> _eventQueue = new Vector<>(16);

	/**
	 * Tests normal Install/Uninstall/start/stop of a bundle.
	 */
	@Test
	public void testNormalInstallStartStopUninstall() throws Exception {
		Bundle tb = bundleInstaller.installBundle("lifecycle.tb1.jar", false);
		assertThat(tb.getState() == Bundle.INSTALLED
						|| tb.getState() == Bundle.RESOLVED)
				.as("Wring bundle state: " + tb.getState())
				.isTrue();
		try {
			tb.start();
		}
		catch (BundleException e) {
			fail("Failed to start " + tb.getSymbolicName(), e);
		}
		assertThat(tb)
				.as("Wrong state for " + tb.getSymbolicName()).isInState(Bundle.ACTIVE);
		try {
			tb.stop();
		}
		catch (BundleException e) {
			fail("Failed to stop " + tb.getSymbolicName(), e);
		}
		assertThat(tb)
				.as("Wrong state for " + tb.getSymbolicName()).isInState(Bundle.RESOLVED);
		try {
			tb.uninstall();
		}
		catch (BundleException e) {
			fail("Failed to uninstall " + tb.getSymbolicName(), e);
		}
		assertThat(tb)
				.as("Wrong state for " + tb.getSymbolicName()).isInState(Bundle.UNINSTALLED);
	}

	/**
	 * Tests Start/Stop with exception.
	 */
	@Test
	public void testStartStopWithException() throws Exception {
		Bundle tb2a, tb2b;
		tb2a = bundleInstaller.installBundle("lifecycle.tb2a.jar", false);
		try {
			tb2a.start();
			fail("Testing start with exception: No exception thrown, Error!");
		}
		catch (BundleException be) {
			assertThat(tb2a)
					.as("Wrong state for " + tb2a.getSymbolicName()).isInState(Bundle.RESOLVED);
		}
		try {
			tb2a.uninstall();
		}
		catch (BundleException e) {
			fail("Failed to uninstall " + tb2a.getSymbolicName(), e);
		}
		tb2a = null;

		tb2b = bundleInstaller.installBundle("lifecycle.tb2b.jar", false);
		try {
			tb2b.start();
		}
		catch (BundleException e) {
			fail("Failed to start " + tb2b.getSymbolicName(), e);
		}
		assertThat(tb2b)
				.as("Wrong state for " + tb2b.getSymbolicName()).isInState(Bundle.ACTIVE);
		try {
			tb2b.stop();
			fail("Testing stop with exception: No exception thrown, Error!");
		}
		catch (BundleException be) {
			assertThat(tb2b)
					.as("Wrong state for " + tb2b.getSymbolicName()).isInState(Bundle.RESOLVED);
		}
		try {
			tb2b.uninstall();
		}
		catch (BundleException e) {
			fail("Failed to uninstall " + tb2b.getSymbolicName(), e);
		}
		tb2b = null;
	}

	/**
	 * Tests unavailable URL.
	 */
	@Test
	public void testUnavailableURL() throws Exception {
		assertThatExceptionOfType(BundleException.class)
				.as("No exception thrown, Error!")
				.isThrownBy(() -> bundleContext.installBundle("NoSuchBundle.jar"));
	}

	/**
	 * Tests halfway broken inputstream during install.
	 */
	@Test
	public void testBrokenStreamInstall() throws Exception {
		byte b[] = new byte[500];
		try {
			URL url = bundleContext.getBundle().getEntry("lifecycle.tb4.jar");
			assertThat(url)
					.as("lifecycle.tb4.jar is null")
				.isNotNull();
			InputStream in = url.openStream();
			// Read 500 bytes of the file
			for (int i = 0; i < 500; i += in.read(b, i, 500 - i)) {
				// empty
			}
			// Now, make an InputStream from those first 500 bytes.
			ByteArrayInputStream bin = new ByteArrayInputStream(b);
			assertThatExceptionOfType(BundleException.class).as("No exception thrown, Error!")
					.isThrownBy(() -> bundleContext.installBundle(url.toExternalForm(), bin));
		}
		catch (IOException ioe) {
			fail("Failed to read content", ioe);
		}
	}

	/**
	 * Tests halfway broken inputstream during update.
	 */
	@Test
	public void testBrokenStreamUpdate() throws Exception {
		Bundle tb = bundleInstaller.installBundle("lifecycle.tb6a.jar", false);
		try {
			tb.start();
			long Id = tb.getBundleId();
			String bsn = tb.getSymbolicName();
			Version version = tb.getVersion();

			URL url = bundleContext.getBundle().getEntry("lifecycle.tb6b.jar");
			assertThat(url)
					.as("lifecycle.tb6b.jar is null")
				.isNotNull();
			InputStream in = url.openStream();

			// Read 500 bytes of the file
			byte b[] = new byte[500];
			for (int i = 0; i < 500; i += in.read(b, i, 500 - i)) {
				// empty
			}
			// Now, make an InputStream from those first 500 bytes.
			ByteArrayInputStream bin = new ByteArrayInputStream(b);
			assertThatExceptionOfType(BundleException.class).as("No exception thrown, Error!")
					.isThrownBy(() -> tb.update(bin));

			// Make sure it is still the same bundle and active
			assertThat(tb)
					.as("Bundle is not active.").isInState(Bundle.ACTIVE);
			assertThat(tb.getBundleId())
					.as("Wrong bundle id")
				.isEqualTo(Id);
			assertThat(tb.getSymbolicName())
					.as("Wrong symbolic name.")
				.isEqualTo(bsn);
			assertThat(tb.getVersion())
					.as("Wrong version.")
				.isEqualTo(version);
		} catch (IOException ioe) {
			fail("Failed to read content", ioe);
		} finally {
			tb.uninstall();
		}
	}

	/**
	 * Used by the Event test below to receive bundle events.
	 */
	public synchronized void bundleChanged(BundleEvent be) {
		// TODO should really test for RESOLVED/UNRESOLVED. Filtering for now
		// until ...
		if ((be.getType() & (BundleEvent.RESOLVED | BundleEvent.UNRESOLVED)) != 0)
			return;
		// System.out.println("In " + _eventQueue.size() + " event : "
		// + event(be.getType()));
		_eventQueue.add(be);
		notify();
	}

	/**
	 * Used by the Event test below to receive service events.
	 */
	public void serviceChanged(ServiceEvent se) {
		_eventQueue.add(se);
	}

	/**
	 * Tests various events.
	 */
	@Test
	public void testEvents() throws Exception {
		bundleContext.addBundleListener(this);
		bundleContext.addServiceListener(this);
		Bundle tb = bundleInstaller.installBundle("lifecycle.tb5.jar", false);
		syncBundle("Get install event", BundleEvent.INSTALLED, bundleContext
				.getBundle());
		tb.start();
		syncService("EventTest", ServiceEvent.REGISTERED);
		syncBundle("Get started event", BundleEvent.STARTED, tb);
		tb.stop();
		syncService("EventTest", ServiceEvent.UNREGISTERING);
		syncBundle("Get stopped event", BundleEvent.STOPPED, tb);
		tb.uninstall();
		syncBundle("Get uninstalled event", BundleEvent.UNINSTALLED, tb);
		bundleContext.removeBundleListener(this);
		bundleContext.removeServiceListener(this);
		final boolean[] called = new boolean[] {false};
		ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent se) {
				called[0] = true;
			}
		};
		bundleContext
				.addServiceListener(sl,
						"(objectClass=org.osgi.test.cases.framework.lifecycle.tb5.EventTest)");
		tb = bundleInstaller.installBundle("lifecycle.tb5.jar", false);
		tb.start();
		tb.stop();
		tb.uninstall();
		bundleContext.removeServiceListener(sl);
		assertThat(called[0])
				.as("Listener not called")
				.isTrue();

		called[0] = false;
		bundleContext.addServiceListener(sl, "(objectClass=not)");
		tb = bundleInstaller.installBundle("lifecycle.tb5.jar", false);
		tb.start();
		tb.stop();
		tb.uninstall();
		bundleContext.removeServiceListener(sl);
		assertThat(called[0])
				.as("Listener called")
				.isFalse();
	}

	/**
	 * Tests service registrations.
	 */
	@Test
	public void testServiceRegistrations() throws Exception {
		String clazzes[];
		ServiceRegistration< ? > sr;
		ServiceReference< ? > refs[];
		Hashtable<String,Object> props;
		boolean found;
		clazzes = new String[] {TestBundleControl.class.getName(),
				Object.class.getName(),
				ServiceListener.class.getName()};
		props = new Hashtable<>();
		props.put("PropertyX", "TBC");
		sr = bundleContext.registerService(clazzes, this, props);
		refs = bundleContext.getServiceReferences(
				Object.class.getName(), null);
		assertThat(refs)
				.as("No service references found")
				.isNotNull();
		found = false;
		for (int i = 0; i < refs.length; i++)
			if ("TBC".equals(refs[i].getProperty("PropertyX")))
				found = true;
		assertThat(found)
				.as("Did not find PropertyX")
				.isTrue();
		sr.unregister();
		sr = null;
		String[] activatorClazzes = new String[] {BundleActivator.class.getName()};
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("No exception thrown, Error!")
				.isThrownBy(() -> bundleContext.registerService(activatorClazzes, this, props));
	}

	/**
	 * Tests normal update.
	 */
	@Test
	public void testNormalUpdate() throws Exception {
		Bundle tb;
		long Id;
		bundleContext.addBundleListener(this);
		tb = bundleInstaller.installBundle("lifecycle.tb6a.jar", false);
		syncBundle("Test update, get installed ", BundleEvent.INSTALLED,
				bundleContext.getBundle());
		tb.start();
		syncBundle("Test update, get started ", BundleEvent.STARTED, tb);
		Id = tb.getBundleId();
		URL url = bundleContext.getBundle().getEntry("lifecycle.tb6b.jar");
		assertThat(url)
				.as("url is null")
				.isNotNull();
		InputStream in = url.openStream();
		tb.update(in);
		syncBundle("Test update expect stopped before update",
				BundleEvent.STOPPED, tb);
		syncBundle("Test update, now get updated ", BundleEvent.UPDATED, tb);
		syncBundle("Test update, start again", BundleEvent.STARTED, tb);
		assertThat(tb.getBundleId())
				.as("Wrong bundle id")
				.isEqualTo(Id);
		tb.stop();
		syncBundle("Test update get stopped", BundleEvent.STOPPED, tb);
		tb.uninstall();
		syncBundle("Test update, get uninstalled ", BundleEvent.UNINSTALLED, tb);
		bundleContext.removeBundleListener(this);
	}

	/**
	 * Tests failed update with rollback.
	 */
	@Test
	public void testRollbackUpdate() throws Exception {
		Bundle tb;
		// update() re-reads the location, so install by URL rather than by stream
		tb = bundleContext.installBundle(bundleContext.getBundle().getEntry("lifecycle.tb7.jar").toExternalForm());
		tb.start();
		assertThatExceptionOfType(BundleException.class)
				.as("No exception thrown, Error!")
				.isThrownBy(() -> tb.update());
		assertThat(tb)
				.as("Wrong bundle state").isInState(Bundle.ACTIVE);
		tb.uninstall();
	}

	/**
	 * Tries to use a stopped BundleContext.
	 */
	@Test
	public void testStoppedBundleContext() throws Exception {
		Bundle tb = bundleInstaller.installBundle("lifecycle.tb8.jar", false);
		try {
			tb.start();
			BundleContext otherContext = tb.getBundleContext();
			tb.stop();
			// By now, this bc should be illegal to use.
			assertThatExceptionOfType(IllegalStateException.class).as("BundleContext is still usable, ERROR!")
					.isThrownBy(() -> otherContext.getServiceReference(TestBundleControl.class .getName()));
			tb.start();
			// The context should not become reusable after we restart the
			// bundle!
			assertThatExceptionOfType(IllegalStateException.class).as("BundleContext is still usable, ERROR!")
					.isThrownBy(() -> otherContext.getServiceReference(TestBundleControl.class .getName()));
		}
		finally {
			tb.stop();
			tb.uninstall();
		}
	}

	/**
	 * Tests that getLocation returns the correct location.
	 */
	@Test
	public void testGetLocation() throws Exception {
		// Use the same bundle as the "hang in stop" test, we won't start (or
		// stop) it anyway.
		URL url = bundleContext.getBundle().getEntry("lifecycle.tb9.jar");
		assertThat(url).isNotNull();
		String loc = url.toExternalForm();
		Bundle tb = bundleContext.installBundle(loc);
		try {
			assertThat(tb.getLocation())
					.as("Unexpected Location")
				.isEqualTo(loc);
		}
		finally {
			tb.uninstall();
		}
	}

	private synchronized void syncService(String test, int event) {
		if (_eventQueue.size() == 0)
			fail(test + ": Time out on event receive " + event);
		ServiceEvent se = (ServiceEvent) _eventQueue.remove(0);
		String service = (String) se.getServiceReference().getProperty(
				"Service-Name");
		assertThat(se.getType())
				.as("Wrong event type")
				.isEqualTo(event);
		assertThat(service)
				.as("Wrong service")
				.isEqualTo(test);
	}

	private synchronized void syncBundle(String test, int event, Bundle origin) {
		try {
			if (_eventQueue.size() == 0) {
				System.out.println("Waiting for event");
				wait(10000);
			}
			if (_eventQueue.size() == 0) {
				fail(test + ": Time out on event receive " + event(event));
			}
			BundleEvent be = (BundleEvent) _eventQueue.remove(0);
			assertThat(event(be.getType()))
					.as(test + ": invalid event")
				.isEqualTo(event(event));
			assertThat(be.getOrigin())
					.as("wrong origin")
				.isSameAs(origin);
		}
		catch (InterruptedException x) {/* cannot happen */
		}
	}

	private String event(int event) {
		switch (event) {
			case 0 :
				return "(NONE)";
			case BundleEvent.INSTALLED :
				return "INSTALLED(" + BundleEvent.INSTALLED + ")";
			case BundleEvent.STARTED :
				return "STARTED(" + BundleEvent.STARTED + ")";
			case BundleEvent.UPDATED :
				return "UPDATED(" + BundleEvent.UPDATED + ")";
			case BundleEvent.STOPPED :
				return "STOPPED(" + BundleEvent.STOPPED + ")";
			case BundleEvent.UNINSTALLED :
				return "UNINSTALLED(" + BundleEvent.UNINSTALLED + ")";
			case BundleEvent.RESOLVED :
				return "RESOLVED(" + BundleEvent.RESOLVED + ")";
			case BundleEvent.UNRESOLVED :
				return "UNRESOLVED(" + BundleEvent.UNRESOLVED + ")";
			case BundleEvent.STARTING :
				return "STARTING(" + BundleEvent.STARTING + ")";
			case BundleEvent.STOPPING :
				return "STOPPING(" + BundleEvent.STOPPING + ")";
			default :
				return "UNKNOWN(" + event + ")";
		}
	}
}
