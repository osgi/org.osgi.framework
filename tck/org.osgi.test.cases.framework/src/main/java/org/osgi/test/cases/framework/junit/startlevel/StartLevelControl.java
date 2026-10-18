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

package org.osgi.test.cases.framework.junit.startlevel;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.test.cases.framework.junit.tck.BundleEventCollector;
import org.osgi.test.cases.framework.junit.tck.FrameworkEventCollector;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;

public class StartLevelControl {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;
	private static final int ZERO = 0;
	private static final int NEGATIVE = -5;
	private FrameworkStartLevel		fsl;
	private int						ibsl;
	private int						origSl;
	private int						sl_4;
	private int						sl_5;
	private int						sl_6;
	private int						sl_10;
	private int						sl_15;
	private int						sl_20;

	private static int				SLEEP	= 2000;

	private static int				TIMEOUT	= 600000;	// 10 min.
	private FrameworkEventCollector	fec;
	private BundleEventCollector	bec;

	@BeforeEach
	public void setUp() throws Exception {
		String sleepTimeString = bundleContext.getProperty("osgi.tc.startlevel.sleeptime");
		int sleepTime = SLEEP;
		if (sleepTimeString != null) {
			try {
				sleepTime = Integer.parseInt(sleepTimeString);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error while parsing sleep value! The default one will be used : "
						+ SLEEP);
			}
			if (sleepTime < 200) {
				System.out.println("The sleep value is too low : " + sleepTime
						+ " ! The default one will be used : " + SLEEP);
			}
			else {
				SLEEP = sleepTime;
			}
		}
		sleepTimeString = bundleContext.getProperty("osgi.tc.startlevel.timeout");
		if (sleepTimeString != null) {
			try {
				TIMEOUT = Integer.parseInt(sleepTimeString);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error while parsing timeout value! The default one will be used : "
						+ TIMEOUT);
			}
		}
		fsl = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		ibsl = fsl.getInitialBundleStartLevel();
		origSl = fsl.getStartLevel();
		int min = ibsl > origSl ? ibsl : origSl;
		sl_4 = min + 4;
		sl_5 = min + 5;
		sl_6 = min + 6;
		sl_10 = min + 10;
		sl_15 = min + 15;
		sl_20 = min + 20;
		fec = new FrameworkEventCollector(FrameworkEvent.STARTLEVEL_CHANGED);
		bundleContext.addFrameworkListener(fec);
		bec = new BundleEventCollector(BundleEvent.STARTED
				| BundleEvent.STOPPED);
		bundleContext.addBundleListener(bec);
	}

	@AfterEach
	public void tearDown() throws Exception {
		fec.clear();
		fsl.setInitialBundleStartLevel(ibsl);
		fsl.setStartLevel(origSl, (FrameworkListener[]) null);
		fec.getList(1, TIMEOUT);
		bundleContext.removeFrameworkListener(fec);
		bundleContext.removeBundleListener(bec);
		fsl = null;
	}

	@Test
	public void testInitialBundleStartLevel() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStartEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStopEvents = new ArrayList<>();

		fsl.setInitialBundleStartLevel(sl_20);
		assertThat(fsl
				.getInitialBundleStartLevel())
				.as("getInitialBundleStartLevel")
				.isEqualTo(sl_20);

		fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);

		Bundle tb1 = bundleInstaller.installBundle("startlevel.tb1.jar", false);
		Bundle tb2 = null;
		try {
			tb1.start();
			assertThat(inState(tb1,
					Bundle.INSTALLED | Bundle.RESOLVED))
					.as("getState() = INSTALLED | RESOLVED")
					.isTrue();

			fsl.setInitialBundleStartLevel(sl_10);
			tb2 = bundleInstaller.installBundle("startlevel.tb2.jar", false);
			tb2.start();
			expectedBundleStartEvents.add(new BundleEvent(BundleEvent.STARTED,
					tb2));
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);
			assertThat(tb2)
					.as("getState() = ACTIVE")
				.isInState(Bundle.ACTIVE);
			tb1.uninstall();
			tb1 = null;
			tb2.stop();
			expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED,
					tb2));
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
		}
		finally {
			if (tb1 != null) {
				tb1.uninstall();
			}
			if (tb2 != null) {
				tb2.uninstall();
			}
		}
	}

	@Test
	public void testStartOrder() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStartEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStopEvents = new ArrayList<>();

		fsl.setInitialBundleStartLevel(sl_20);
		fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);

		Bundle tb1 = bundleInstaller.installBundle("startlevel.tb1.jar", false);
		tb1.start();
		Bundle tb2 = bundleInstaller.installBundle("startlevel.tb2.jar", false);
		tb2.start();
		try {
			// start tb1 and tb2
			fsl.setStartLevel(sl_20, (FrameworkListener[]) null);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			expectedBundleStartEvents.add(new BundleEvent(BundleEvent.STARTED,
					tb1));
			expectedBundleStartEvents.add(new BundleEvent(BundleEvent.STARTED,
					tb2));
			assertThat(bec.getListSorted( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);

			// stop tb2 and tb1
			fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED,
					tb1));
			expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED,
					tb2));
			assertThat(bec.getListSorted( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);

			// reverse the start order
			BundleStartLevel bsl = tb2.adapt(BundleStartLevel.class);
			bsl.setStartLevel(sl_15);
			Thread.sleep(SLEEP);

			// start tb2 and tb1
			fsl.setStartLevel(sl_20, (FrameworkListener[]) null);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			expectedBundleStartEvents.clear();
			expectedBundleStartEvents.add(new BundleEvent(BundleEvent.STARTED,
					tb2));
			expectedBundleStartEvents.add(new BundleEvent(BundleEvent.STARTED,
					tb1));
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);

			// stop tb1 and tb2
			fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			expectedBundleStopEvents.clear();
			expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED,
					tb1));
			expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED,
					tb2));
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
		}
		finally {
			tb1.uninstall();
			tb2.uninstall();
		}
	}

	@Test
	public void testSetStartLevel() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<FrameworkEvent>();
		ArrayList<BundleEvent> expectedBundleStartEvents = new ArrayList<BundleEvent>();
		ArrayList<BundleEvent> expectedBundleStopEvents = new ArrayList<BundleEvent>();

		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));

		FrameworkEventCollector[] testListeners = new FrameworkEventCollector[] {
				new FrameworkEventCollector(-1), new FrameworkEventCollector(-1), new FrameworkEventCollector(-1)
		};

		fsl.setInitialBundleStartLevel(sl_15);
		fsl.setStartLevel(sl_10, testListeners);
		checkFrameworkEvents(expectedFrameworkEvents, testListeners);

		Bundle tb1 = bundleInstaller.installBundle("startlevel.tb1.jar", false);

		expectedBundleStartEvents
				.add(new BundleEvent(BundleEvent.STARTED, tb1));
		expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED, tb1));
		try {
			fsl.setStartLevel(sl_20, testListeners);
			checkFrameworkEvents(expectedFrameworkEvents, testListeners);
			assertThat(inState(tb1,
					Bundle.INSTALLED | Bundle.RESOLVED))
					.as("getState() = INSTALLED | RESOLVED")
					.isTrue();

			fsl.setStartLevel(sl_10, testListeners);
			checkFrameworkEvents(expectedFrameworkEvents, testListeners);
			assertThat(inState(tb1,
					Bundle.INSTALLED | Bundle.RESOLVED))
					.as("getState() = INSTALLED | RESOLVED")
					.isTrue();

			tb1.start();
			assertThat(inState(tb1,
					Bundle.INSTALLED | Bundle.RESOLVED))
					.as("getState() = INSTALLED | RESOLVED")
					.isTrue();

			fsl.setStartLevel(sl_20, testListeners);
			checkFrameworkEvents(expectedFrameworkEvents, testListeners);
			assertThat(tb1)
					.as("getState() = ACTIVE")
				.isInState(Bundle.ACTIVE);
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);

			fsl.setStartLevel(sl_10, testListeners);
			checkFrameworkEvents(expectedFrameworkEvents, testListeners);
			assertThat(inState(tb1,
					Bundle.INSTALLED | Bundle.RESOLVED))
					.as("getState() = INSTALLED | RESOLVED")
					.isTrue();
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);

			tb1.stop();
			fsl.setStartLevel(sl_20, testListeners);
			checkFrameworkEvents(expectedFrameworkEvents, testListeners);
			assertThat(inState(tb1,
					Bundle.INSTALLED | Bundle.RESOLVED))
					.as("getState() = INSTALLED | RESOLVED")
					.isTrue();
		}
		finally {
			tb1.uninstall();
		}
	}

	private void checkFrameworkEvents(List<FrameworkEvent> expectedFrameworkEvents, FrameworkEventCollector[] callerFrameworkEvents) {
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);
		if (callerFrameworkEvents != null) {
			for (int i = 0; i < callerFrameworkEvents.length; i++) {
				assertThat(callerFrameworkEvents[i].getList(expectedFrameworkEvents .size(), TIMEOUT))
						.as("Wrong events for caller listeners " + i)
						.usingElementComparator(fec.getComparator())
						.containsExactlyElementsOf(expectedFrameworkEvents);
			}
		}
	}

	@Test
	public void testSetBundleStartLevel() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStartEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStopEvents = new ArrayList<>();

		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));

		fsl.setInitialBundleStartLevel(sl_15);
		fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);

		Bundle tb1 = bundleInstaller.installBundle("startlevel.tb1.jar", false);

		expectedBundleStartEvents
				.add(new BundleEvent(BundleEvent.STARTED, tb1));
		expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED, tb1));
		try {
			BundleStartLevel bsl = tb1.adapt(BundleStartLevel.class);
			bsl.setStartLevel(sl_5);
			Thread.sleep(SLEEP);
			assertThat(inState(tb1, Bundle.INSTALLED
					| Bundle.RESOLVED))
					.as("Startlevel 10/5 stop")
					.isTrue();

			bsl.setStartLevel(sl_15);
			Thread.sleep(SLEEP);
			assertThat(inState(tb1, Bundle.INSTALLED
					| Bundle.RESOLVED))
					.as("StartLevel 10/15 stop")
					.isTrue();

			tb1.start();
			assertThat(inState(tb1, Bundle.INSTALLED
					| Bundle.RESOLVED))
					.as("StartLevel 10/15 start")
					.isTrue();

			bsl.setStartLevel(sl_5);
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);
			assertThat(tb1)
					.as("StartLevel 10/5 start")
				.isInState(Bundle.ACTIVE);

			bsl.setStartLevel(sl_15);
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
			assertThat(inState(tb1, Bundle.INSTALLED
					| Bundle.RESOLVED))
					.as("StartLevel 10/15 start")
					.isTrue();

			bsl.setStartLevel(sl_5);
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);
			assertThat(tb1)
					.as("StartLevel 10/5 start")
				.isInState(Bundle.ACTIVE);

			tb1.stop();
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
			assertThat(inState(tb1, Bundle.INSTALLED | Bundle.RESOLVED))
					.as("stop")
					.isTrue();
		}
		finally {
			tb1.uninstall();
		}
	}

	@Test
	public void testPersistentlyStarted() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStartEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStopEvents = new ArrayList<>();

		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));

		fsl.setInitialBundleStartLevel(sl_15);
		assertThat(fsl
				.getInitialBundleStartLevel())
				.as("setInitialBundleStartLevel")
				.isEqualTo(sl_15);

		fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);

		Bundle tb1 = bundleInstaller.installBundle("startlevel.tb1.jar", false);

		expectedBundleStartEvents
				.add(new BundleEvent(BundleEvent.STARTED, tb1));
		expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED, tb1));
		try {
			BundleStartLevel bsl = tb1.adapt(BundleStartLevel.class);
			assertThat(bsl
					.isPersistentlyStarted())
					.as("isBundlePersistentlyStarted").isFalse();

			tb1.start();
			assertThat(bsl
					.isPersistentlyStarted())
					.as("isBundlePersistentlyStarted").isTrue();

			fsl.setStartLevel(sl_20, (FrameworkListener[]) null);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);
			assertThat(bsl
					.isPersistentlyStarted())
					.as("isBundlePersistentlyStarted").isTrue();

			fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
			assertThat(bsl
					.isPersistentlyStarted())
					.as("isBundlePersistentlyStarted").isTrue();

			bsl.setStartLevel(sl_5);
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle started event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);
			assertThat(bsl
					.isPersistentlyStarted())
					.as("isBundlePersistentlyStarted").isTrue();

			bsl.setStartLevel(sl_15);
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle stopped event")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
			assertThat(bsl
					.isPersistentlyStarted())
					.as("isBundlePersistentlyStarted").isTrue();
		}
		finally {
			tb1.uninstall();
		}
	}

	@Test
	public void testSystemBundle() throws Exception {
		Bundle systemBundle = bundleContext.getBundle(0);
		BundleStartLevel bsl = systemBundle.adapt(BundleStartLevel.class);
		assertThat(bsl
				.getStartLevel())
				.as("getBundleStartLevel").isZero();
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("got no IllegalArgumentException")
				.isThrownBy(() -> bsl.setStartLevel(42));
	}

	@Test
	public void testExceptionInActivator() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<FrameworkEvent> expectedFrameworkError1 = new ArrayList<>();
		ArrayList<FrameworkEvent> expectedFrameworkError2 = new ArrayList<>();

		FrameworkEventCollector fec2 = new FrameworkEventCollector(
				FrameworkEvent.STARTLEVEL_CHANGED | FrameworkEvent.ERROR);
		bundleContext.addFrameworkListener(fec2);

		try {
			expectedFrameworkEvents.add(new FrameworkEvent(
					FrameworkEvent.STARTLEVEL_CHANGED, bundleContext
							.getBundle(0), null));

			fsl.setInitialBundleStartLevel(sl_5);
			fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
			assertThat(fec2.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec2
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);

			Bundle tb5 = bundleInstaller.installBundle("startlevel.tb5.jar", false);
			expectedFrameworkError1.add(new FrameworkEvent(
					FrameworkEvent.ERROR, tb5, new BundleException("")));
			expectedFrameworkError2.add(new FrameworkEvent(
					FrameworkEvent.ERROR, tb5, new BundleException("")));
			expectedFrameworkError2.add(new FrameworkEvent(
					FrameworkEvent.STARTLEVEL_CHANGED, bundleContext
							.getBundle(0), null));

			try {
				try {
					tb5.start();
					assertThat(inState(tb5,
							Bundle.ACTIVE))
							.as("getState() = ACTIVE").isTrue();

				}
				catch (Exception e) {
					fail("Unexpected exception: " + e.getMessage());
				}

				// FrameworkEvent.ERROR due to active startlevel change
				fsl.setStartLevel(sl_4, (FrameworkListener[]) null);
				assertThat(fec2.getList( expectedFrameworkError2.size(), TIMEOUT))
						.as("Received framework events")
						.usingElementComparator(fec2.getComparator())
						.containsExactlyElementsOf(expectedFrameworkError2);
				assertThat(inState(tb5, Bundle.INSTALLED | Bundle.RESOLVED))
						.as("getState() = INSTALLED | RESOLVED").isTrue();

				// no FrameworkEvent.ERROR
				fsl.setStartLevel(sl_5, (FrameworkListener[]) null);
				assertThat(fec2 .getList(expectedFrameworkEvents.size(), TIMEOUT))
						.as("Received start level changed event")
						.usingElementComparator(fec2
						.getComparator())
						.containsExactlyElementsOf(expectedFrameworkEvents);
				assertThat(inState(tb5,
						Bundle.ACTIVE))
						.as("getState() = ACTIVE").isTrue();

				// FrameworkEvent.ERROR due to bundle startlevel change
				BundleStartLevel bsl = tb5.adapt(BundleStartLevel.class);
				bsl.setStartLevel(sl_6);
				assertThat(fec2.getList( expectedFrameworkError1.size(), TIMEOUT))
						.as("Received framework events")
						.usingElementComparator(fec2.getComparator())
						.containsExactlyElementsOf(expectedFrameworkError1);
				assertThat(inState(tb5, Bundle.INSTALLED | Bundle.RESOLVED))
						.as("getState() = INSTALLED | RESOLVED").isTrue();
			}
			finally {
				tb5.uninstall();
			}
		}
		finally {
			bundleContext.removeFrameworkListener(fec2);
		}
	}

	@Test
	public void testActivatorChangeBundleStartLevel() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleEvents = new ArrayList<>();

		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));

		fsl.setInitialBundleStartLevel(sl_5);
		fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);
		Bundle tb3 = bundleInstaller.installBundle("startlevel.tb3.jar", false);
		expectedBundleEvents.add(new BundleEvent(BundleEvent.STARTED, tb3));
		expectedBundleEvents.add(new BundleEvent(BundleEvent.STOPPED, tb3));
		try {
			tb3.start();
			BundleStartLevel bsl = tb3.adapt(BundleStartLevel.class);
			assertThat(bsl
					.getStartLevel())
					.as("getBundleStartLevel")
					.isEqualTo(sl_15);
			assertThat(bec.getListSorted( expectedBundleEvents .size(), TIMEOUT))
					.as("Received bundle events")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleEvents);
		}
		finally {
			tb3.uninstall();
		}
	}

	@Test
	public void testActivatorChangeStartLevel() throws Exception {
		ArrayList<FrameworkEvent> expectedFrameworkEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStartEvents = new ArrayList<>();
		ArrayList<BundleEvent> expectedBundleStopEvents = new ArrayList<>();

		expectedFrameworkEvents.add(new FrameworkEvent(
				FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0),
				null));
		fsl.setInitialBundleStartLevel(sl_5);
		fsl.setStartLevel(sl_10, (FrameworkListener[]) null);
		assertThat(fec.getList(expectedFrameworkEvents .size(), TIMEOUT))
				.as("Received start level changed event")
				.usingElementComparator(fec.getComparator())
				.containsExactlyElementsOf(expectedFrameworkEvents);
		Bundle tb4 = bundleInstaller.installBundle("startlevel.tb4.jar", false);
		expectedBundleStartEvents
				.add(new BundleEvent(BundleEvent.STARTED, tb4));
		expectedBundleStopEvents.add(new BundleEvent(BundleEvent.STOPPED, tb4));
		try {
			tb4.start();
			assertThat(bec.getList( expectedBundleStartEvents.size(), TIMEOUT))
					.as("Received bundle events")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStartEvents);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			assertThat(fsl.getStartLevel())
					.as("getStartLevel")
					.isEqualTo(sl_15);

			tb4.stop();
			assertThat(bec.getList( expectedBundleStopEvents.size(), TIMEOUT))
					.as("Received bundle events")
					.usingElementComparator(bec.getComparator())
					.containsExactlyElementsOf(expectedBundleStopEvents);
			assertThat(fec.getList( expectedFrameworkEvents.size(), TIMEOUT))
					.as("Received start level changed event")
					.usingElementComparator(fec
					.getComparator())
					.containsExactlyElementsOf(expectedFrameworkEvents);
			assertThat(fsl.getStartLevel())
					.as("getStartLevel")
					.isEqualTo(sl_10);
		}
		finally {
			tb4.uninstall();
		}
	}

	@Test
	public void testExceptions(
			@InjectInstalledBundle(value = "startlevel.tb1.jar", start = false) Bundle tb1
			) throws Exception {
		try {
			assertThatExceptionOfType(IllegalArgumentException.class).as("Expected IllegalArgumentException.")
					.isThrownBy(() -> fsl.setInitialBundleStartLevel(ZERO));
			assertThatExceptionOfType(IllegalArgumentException.class).as("Expected IllegalArgumentException.")
					.isThrownBy(() -> fsl.setInitialBundleStartLevel(NEGATIVE));
			assertThatExceptionOfType(IllegalArgumentException.class).as("Expected IllegalArgumentException.")
					.isThrownBy(() -> fsl.setStartLevel(ZERO));
			assertThatExceptionOfType(IllegalArgumentException.class).as("Expected IllegalArgumentException.")
					.isThrownBy(() -> fsl.setStartLevel(NEGATIVE));
			BundleStartLevel bsl = tb1.adapt(BundleStartLevel.class);
			assertThatExceptionOfType(IllegalArgumentException.class).as("Expected IllegalArgumentException.")
					.isThrownBy(() -> bsl.setStartLevel(ZERO));
			assertThatExceptionOfType(IllegalArgumentException.class).as("Expected IllegalArgumentException.")
					.isThrownBy(() -> bsl.setStartLevel(NEGATIVE));
		} finally {
			tb1.uninstall();
		}
	}

	private boolean inState(Bundle b, int stateMask) {
		return (b.getState() & stateMask) != 0;
	}
}
