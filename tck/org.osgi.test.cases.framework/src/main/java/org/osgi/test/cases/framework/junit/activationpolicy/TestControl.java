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
package org.osgi.test.cases.framework.junit.activationpolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;


/**
 * This class contains tests related with the framework class loading policies.
 * 
 * @author left
 * @author $Id$
 */
public class TestControl {

	public void compareEvents(Object[] expectedEvents, Object[] actualEvents) {
		assertEquals(expectedEvents.length, actualEvents.length, "number of results");
		for (int i = 0; i < actualEvents.length; i++) {
			if (expectedEvents[i] instanceof BundleEvent) {
				BundleEvent expected = (BundleEvent) expectedEvents[i];
				BundleEvent actual = (BundleEvent) actualEvents[i];
				assertEquals(expected.getBundle(), actual.getBundle(), "Event Bundles");
				assertEquals(expected.getType(), actual.getType(), "Event Type");
			} else if (expectedEvents[i] instanceof FrameworkEvent) {
				FrameworkEvent expected = (FrameworkEvent) expectedEvents[i];
				FrameworkEvent actual = (FrameworkEvent) actualEvents[i];
				assertEquals(expected.getSource(), actual.getSource(), "Event Bundles");
				assertEquals(expected.getType(), actual.getType(), "Event Type");
			}
		}
	}
	
	public void compareEventsUnordered(Object[] expectedEvents, Object[] actualEvents) {
		assertEquals(expectedEvents.length, actualEvents.length, "number of results");
		for (int i = 0; i < expectedEvents.length; i++) {
            boolean found = false;
            for (int j = 0; !found && (j < actualEvents.length); j++) {
                if (expectedEvents[i] instanceof BundleEvent) {
                    BundleEvent expected = (BundleEvent) expectedEvents[i];
                    BundleEvent actual = (BundleEvent) actualEvents[j];
                    found = (expected.getBundle().equals(actual.getBundle())
                        && (expected.getType() == actual.getType()));
                } else if (expectedEvents[i] instanceof FrameworkEvent) {
                    FrameworkEvent expected = (FrameworkEvent) expectedEvents[i];
                    FrameworkEvent actual = (FrameworkEvent) actualEvents[j];
                    found = (expected.getSource().equals(actual.getSource())
                        && (expected.getType() == actual.getType()));
                }
            }
            assertTrue(found, "Did not find expected event: " + expectedEvents[i]);
		}
	}

