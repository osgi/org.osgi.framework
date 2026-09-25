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

import static org.assertj.core.api.Assertions.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.osgi.test.assertj.event.TimedEventListAssert.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.test.assertj.bundleevent.BundleEventConditions;
import org.osgi.test.assertj.frameworkevent.FrameworkEventConditions;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectEventRecorder;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.event.EventRecorder;
import org.osgi.test.common.event.TimedEvent;
import org.osgi.test.common.install.BundleInstaller;


/**
 * This class contains tests related with the framework class loading policies.
 * 
 * @author left
 * @author $Id$
 */
public class TestControl {

	/** How long to wait for events that are expected to arrive. */
	private static final Duration	TIMEOUT	= Duration.ofSeconds(5);
	/** How long the framework must be quiet before we conclude that no (further) event arrives. */
	private static final Duration	QUIET	= Duration.ofMillis(500);

	private static final int		BUNDLE_EVENTS	= BundleEvent.STARTED | BundleEvent.LAZY_ACTIVATION
		| BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.STOPPED;

	private static Condition<BundleEvent> event(int type, Bundle bundle) {
		return allOf(BundleEventConditions.type(type), BundleEventConditions.bundle(bundle));
	}

	private static Condition<FrameworkEvent> startLevelChanged(BundleContext bundleContext) {
		return allOf(FrameworkEventConditions.type(FrameworkEvent.STARTLEVEL_CHANGED),
			FrameworkEventConditions.bundle(bundleContext.getBundle(0)));
	}

	/**
	 * Wait for the STARTLEVEL_CHANGED event of a preceding setStartLevel call.
	 */
	private static void assertStartLevelChanged(EventRecorder<FrameworkEvent> startLevelEvents,
		BundleContext bundleContext) throws InterruptedException {
		assertThat(startLevelEvents.waitForCount(1, TIMEOUT)).hasEventsExactly(startLevelChanged(bundleContext));
	}

	/**
	 * Restore the start level after a start-level test. Waits for the
	 * resulting STARTLEVEL_CHANGED event but does not assert on it.
	 */
	private static void restoreStartLevel(FrameworkStartLevel startLevel, int initialSL, int initialBSL,
		EventRecorder<FrameworkEvent> startLevelEvents) throws InterruptedException {
		startLevelEvents.drain();
		startLevel.setStartLevel(initialSL, (FrameworkListener[]) null);
		startLevelEvents.collect(1, TIMEOUT);
		startLevel.setInitialBundleStartLevel(initialBSL);
	}