	/*
	 * Tests a simple lazy policy with no includes or excludes directives
	 */
	@Test
	public void testActivationPolicy01(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tblazy1.jar",start =  false) Bundle tblazy1,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start =  false) Bundle tblazy2,
			@InjectInstalledBundle(value = "activationpolicy.tblazy3.jar",start =  false) Bundle tblazy3,
			@InjectInstalledBundle(value = "activationpolicy.tblazy4.jar",start =  false) Bundle tblazy4
			) throws Exception {

		tblazy1.start(Bundle.START_ACTIVATION_POLICY);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		tblazy3.start(Bundle.START_ACTIVATION_POLICY);
		tblazy4.start(Bundle.START_ACTIVATION_POLICY);
		// listen for STARTED, STOPPED and LAZY_ACTIVATION events
		// we should not get LAZY_ACTIVATION events because this is not a synchronous listener.
		EventListenerTestResults resultsListener = new EventListenerTestResults(BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION);
		bundleContext.addBundleListener(resultsListener);

		tblazy1.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy1.LazySimple").getConstructor()
				.newInstance();

		// The bundle must have been activated now
		Object[] expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, tblazy2);
		Object[] actualEvents = resultsListener.getResults(1);
		compareEvents(expectedEvents, actualEvents);

	}

	/*
	 * Tests a bundle with the lazy activation policy and an excludes directive
	 */
	@Test
	public void testActivationPolicy02(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tblazy1.jar",start =  false) Bundle tblazy1,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start =  false) Bundle tblazy2,
			@InjectInstalledBundle(value = "activationpolicy.tblazy3.jar",start =  false) Bundle tblazy3,
			@InjectInstalledBundle(value = "activationpolicy.tblazy4.jar",start =  false) Bundle tblazy4
			) throws Exception {
		
		tblazy1.start(Bundle.START_ACTIVATION_POLICY);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		tblazy3.start(Bundle.START_ACTIVATION_POLICY);
		tblazy4.start(Bundle.START_ACTIVATION_POLICY);

		// listen for STARTED, STOPPED and LAZY_ACTIVATION evnets
		// we should not get LAZY_ACTIVATION events because this is not a synchronous listener.
		EventListenerTestResults resultsListener = new EventListenerTestResults(BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION);
		bundleContext.addBundleListener(resultsListener);

		// First load a class that depends on a class included in an excludes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyExclude1")
				.getConstructor()
				.newInstance();
		// this should result in no STARTED event
		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = resultsListener.getResults(0);
		compareEvents(expectedEvents, actualEvents);

		// Now load a class that was not included in an excludes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyExclude2")
				.getConstructor()
				.newInstance();
		// this should result in a STARTED event for tblazy3 bundle
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, tblazy3);
		actualEvents = resultsListener.getResults(1);
		compareEvents(expectedEvents, actualEvents);
	}

	/*
	 * Tests a bundle with the lazy activation policy and an includes directive
	 */
	@Test
	public void testActivationPolicy03(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tblazy1.jar",start =  false) Bundle tblazy1,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start =  false) Bundle tblazy2,
			@InjectInstalledBundle(value = "activationpolicy.tblazy3.jar",start =  false) Bundle tblazy3,
			@InjectInstalledBundle(value = "activationpolicy.tblazy4.jar",start =  false) Bundle tblazy4
			) throws Exception {

		tblazy1.start(Bundle.START_ACTIVATION_POLICY);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		tblazy3.start(Bundle.START_ACTIVATION_POLICY);
		tblazy4.start(Bundle.START_ACTIVATION_POLICY);
		// listen for STARTED, STOPPED and LAZY_ACTIVATION evnets
		// we should not get LAZY_ACTIVATION events because this is not a synchronous listener.
		EventListenerTestResults resultsListener = new EventListenerTestResults(BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION);
		bundleContext.addBundleListener(resultsListener);

		// first load a class that depends on a class that was not included in an includes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyInclude1")
				.getConstructor()
				.newInstance();
		// this should result in no STARTED event
		Object[] expectedEvents = new Object[0];
		Object[] actualEvents = resultsListener.getResults(0);
		compareEvents(expectedEvents, actualEvents);

		// now load a class that depends on a class that is included in an includes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyInclude2")
				.getConstructor()
				.newInstance();
		// this should result in a STARTED event
		expectedEvents = new Object[1];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, tblazy4);
		actualEvents = resultsListener.getResults(1);
		compareEvents(expectedEvents, actualEvents);
	}

	/*
	 * Tests the lazy activation policy in relation to the start-level service.
	 */
	@Test
	public void testActivationPolicy04(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
		// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
		EventListenerTestResults resultsListener = new SyncEventListenerTestResults(BundleEvent.STARTED | BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.STOPPED);
		bundleContext.addBundleListener(resultsListener);
		EventListenerTestResults startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
		bundleContext.addFrameworkListener(startlevelListener);
		try {
			// crank up the framework start-level.  This should result in no STARTED event
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			Object[] actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level,
			// but no STARTED event should be fired because nothing triggered the bundle to activate
			Object[] expectedEvents = new Object[3];
			expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			Object[] actualEvents = resultsListener.getResults(3);
			compareEvents(expectedEvents, actualEvents);

			// now load a class from it before the start-level is met.  This should result in no events
			tblazy2.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy2.ATest");
			expectedEvents = new Object[0];
			actualEvents = resultsListener.getResults(0);
			compareEvents(expectedEvents, actualEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

            // Given that the start-level was met, then depending on the framework implementation we should
            // see either:
            //   * LAZY_ACTIVATION, STARTING, STARTED, STOPPING, STOPPED events or
            //   * LAZY_ACTIVATION, STOPPING, STOPPED events
			// The difference comes from whether the framework treats the trigger as a one-time trigger or not.
			actualEvents = resultsListener.getResults(4);
            // This is the case if the trigger is a one-time event.
            if (actualEvents.length == 4)
            {
                expectedEvents = new Object[4];
                expectedEvents[0] = new BundleEvent(BundleEvent.STARTING, tblazy2);
                expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tblazy2);
                expectedEvents[2] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
                expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
                compareEvents(expectedEvents, actualEvents);
            }
            // This is the case if the trigger is NOT a one-time event.
            else
            {
                expectedEvents = new Object[3];
                expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
                expectedEvents[1] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
                expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
                compareEvents(expectedEvents, actualEvents);

                // now load a class while start-level is met.
                startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
                expectedFrameworkEvents = new Object[1];
                expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
                actualFrameworkEvents = startlevelListener.getResults(1);
                compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

                tblazy2.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy2.ATest");

                startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
                expectedFrameworkEvents = new Object[1];
                expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
                actualFrameworkEvents = startlevelListener.getResults(1);
                compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

                // Check for the proper events, STARTED should be fired here because the start-level was met
                expectedEvents = new Object[5];
                expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
                expectedEvents[1] = new BundleEvent(BundleEvent.STARTING, tblazy2);
                expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, tblazy2);
                expectedEvents[3] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
                expectedEvents[4] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
                actualEvents = resultsListener.getResults(5);
                compareEvents(expectedEvents, actualEvents);
            }
		} finally {
			startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
			bundleContext.addFrameworkListener(startlevelListener);
			startLevel.setStartLevel(initialSL, (FrameworkListener[]) null);
			startlevelListener.getResults(1);
			startLevel.setInitialBundleStartLevel(initialBSL);
			bundleContext.removeFrameworkListener(startlevelListener);
		}
	}

	/*
	 * Tests Bundle.start(START_ACTIVATION_POLICY) in relation to the start-level service
	 */
	@Test
	public void testActivationPolicy05(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		// make this a persistent start and ignore the activation policy;
		// this should not activate the bundle because the start-level is not met.
		tblazy2.start(0);
		BundleStartLevel tblazy2StartLevel = tblazy2.adapt(BundleStartLevel.class);
		assertTrue(tblazy2StartLevel.isPersistentlyStarted(), "bundle is persistently started.");
		assertTrue(!tblazy2StartLevel.isActivationPolicyUsed(), "bundle is not using activation policy.");
		// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
		// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
		EventListenerTestResults resultsListener = new SyncEventListenerTestResults(BundleEvent.STARTED | BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.STOPPED);
		bundleContext.addBundleListener(resultsListener);
		EventListenerTestResults startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
		bundleContext.addFrameworkListener(startlevelListener);
		try {
			// crank up the framework start-level.  This should result in a STARTED event because we are ingoring the activation policy
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			Object[] actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a STARTING, STARTED, STOPPING, STOPPED event to be sent here because we met the start-level
			// and we are ignoring the lazy activation policy
			Object[] expectedEvents = new Object[4];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			Object[] actualEvents = resultsListener.getResults(4);
			compareEvents(expectedEvents, actualEvents);

			// now mark the bundle to use the activation policy
			tblazy2.start(Bundle.START_ACTIVATION_POLICY);
			assertTrue(tblazy2StartLevel.isActivationPolicyUsed(), "bundle is using activation policy.");

			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level
			// but no STARTING or STARTED event because no trigger class was loaded.
			expectedEvents = new Object[3];
			expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			actualEvents = resultsListener.getResults(3);
			compareEvents(expectedEvents, actualEvents);

			// persistently stop the bundle
			tblazy2.stop();
			// no events are expected because the bundle should already be stopped
			expectedEvents = new Object[0];
			actualEvents = resultsListener.getResults(0);
			compareEvents(expectedEvents, actualEvents);
			
			// now call start(START_TRANSIENT | START_ACTIVATION_POLICY) while start-level is met.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			tblazy2.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);

			// we expect a LAZY_ACTIVATION event
			expectedEvents = new Object[1];
			expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
			actualEvents = resultsListener.getResults(1);
			compareEvents(expectedEvents, actualEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			actualEvents = resultsListener.getResults(2);
			compareEvents(expectedEvents, actualEvents);

			// make sure the bundle is not persistently started and is not using its activation policy
			assertTrue(!tblazy2StartLevel.isPersistentlyStarted(), "bundle is not persistently started.");
			assertTrue(!tblazy2StartLevel.isActivationPolicyUsed(), "bundle is not using activation policy.");
		} finally {
			startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
			bundleContext.addFrameworkListener(startlevelListener);
			startLevel.setStartLevel(initialSL, (FrameworkListener[]) null);
			startlevelListener.getResults(1);
			startLevel.setInitialBundleStartLevel(initialBSL);
			bundleContext.removeFrameworkListener(startlevelListener);
		}
	}

	/*
	 * Tests Bundle.start(START_TRANSIENT) in relation to the start-level service
	 */
	@Test
	public void testStartTransient01(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
		// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
		EventListenerTestResults resultsListener = new SyncEventListenerTestResults(BundleEvent.STARTED | BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.STOPPED);
		bundleContext.addBundleListener(resultsListener);
		EventListenerTestResults startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
		bundleContext.addFrameworkListener(startlevelListener);
		try {
			// crank up the framework start-level.  This should result in no STARTED event
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			Object[] actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level
			Object[] expectedEvents = new Object[3];
			expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			Object[] actualEvents = resultsListener.getResults(3);
			compareEvents(expectedEvents, actualEvents);

			// now call start(START_TRANSIENT) before the start-level is met.  This should result in no STARTED event
			try {
				tblazy2.start(Bundle.START_TRANSIENT);
				fail("expected a BundleException because start level is not met.");
			} catch (BundleException e) {
				// expected
			}

			expectedEvents = new Object[0];
			actualEvents = resultsListener.getResults(0);
			compareEvents(expectedEvents, actualEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level
			// but no STARTED event because the transient start was called before the start-level was met
			expectedEvents = new Object[3];
			expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			actualEvents = resultsListener.getResults(3);
			compareEvents(expectedEvents, actualEvents);

			// now call start(START_TRANSIENT) while start-level is met.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			tblazy2.start(Bundle.START_TRANSIENT);

			// we expect a LAZY_ACTIVATION event here because the start-level was met before we called Bundle.start method.
			expectedEvents = new Object[3];
			expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STARTING, tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, tblazy2);
			actualEvents = resultsListener.getResults(3);
			compareEvents(expectedEvents, actualEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			actualEvents = resultsListener.getResults(2);
			compareEvents(expectedEvents, actualEvents);
		} finally {
			startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
			bundleContext.addFrameworkListener(startlevelListener);
			startLevel.setStartLevel(initialSL, (FrameworkListener[]) null);
			startlevelListener.getResults(1);
			startLevel.setInitialBundleStartLevel(initialBSL);
			bundleContext.removeFrameworkListener(startlevelListener);
		}
	}

	/*
	 * Tests Bundle.stop(STOP_TRANSIENT) in relation to the start-level service
	 */
	@Test
	public void testStopTransient01(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
		// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
		EventListenerTestResults resultsListener = new SyncEventListenerTestResults(BundleEvent.STARTED | BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.STOPPED);
		bundleContext.addBundleListener(resultsListener);
		EventListenerTestResults startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
		bundleContext.addFrameworkListener(startlevelListener);
		try {
			// persistently start the bundle
			tblazy2.start();
			BundleStartLevel tblazy2StartLevel = tblazy2.adapt(BundleStartLevel.class);
			assertTrue(tblazy2StartLevel.isPersistentlyStarted(), "bundle is persistently started.");

			// test transient start Bundle.stop(START_TRANSIENT)
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			Object[] expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			Object[] actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a STARTING, STARTED, STOPPING, STOPPED event to be sent here because we met the start-level and we were persistently started
			// no LAZY_ACTIVATION event should be fired because this activation was not a result of a class load.
			Object[] expectedEvents = new Object[4];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTING,  tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STARTED,  tblazy2);
			expectedEvents[2] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[3] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			Object[] actualEvents = resultsListener.getResults(4);
			compareEvents(expectedEvents, actualEvents);

			// now call stop(STOP_TRANSIENT) while the start-level is met.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			// we expect a STARTING, STARTED event to be sent here because we met the start-level
			// no LAZY_ACTIVATION event should be fired because this activation was not a result of a class load
			expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tblazy2);
			actualEvents = resultsListener.getResults(2);
			compareEvents(expectedEvents, actualEvents);

			tblazy2.stop(Bundle.STOP_TRANSIENT);
			assertTrue(tblazy2StartLevel.isPersistentlyStarted(), "Bundle is persistently started.");

			// we expect a STOPPING, STOPPED event to be sent here because we met the start-level
			expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			actualEvents = resultsListener.getResults(2);
			compareEvents(expectedEvents, actualEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);


			// now set the start-level back up and check that the bundle is started again because it is persistently started.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STARTING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tblazy2);
			actualEvents = resultsListener.getResults(2);
			compareEvents(expectedEvents, actualEvents);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			expectedFrameworkEvents = new Object[1];
			expectedFrameworkEvents[0] = new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleContext.getBundle(0), null);
			actualFrameworkEvents = startlevelListener.getResults(1);
			compareEvents(expectedFrameworkEvents, actualFrameworkEvents);

			expectedEvents = new Object[2];
			expectedEvents[0] = new BundleEvent(BundleEvent.STOPPING, tblazy2);
			expectedEvents[1] = new BundleEvent(BundleEvent.STOPPED, tblazy2);
			actualEvents = resultsListener.getResults(2);
			compareEvents(expectedEvents, actualEvents);
		} finally {
			startlevelListener = new EventListenerTestResults(FrameworkEvent.STARTLEVEL_CHANGED);
			bundleContext.addFrameworkListener(startlevelListener);
			startLevel.setStartLevel(initialSL, (FrameworkListener[]) null);
			startlevelListener.getResults(1);
			startLevel.setInitialBundleStartLevel(initialBSL);
			bundleContext.removeFrameworkListener(startlevelListener);
		}
	}

	/*
	 * Tests a simple dependency chain and checks to see if lazy activation bundles got
	 * started in the correct order.
	 */
	@Test
	public void testActivationPolicyChain01(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tbchain1.jar",start =  false) Bundle tbchain1,
			@InjectInstalledBundle(value = "activationpolicy.tbchain2.jar",start =  false) Bundle tbchain2,
			@InjectInstalledBundle(value = "activationpolicy.tbchain3.jar",start =  false) Bundle tbchain3,
			@InjectInstalledBundle(value = "activationpolicy.tbchain4.jar",start =  false) Bundle tbchain4,
			@InjectInstalledBundle(value = "activationpolicy.tbchain5.jar",start =  false) Bundle tbchain5
			) throws Exception {


		tbchain1.start(Bundle.START_ACTIVATION_POLICY);
		tbchain2.start(Bundle.START_ACTIVATION_POLICY);
		tbchain3.start(Bundle.START_ACTIVATION_POLICY);
		tbchain4.start(Bundle.START_ACTIVATION_POLICY);
		tbchain5.start(Bundle.START_ACTIVATION_POLICY);

		EventListenerTestResults resultsListener = new EventListenerTestResults(BundleEvent.STARTED | BundleEvent.STOPPED);
		bundleContext.addBundleListener(resultsListener);

		tbchain1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tbchain1.SingleChainTest")
				.getConstructor()
				.newInstance();

		Object[] expectedEvents = new Object[3];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, tbchain3);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tbchain2);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, tbchain1);
		Object[] actualEvents = resultsListener.getResults(3);
		compareEvents(expectedEvents, actualEvents);
	}

	/*
	 * More advanced chain test that contains multiple class hierachies
	 * NOTE there may be too much assumption on the order the VM verifier loads interface classes when more than one is implemented.
	 */
	@Test
	public void testActivationPolicyChain02(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tbchain1.jar",start =  false) Bundle tbchain1,
			@InjectInstalledBundle(value = "activationpolicy.tbchain2.jar",start =  false) Bundle tbchain2,
			@InjectInstalledBundle(value = "activationpolicy.tbchain3.jar",start =  false) Bundle tbchain3,
			@InjectInstalledBundle(value = "activationpolicy.tbchain4.jar",start =  false) Bundle tbchain4,
			@InjectInstalledBundle(value = "activationpolicy.tbchain5.jar",start =  false) Bundle tbchain5
			) throws Exception {


		tbchain1.start(Bundle.START_ACTIVATION_POLICY);
		tbchain2.start(Bundle.START_ACTIVATION_POLICY);
		tbchain3.start(Bundle.START_ACTIVATION_POLICY);
		tbchain4.start(Bundle.START_ACTIVATION_POLICY);
		tbchain5.start(Bundle.START_ACTIVATION_POLICY);

		EventListenerTestResults resultsListener = new EventListenerTestResults(BundleEvent.STARTED | BundleEvent.STOPPED);
		bundleContext.addBundleListener(resultsListener);

		tbchain1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tbchain1.TestMultiChain")
				.getConstructor()
				.newInstance();

		Object[] expectedEvents = new Object[5];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, tbchain5);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tbchain3);
		expectedEvents[2] = new BundleEvent(BundleEvent.STARTED, tbchain4);
		expectedEvents[3] = new BundleEvent(BundleEvent.STARTED, tbchain2);
		expectedEvents[4] = new BundleEvent(BundleEvent.STARTED, tbchain1);
		Object[] actualEvents = resultsListener.getResults(5);
		compareEvents(expectedEvents, actualEvents);
	}

	/*
	 * This tests that a ClassCircularityError is not generated when the "trigger" class is loaded while a bundle
	 * is being activated as a result of a lazy activation policy
	 */
	@Test
	public void testClassCircularity(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tblazy5.jar",start = false) Bundle tblazy5,
			@InjectInstalledBundle(value = "activationpolicy.tblazy6.jar",start = false) Bundle tblazy6
			) throws Exception {

		tblazy5.start(Bundle.START_ACTIVATION_POLICY);
		tblazy6.start(Bundle.START_ACTIVATION_POLICY);

		EventListenerTestResults resultsListener = new EventListenerTestResults(BundleEvent.STARTED);
		bundleContext.addBundleListener(resultsListener);

		tblazy5.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy5.CircularityErrorTest")
				.getConstructor()
				.newInstance();
		// The order of activation is reversed here because tblazy6's activator loads a class in tblazy5 that triggers it to be activated before
		// tblazy6 is started, therefore the STARTED event should be fired for tblazy5 first.
		// It is questionable whether this is required by the specification.  It may be better just to make sure both bundles are in the ACTIVE state 
		// after the classload is successful to determine that no errors occurred.
		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = new BundleEvent(BundleEvent.STARTED, tblazy5);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tblazy6);
		Object[] actualEvents = resultsListener.getResults(2);
		compareEventsUnordered(expectedEvents, actualEvents);
	}

	/*
	 * Tests that getBundleContext works as when LAZY_ACTIVATION event is fired.
	 */
	@Test
	public void testGetBundleContext(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start = false) Bundle tblazy2
			) throws Exception {

		// listen for STARTED, STOPPED and LAZY_ACTIVATION evnets
		// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
		SyncEventListenerTestResults resultsListener = new SyncEventListenerTestResults(BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION, true);
		bundleContext.addBundleListener(resultsListener);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);

		tblazy2.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy2.ATest");
	
		// The bundle must have been activated now
		Object[] expectedEvents = new Object[2];
		expectedEvents[0] = new BundleEvent(BundleEvent.LAZY_ACTIVATION, tblazy2);
		expectedEvents[1] = new BundleEvent(BundleEvent.STARTED, tblazy2);
		Object[] actualEvents = resultsListener.getResults(2);
		compareEvents(expectedEvents, actualEvents);
		BundleContext[] contexts = resultsListener.getContexts();
		assertEquals(1, contexts.length, "number of contexts");
		assertEquals(tblazy2, contexts[0].getBundle(), "bundle context");

	}
}