	/*
	 * Tests a simple lazy policy with no includes or excludes directives
	 */
	@Test
	public void testActivationPolicy01(
			@InjectInstalledBundle(value = "activationpolicy.tblazy1.jar",start =  false) Bundle tblazy1,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start =  false) Bundle tblazy2,
			@InjectInstalledBundle(value = "activationpolicy.tblazy3.jar",start =  false) Bundle tblazy3,
			@InjectInstalledBundle(value = "activationpolicy.tblazy4.jar",start =  false) Bundle tblazy4,
			// listen for STARTED, STOPPED and LAZY_ACTIVATION events
			// we should not get LAZY_ACTIVATION events because this is not a synchronous listener.
			@InjectEventRecorder(typeMask = BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION) EventRecorder<BundleEvent> events
			) throws Exception {

		tblazy1.start(Bundle.START_ACTIVATION_POLICY);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		tblazy3.start(Bundle.START_ACTIVATION_POLICY);
		tblazy4.start(Bundle.START_ACTIVATION_POLICY);

		tblazy1.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy1.LazySimple").getConstructor()
				.newInstance();

		// The bundle must have been activated now
		assertThat(events.waitForCount(1, TIMEOUT)).hasEventsExactly(event(BundleEvent.STARTED, tblazy2));

	}

	/*
	 * Tests a bundle with the lazy activation policy and an excludes directive
	 */
	@Test
	public void testActivationPolicy02(
			@InjectInstalledBundle(value = "activationpolicy.tblazy1.jar",start =  false) Bundle tblazy1,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start =  false) Bundle tblazy2,
			@InjectInstalledBundle(value = "activationpolicy.tblazy3.jar",start =  false) Bundle tblazy3,
			@InjectInstalledBundle(value = "activationpolicy.tblazy4.jar",start =  false) Bundle tblazy4,
			// listen for STARTED, STOPPED and LAZY_ACTIVATION evnets
			// we should not get LAZY_ACTIVATION events because this is not a synchronous listener.
			@InjectEventRecorder(typeMask = BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION) EventRecorder<BundleEvent> events
			) throws Exception {
		
		tblazy1.start(Bundle.START_ACTIVATION_POLICY);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		tblazy3.start(Bundle.START_ACTIVATION_POLICY);
		tblazy4.start(Bundle.START_ACTIVATION_POLICY);

		// First load a class that depends on a class included in an excludes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyExclude1")
				.getConstructor()
				.newInstance();
		// this should result in no STARTED event
		assertThat(events.collectQuiet(0, QUIET, TIMEOUT)).isEmpty();

		// Now load a class that was not included in an excludes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyExclude2")
				.getConstructor()
				.newInstance();
		// this should result in a STARTED event for tblazy3 bundle
		assertThat(events.waitForCount(1, TIMEOUT)).hasEventsExactly(event(BundleEvent.STARTED, tblazy3));
	}

	/*
	 * Tests a bundle with the lazy activation policy and an includes directive
	 */
	@Test
	public void testActivationPolicy03(
			@InjectInstalledBundle(value = "activationpolicy.tblazy1.jar",start =  false) Bundle tblazy1,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start =  false) Bundle tblazy2,
			@InjectInstalledBundle(value = "activationpolicy.tblazy3.jar",start =  false) Bundle tblazy3,
			@InjectInstalledBundle(value = "activationpolicy.tblazy4.jar",start =  false) Bundle tblazy4,
			// listen for STARTED, STOPPED and LAZY_ACTIVATION evnets
			// we should not get LAZY_ACTIVATION events because this is not a synchronous listener.
			@InjectEventRecorder(typeMask = BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION) EventRecorder<BundleEvent> events
			) throws Exception {

		tblazy1.start(Bundle.START_ACTIVATION_POLICY);
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		tblazy3.start(Bundle.START_ACTIVATION_POLICY);
		tblazy4.start(Bundle.START_ACTIVATION_POLICY);

		// first load a class that depends on a class that was not included in an includes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyInclude1")
				.getConstructor()
				.newInstance();
		// this should result in no STARTED event
		assertThat(events.collectQuiet(0, QUIET, TIMEOUT)).isEmpty();

		// now load a class that depends on a class that is included in an includes package
		tblazy1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy1.LazyInclude2")
				.getConstructor()
				.newInstance();
		// this should result in a STARTED event
		assertThat(events.waitForCount(1, TIMEOUT)).hasEventsExactly(event(BundleEvent.STARTED, tblazy4));
	}

	/*
	 * Tests the lazy activation policy in relation to the start-level service.
	 */
	@Test
	public void testActivationPolicy04(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller,
			// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
			// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
			@InjectEventRecorder(typeMask = BUNDLE_EVENTS, synchronous = true) EventRecorder<BundleEvent> events,
			@InjectEventRecorder(typeMask = FrameworkEvent.STARTLEVEL_CHANGED) EventRecorder<FrameworkEvent> startLevelEvents
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		try {
			// crank up the framework start-level.  This should result in no STARTED event
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level,
			// but no STARTED event should be fired because nothing triggered the bundle to activate
			assertThat(events.waitForCount(3, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.LAZY_ACTIVATION, tblazy2),
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// now load a class from it before the start-level is met.  This should result in no events
			tblazy2.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy2.ATest");
			assertThat(events.drain()).isEmpty();

			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

            // Given that the start-level was met, then depending on the framework implementation we should
            // see either:
            //   * LAZY_ACTIVATION, STARTING, STARTED, STOPPING, STOPPED events or
            //   * LAZY_ACTIVATION, STOPPING, STOPPED events
			// The difference comes from whether the framework treats the trigger as a one-time trigger or not.
			List<TimedEvent<BundleEvent>> actualEvents = events.collect(4, QUIET);
            // This is the case if the trigger is a one-time event.
            if (actualEvents.size() == 4)
            {
				assertThat(actualEvents).hasEventsExactly(
					event(BundleEvent.STARTING, tblazy2),
					event(BundleEvent.STARTED, tblazy2),
					event(BundleEvent.STOPPING, tblazy2),
					event(BundleEvent.STOPPED, tblazy2));
            }
            // This is the case if the trigger is NOT a one-time event.
            else
            {
				assertThat(actualEvents).hasEventsExactly(
					event(BundleEvent.LAZY_ACTIVATION, tblazy2),
					event(BundleEvent.STOPPING, tblazy2),
					event(BundleEvent.STOPPED, tblazy2));

                // now load a class while start-level is met.
                startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
				assertStartLevelChanged(startLevelEvents, bundleContext);

                tblazy2.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy2.ATest");

                startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
				assertStartLevelChanged(startLevelEvents, bundleContext);

                // Check for the proper events, STARTED should be fired here because the start-level was met
				assertThat(events.waitForCount(5, TIMEOUT)).hasEventsExactly(
					event(BundleEvent.LAZY_ACTIVATION, tblazy2),
					event(BundleEvent.STARTING, tblazy2),
					event(BundleEvent.STARTED, tblazy2),
					event(BundleEvent.STOPPING, tblazy2),
					event(BundleEvent.STOPPED, tblazy2));
            }
		} finally {
			restoreStartLevel(startLevel, initialSL, initialBSL, startLevelEvents);
		}
	}

	/*
	 * Tests Bundle.start(START_ACTIVATION_POLICY) in relation to the start-level service
	 */
	@Test
	public void testActivationPolicy05(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller,
			// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
			// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
			@InjectEventRecorder(typeMask = BUNDLE_EVENTS, synchronous = true) EventRecorder<BundleEvent> events,
			@InjectEventRecorder(typeMask = FrameworkEvent.STARTLEVEL_CHANGED) EventRecorder<FrameworkEvent> startLevelEvents
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
		try {
			// crank up the framework start-level.  This should result in a STARTED event because we are ingoring the activation policy
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a STARTING, STARTED, STOPPING, STOPPED event to be sent here because we met the start-level
			// and we are ignoring the lazy activation policy
			assertThat(events.waitForCount(4, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STARTING, tblazy2),
				event(BundleEvent.STARTED, tblazy2),
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// now mark the bundle to use the activation policy
			tblazy2.start(Bundle.START_ACTIVATION_POLICY);
			assertTrue(tblazy2StartLevel.isActivationPolicyUsed(), "bundle is using activation policy.");

			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level
			// but no STARTING or STARTED event because no trigger class was loaded.
			assertThat(events.waitForCount(3, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.LAZY_ACTIVATION, tblazy2),
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// persistently stop the bundle
			tblazy2.stop();
			// no events are expected because the bundle should already be stopped
			assertThat(events.drain()).isEmpty();
			
			// now call start(START_TRANSIENT | START_ACTIVATION_POLICY) while start-level is met.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			tblazy2.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);

			// we expect a LAZY_ACTIVATION event
			assertThat(events.waitForCount(1, TIMEOUT)).hasEventsExactly(event(BundleEvent.LAZY_ACTIVATION, tblazy2));

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// make sure the bundle is not persistently started and is not using its activation policy
			assertTrue(!tblazy2StartLevel.isPersistentlyStarted(), "bundle is not persistently started.");
			assertTrue(!tblazy2StartLevel.isActivationPolicyUsed(), "bundle is not using activation policy.");
		} finally {
			restoreStartLevel(startLevel, initialSL, initialBSL, startLevelEvents);
		}
	}

	/*
	 * Tests Bundle.start(START_TRANSIENT) in relation to the start-level service
	 */
	@Test
	public void testStartTransient01(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller,
			// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
			// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
			@InjectEventRecorder(typeMask = BUNDLE_EVENTS, synchronous = true) EventRecorder<BundleEvent> events,
			@InjectEventRecorder(typeMask = FrameworkEvent.STARTLEVEL_CHANGED) EventRecorder<FrameworkEvent> startLevelEvents
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		tblazy2.start(Bundle.START_ACTIVATION_POLICY);
		try {
			// crank up the framework start-level.  This should result in no STARTED event
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level
			assertThat(events.waitForCount(3, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.LAZY_ACTIVATION, tblazy2),
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// now call start(START_TRANSIENT) before the start-level is met.  This should result in no STARTED event
			try {
				tblazy2.start(Bundle.START_TRANSIENT);
				fail("expected a BundleException because start level is not met.");
			} catch (BundleException e) {
				// expected
			}

			assertThat(events.drain()).isEmpty();

			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a LAZY_ACTIVATION, STOPPING, STOPPED event to be sent here because we met the start-level
			// but no STARTED event because the transient start was called before the start-level was met
			assertThat(events.waitForCount(3, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.LAZY_ACTIVATION, tblazy2),
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// now call start(START_TRANSIENT) while start-level is met.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			tblazy2.start(Bundle.START_TRANSIENT);

			// we expect a LAZY_ACTIVATION event here because the start-level was met before we called Bundle.start method.
			assertThat(events.waitForCount(3, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.LAZY_ACTIVATION, tblazy2),
				event(BundleEvent.STARTING, tblazy2),
				event(BundleEvent.STARTED, tblazy2));

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));
		} finally {
			restoreStartLevel(startLevel, initialSL, initialBSL, startLevelEvents);
		}
	}

	/*
	 * Tests Bundle.stop(STOP_TRANSIENT) in relation to the start-level service
	 */
	@Test
	public void testStopTransient01(
			@InjectBundleContext BundleContext bundleContext,
			@InjectBundleInstaller BundleInstaller bundleInstaller,
			// listen for STARTING, STARTED, STOPPING, STOPPED and LAZY_ACTIVATION events
			// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
			@InjectEventRecorder(typeMask = BUNDLE_EVENTS, synchronous = true) EventRecorder<BundleEvent> events,
			@InjectEventRecorder(typeMask = FrameworkEvent.STARTLEVEL_CHANGED) EventRecorder<FrameworkEvent> startLevelEvents
			) throws Exception {
		FrameworkStartLevel startLevel = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);
		int initialSL = startLevel.getStartLevel();
		int initialBSL = startLevel.getInitialBundleStartLevel();
		startLevel.setInitialBundleStartLevel(initialSL + 10);
		Bundle tblazy2 = bundleInstaller.installBundle("activationpolicy.tblazy2.jar", false);

		try {
			// persistently start the bundle
			tblazy2.start();
			BundleStartLevel tblazy2StartLevel = tblazy2.adapt(BundleStartLevel.class);
			assertTrue(tblazy2StartLevel.isPersistentlyStarted(), "bundle is persistently started.");

			// test transient start Bundle.stop(START_TRANSIENT)
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);
		
			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a STARTING, STARTED, STOPPING, STOPPED event to be sent here because we met the start-level and we were persistently started
			// no LAZY_ACTIVATION event should be fired because this activation was not a result of a class load.
			assertThat(events.waitForCount(4, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STARTING, tblazy2),
				event(BundleEvent.STARTED, tblazy2),
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			// now call stop(STOP_TRANSIENT) while the start-level is met.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			// we expect a STARTING, STARTED event to be sent here because we met the start-level
			// no LAZY_ACTIVATION event should be fired because this activation was not a result of a class load
			assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STARTING, tblazy2),
				event(BundleEvent.STARTED, tblazy2));

			tblazy2.stop(Bundle.STOP_TRANSIENT);
			assertTrue(tblazy2StartLevel.isPersistentlyStarted(), "Bundle is persistently started.");

			// we expect a STOPPING, STOPPED event to be sent here because we met the start-level
			assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);


			// now set the start-level back up and check that the bundle is started again because it is persistently started.
			startLevel.setStartLevel(startLevel.getStartLevel() + 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STARTING, tblazy2),
				event(BundleEvent.STARTED, tblazy2));

			startLevel.setStartLevel(startLevel.getStartLevel() - 15, (FrameworkListener[]) null);
			assertStartLevelChanged(startLevelEvents, bundleContext);

			assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
				event(BundleEvent.STOPPING, tblazy2),
				event(BundleEvent.STOPPED, tblazy2));
		} finally {
			restoreStartLevel(startLevel, initialSL, initialBSL, startLevelEvents);
		}
	}

	/*
	 * Tests a simple dependency chain and checks to see if lazy activation bundles got
	 * started in the correct order.
	 */
	@Test
	public void testActivationPolicyChain01(
			@InjectInstalledBundle(value = "activationpolicy.tbchain1.jar",start =  false) Bundle tbchain1,
			@InjectInstalledBundle(value = "activationpolicy.tbchain2.jar",start =  false) Bundle tbchain2,
			@InjectInstalledBundle(value = "activationpolicy.tbchain3.jar",start =  false) Bundle tbchain3,
			@InjectInstalledBundle(value = "activationpolicy.tbchain4.jar",start =  false) Bundle tbchain4,
			@InjectInstalledBundle(value = "activationpolicy.tbchain5.jar",start =  false) Bundle tbchain5,
			@InjectEventRecorder(typeMask = BundleEvent.STARTED | BundleEvent.STOPPED) EventRecorder<BundleEvent> events
			) throws Exception {


		tbchain1.start(Bundle.START_ACTIVATION_POLICY);
		tbchain2.start(Bundle.START_ACTIVATION_POLICY);
		tbchain3.start(Bundle.START_ACTIVATION_POLICY);
		tbchain4.start(Bundle.START_ACTIVATION_POLICY);
		tbchain5.start(Bundle.START_ACTIVATION_POLICY);

		tbchain1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tbchain1.SingleChainTest")
				.getConstructor()
				.newInstance();

		assertThat(events.waitForCount(3, TIMEOUT)).hasEventsExactly(
			event(BundleEvent.STARTED, tbchain3),
			event(BundleEvent.STARTED, tbchain2),
			event(BundleEvent.STARTED, tbchain1));
	}

	/*
	 * More advanced chain test that contains multiple class hierachies
	 * NOTE there may be too much assumption on the order the VM verifier loads interface classes when more than one is implemented.
	 */
	@Test
	public void testActivationPolicyChain02(
			@InjectInstalledBundle(value = "activationpolicy.tbchain1.jar",start =  false) Bundle tbchain1,
			@InjectInstalledBundle(value = "activationpolicy.tbchain2.jar",start =  false) Bundle tbchain2,
			@InjectInstalledBundle(value = "activationpolicy.tbchain3.jar",start =  false) Bundle tbchain3,
			@InjectInstalledBundle(value = "activationpolicy.tbchain4.jar",start =  false) Bundle tbchain4,
			@InjectInstalledBundle(value = "activationpolicy.tbchain5.jar",start =  false) Bundle tbchain5,
			@InjectEventRecorder(typeMask = BundleEvent.STARTED | BundleEvent.STOPPED) EventRecorder<BundleEvent> events
			) throws Exception {


		tbchain1.start(Bundle.START_ACTIVATION_POLICY);
		tbchain2.start(Bundle.START_ACTIVATION_POLICY);
		tbchain3.start(Bundle.START_ACTIVATION_POLICY);
		tbchain4.start(Bundle.START_ACTIVATION_POLICY);
		tbchain5.start(Bundle.START_ACTIVATION_POLICY);

		tbchain1.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tbchain1.TestMultiChain")
				.getConstructor()
				.newInstance();

		assertThat(events.waitForCount(5, TIMEOUT)).hasEventsExactly(
			event(BundleEvent.STARTED, tbchain5),
			event(BundleEvent.STARTED, tbchain3),
			event(BundleEvent.STARTED, tbchain4),
			event(BundleEvent.STARTED, tbchain2),
			event(BundleEvent.STARTED, tbchain1));
	}

	/*
	 * This tests that a ClassCircularityError is not generated when the "trigger" class is loaded while a bundle
	 * is being activated as a result of a lazy activation policy
	 */
	@Test
	public void testClassCircularity(
			@InjectInstalledBundle(value = "activationpolicy.tblazy5.jar",start = false) Bundle tblazy5,
			@InjectInstalledBundle(value = "activationpolicy.tblazy6.jar",start = false) Bundle tblazy6,
			@InjectEventRecorder(typeMask = BundleEvent.STARTED) EventRecorder<BundleEvent> events
			) throws Exception {

		tblazy5.start(Bundle.START_ACTIVATION_POLICY);
		tblazy6.start(Bundle.START_ACTIVATION_POLICY);

		tblazy5.loadClass(
				"org.osgi.test.cases.framework.activationpolicy.tblazy5.CircularityErrorTest")
				.getConstructor()
				.newInstance();
		// The order of activation is reversed here because tblazy6's activator loads a class in tblazy5 that triggers it to be activated before
		// tblazy6 is started, therefore the STARTED event should be fired for tblazy5 first.
		// It is questionable whether this is required by the specification.  It may be better just to make sure both bundles are in the ACTIVE state 
		// after the classload is successful to determine that no errors occurred.
		assertThat(events.waitForCount(2, TIMEOUT)).hasEventsInAnyOrder(
			event(BundleEvent.STARTED, tblazy5),
			event(BundleEvent.STARTED, tblazy6));
	}

	/*
	 * Tests that getBundleContext works as when LAZY_ACTIVATION event is fired.
	 */
	@Test
	public void testGetBundleContext(
			@InjectBundleContext BundleContext bundleContext,
			@InjectInstalledBundle(value = "activationpolicy.tblazy2.jar",start = false) Bundle tblazy2,
			// listen for STARTED, STOPPED and LAZY_ACTIVATION evnets
			// we *should* get LAZY_ACTIVATION events because this *is* a synchronous listener.
			@InjectEventRecorder(typeMask = BundleEvent.STARTED | BundleEvent.STOPPED | BundleEvent.LAZY_ACTIVATION, synchronous = true) EventRecorder<BundleEvent> events
			) throws Exception {

		// capture the bundle context while the LAZY_ACTIVATION event is being delivered
		List<BundleContext> contexts = new CopyOnWriteArrayList<>();
		bundleContext.addBundleListener((SynchronousBundleListener) event -> {
			if (event.getType() == BundleEvent.LAZY_ACTIVATION) {
				contexts.add(event.getBundle().getBundleContext());
			}
		});
		tblazy2.start(Bundle.START_ACTIVATION_POLICY);

		tblazy2.loadClass("org.osgi.test.cases.framework.activationpolicy.tblazy2.ATest");
	
		// The bundle must have been activated now
		assertThat(events.waitForCount(2, TIMEOUT)).hasEventsExactly(
			event(BundleEvent.LAZY_ACTIVATION, tblazy2),
			event(BundleEvent.STARTED, tblazy2));
		assertEquals(1, contexts.size(), "number of contexts");
		assertEquals(tblazy2, contexts.get(0).getBundle(), "bundle context");

	}
}
