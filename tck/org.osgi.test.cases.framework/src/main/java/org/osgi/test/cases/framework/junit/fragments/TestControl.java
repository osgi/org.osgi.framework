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
package org.osgi.test.cases.framework.junit.fragments;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;
import org.osgi.test.common.wiring.FrameworkWirings;

/**
 * Test cases for testing fragment bundles and extension bundles.
 *
 * @author $Id$
 */
public class TestControl implements
		BundleListener, FrameworkListener {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	private Hashtable<String,Bundle>	events		= new Hashtable<>();
	private static final String	SEPARATOR	= "#";
	private ServiceRegistration<Bundle>	bundleReg;

	@BeforeEach
	public void setUp() throws Exception {
		// injected bundles are uninstalled after @AfterEach: refresh before the next test
		FrameworkWirings.refreshBundles(bundleContext);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("bundle", "fragments.tests");
		bundleReg = bundleContext.registerService(Bundle.class,
				bundleContext.getBundle(), props);
	}

	@AfterEach
	public void tearDown() throws Exception {
		bundleReg.unregister();
	}

	/**
	 * Fragment bundles must not specify a Bundle Activator. If the Bundle.start
	 * method is called on a Bundle object for a fragment, then the framework
	 * must throw a BundleException indicating that a fragment bundle cannot be
	 * started. If the Bundle.stop method is called on a Bundle object for a
	 * fragment, then the framework must throw a BundleException indicating that
	 * a fragment bundle cannot be stopped.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.start()
	 */
	@Test
	public void testFragmentLifecycle(
			@InjectInstalledBundle(value = "fragments.tb1b.jar", start = false) Bundle tb1b
			) throws Exception {
		// Install fragment bundle
		assertThatExceptionOfType(BundleException.class)
				.as("Framework should throw BundleException for start().")
				.isThrownBy(() -> tb1b.start());
		assertThatExceptionOfType(BundleException.class)
				.as("Framework should throw BundleException for stop().")
				.isThrownBy(() -> tb1b.stop());
	}

	/**
	 * Tests that a bundle fragment supplies classpath entries to host.
	 *
	 * Tests that resources and classes are loaded by their host bundle's class
	 * loader.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.loadClass(String)
	 */
	@Test
	public void testAppendClasspath01() throws Exception {
		Class< ? > classObj1;
		Class< ? > classObj2;

		// Install fragment bundle
		Bundle tb1b = bundleInstaller.installBundle("fragments.tb1b.jar", false);
		// Install host
		Bundle tb1a = bundleInstaller.installBundle("fragments.tb1a.jar", false);
		tb1a.start();
		try {
			// Load a resource that exists in a jar in the classpath of the
			// fragment bundle
			classObj1 = null;
			try {
				classObj1 = tb1a
						.loadClass("org.osgi.test.cases.framework.fragments.tb1c.TestClass");
			}
			catch (ClassNotFoundException e) {
				fail("The class org.osgi.test.cases.framework.fragments.tb1c.TestClass should be appended to the classpath of host.");
			}

			// Load a class resource that exists in the host
			classObj2 = tb1a
					.loadClass("org.osgi.test.cases.framework.fragments.tb1a.TestClass");

			// Test that class loaders are the same
			assertThat(classObj1.getClassLoader())
					.as("Class loader must be the same.")
					.isEqualTo(classObj2
					.getClassLoader());
		}
		finally {
			tb1a.stop();
			tb1a.uninstall();
			tb1b.uninstall();
		}
	}

	/**
	 * Tests that if a classpath entry cannot be located in the bundle, then the
	 * Framework attempts to locate the classpath entry in each attached
	 * fragment bundle. The attached fragment bundles are searched in ascending
	 * bundle id order.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.getResource(String)
	 */
	@Test
	public void testAppendClasspath02(
			@InjectInstalledBundle(value = "fragments.tb1g.jar", start = false) Bundle tb1g,
			@InjectInstalledBundle(value = "fragments.tb1b.jar", start = false) Bundle tb1b,
			@InjectInstalledBundle(value = "fragments.tb1a.jar", start = true) Bundle tb1a
			) throws Exception {
		// Install fragment bundles
		// Install and start host bundle

		try {
			// Check bundle id order is as expected
			assertThat(tb1g.getBundleId() < tb1b.getBundleId())
					.as("Expecting bundle id for fragment bundle installed first to be smaller.")
					.isTrue();

			// Find resource
			URL url = tb1a.getResource("resources/notinhost.txt");
			assertThat(url)
					.as("Expecting a URL that is not null because it exists.")
					.isNotNull();

			// Check content within resource
			InputStream ins = url.openStream();
			try {
				assertThat(ins)
						.as("Expecting to be able to open stream to resource.")
						.isNotNull();

				BufferedReader bufr = new BufferedReader(new InputStreamReader(
						ins));
				String line = bufr.readLine();
				assertThat(line)
						.as("Expecting framework to recover resources in ascending bundle id order.")
						.isEqualTo("tb1g");
			}
			finally {
				ins.close();
			}
		}
		finally {
			// Stop and uninstall
		}
	}

	/**
	 * The Framework must not allow a fragment to replace any class or resource
	 * of a host bundle.
	 *
	 * Tests that URL.getPath method for a URL that uses the bundle resource or
	 * bundle entry protocol returns an absolute path (a path that starts with
	 * '/'). For example, the URL returned from
	 * Bundle.getEntry("myimages/test.gif") must have a path of
	 * "/myimages/test.gif".
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.getResource(String)
	 */
	@Test
	public void testExistingResourceNotReplaced() throws Exception {
		URL url;
		InputStream ins;
		Bundle tb1a;
		BufferedReader bufr;
		String line;

		// Install fragment bundle
		Bundle tb1b = bundleInstaller.installBundle("fragments.tb1b.jar", false);
		// Install and start host bundle
		tb1a = bundleInstaller.installBundle("fragments.tb1a.jar", false);
		tb1a.start();

		try {
			// Find resource
			url = tb1a.getResource("resources/resource.txt");
			assertThat(url)
					.as("Expecting a not null URL.")
					.isNotNull();

			// Test that getPath returns absolute path
			assertThat(url.getPath())
					.as("Absolute path expected.")
					.isEqualTo("/resources/resource.txt");

			// Check content within resource
			ins = url.openStream();
			try {
				assertThat(ins)
						.as("Expecting to open stream to resource")
						.isNotNull();

				bufr = new BufferedReader(new InputStreamReader(ins));
				line = bufr.readLine();
				assertThat(line)
						.as("Framework must not allow a fragment to replace any class or resource of a host bundle")
						.isEqualTo("tb1a");
			}
			finally {
				ins.close();
			}
		}
		finally {
			// Stop and uninstall
			tb1a.stop();
			tb1a.uninstall();
			tb1b.uninstall();
		}
	}

	/**
	 * Tests that a fragment cannot be a host to another fragment.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFragmentCannotBeHost(
			@InjectInstalledBundle(value = "fragments.tb1d.jar", start = false) Bundle tb1d,
			@InjectInstalledBundle(value = "fragments.tb1b.jar", start = false) Bundle tb1b,
			@InjectInstalledBundle(value = "fragments.tb1a.jar", start = true) Bundle tb1a
			) throws Exception {
		// Install fragment bundles
		// Install host

		// Test that state of fragment tb1d is INSTALLED
		assertThat(tb1d)
				.as("Expecting the state of fragment bundle tb1d to be INSTALLED").isInState(Bundle.INSTALLED);
		// Test that state of fragment tb1b is RESOLVED
		assertThat(tb1b)
				.as("Expecting the state of fragment bundle tb1b to be RESOLVED").isInState(Bundle.RESOLVED);
	}

	/**
	 * Tests that a fragment can attach to multiple hosts.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFragmentMultipleHosts(
			@InjectInstalledBundle(value = "fragments.tb3a.jar", start = false) Bundle tb3a,
			@InjectInstalledBundle(value = "fragments.tb3c.jar", start = false) Bundle tb3c,
			@InjectInstalledBundle(value = "fragments.tb3d.jar", start = false) Bundle tb3d
			) throws Exception {
		// Install and start host bundle version 1.0

		// Install and start host bundle version 2.0

		// Install fragment bundle

		// Try recovering resource from host that is in classpath of fragment
		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb3d));
		tb3a.start();
		tb3c.start();
		Class< ? > a = tb3a
				.loadClass("org.osgi.test.cases.framework.fragments.tb3d.SomeClass");
		Class< ? > c = tb3c
				.loadClass("org.osgi.test.cases.framework.fragments.tb3d.SomeClass");
		assertThat(c.getName())
				.as("class from fragment does not have the same name")
				.isEqualTo(a
				.getName());
		assertThat(a.equals(c))
				.as("class from fragment not loaded by different class loaders")
				.isFalse();
	}

	/**
	 * Tests the fragment-attachment directive recognized by the framework for
	 * Bundle-SymbolicName taking the value of "never" which indicates that no
	 * fragments are allowed to attach to the host bundle at any time.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFragmentAttachmentDirective01() throws Exception {
		// Install and start host bundle
		Bundle tb3e = bundleInstaller.installBundle("fragments.tb3e.jar", false);
		tb3e.start();

		// Install fragment bundle
		Bundle tb3d = bundleInstaller.installBundle("fragments.tb3d.jar", false);
		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb3d));

		// Try recovering resource from host that is in classpath of fragment
		try {
			tb3e
					.loadClass("org.osgi.test.cases.framework.fragments.tb3d.SomeClass");
			fail("The class should not be in the classpath of host bundle tb3e with fragment-attachment:=\"none\"");
		}
		catch (ClassNotFoundException e) {
		}
		finally {
			tb3e.stop();
			tb3e.uninstall();
			tb3d.uninstall();
		}
	}

	/**
	 * Tests the fragment-attachment directive recognized by the framework for
	 * Bundle-SymbolicName taking the value of "resolve-time" which indicates
	 * that fragments are allowed to attach to the host bundle only during the
	 * process of resolving the host bundle.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFragmentAttachmentDirective02() throws Exception {
		// Install and start host bundle
		Bundle tb3f = bundleInstaller.installBundle("fragments.tb3f.jar", false);
		tb3f.start();

		// Install fragment bundle, host already resolved
		Bundle tb3d = bundleInstaller.installBundle("fragments.tb3d.jar", false);
		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb3d));

		// Try recovering resource from host that is in classpath of fragment
		try {
			tb3f
					.loadClass("org.osgi.test.cases.framework.fragments.tb3d.SomeClass");
			fail("The class should not be in the classpath of host bundle tb3f with fragment-attachment:=\"resolve-time\"");
		}
		catch (ClassNotFoundException e) {
		}
		finally {
			tb3f.stop();
			tb3f.uninstall();
			tb3d.uninstall();
		}
	}

	/**
	 * Tests the fragment-attachment directive recognized by the framework for
	 * Bundle-SymbolicName taking the value of "resolve-time" which indicates
	 * that fragments are allowed to attach to the host bundle only during the
	 * process of resolving the host bundle.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFragmentAttachmentDirective03() throws Exception {
		// Install fragment bundle, host not yet resolved
		Bundle tb3d = bundleInstaller.installBundle("fragments.tb3d.jar", false);

		// Install and start host bundle
		Bundle tb3f = bundleInstaller.installBundle("fragments.tb3f.jar", false);
		tb3f.start();

		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb3d));

		// Try recovering resource from host that is in classpath of fragment
		try {
			tb3f
					.loadClass("org.osgi.test.cases.framework.fragments.tb3d.SomeClass");
		}
		catch (ClassNotFoundException e) {
			fail("The class should be in the classpath of host bundle tb3f with fragment-attachment:=\"resolve-time\"");
		}
		finally {
			tb3f.stop();
			tb3f.uninstall();
			tb3d.uninstall();
		}
	}

	/**
	 * Tests that attaching a fragment bundle to an already resolved host bundle
	 * is not possible when the fragment's Import-Package entries add additional
	 * packages to the host.
	 *
	 * Tests that if an error occurs during the attachment of a fragment bundle
	 * then the fragment bundle is not attached to the host. A fragment bundle
	 * must enter the resolved state only if it has been successfully attached
	 * to one or more host bundles.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testAttachToResolvedHost01() throws Exception {
		// Install and start host bundle
		Bundle tb1a = bundleInstaller.installBundle("fragments.tb1a.jar", false);
		tb1a.start();

		// Install fragment bundle, host is already ACTIVE
		Bundle tb1e = bundleInstaller.installBundle("fragments.tb1e.jar", false);
		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb1e));

		// Verify that fragment bundle is still in INSTALLED state
		try {
			assertThat(tb1e)
					.as("Expecting fragment bundle to be in INSTALLED state.").isInState(Bundle.INSTALLED);
		}
		finally {
			tb1a.stop();
			tb1a.uninstall();
			tb1e.uninstall();
		}
	}

	/**
	 * Tests that attaching a fragment bundle to an already resolved host bundle
	 * is not possible when the fragment's Require-Bundle entries add additional
	 * required bundles to the host.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testAttachToResolvedHost02() throws Exception {
		// Install and start host bundle
		Bundle tb1a = bundleInstaller.installBundle("fragments.tb1a.jar", false);
		tb1a.start();

		// Install required bundle
		Bundle tb1b = bundleInstaller.installBundle("fragments.tb1b.jar", false);

		// Install fragment bundle, host is already ACTIVE
		Bundle tb1f = bundleInstaller.installBundle("fragments.tb1f.jar", false);
		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb1f));

		// Verify that fragment bundle is still in INSTALLED state
		try {
			assertThat(tb1f)
					.as("Expecting fragment bundle to be in INSTALLED state.").isInState(Bundle.INSTALLED);
		}
		finally {
			tb1a.stop();
			tb1a.uninstall();
			tb1b.uninstall();
			tb1f.uninstall();
		}
	}

	/**
	 * Tests that when an attached fragment bundle is updated, the content of
	 * the previous fragment remains attached to the host bundle. The new
	 * content of the updated fragment must not be allowed to attach to the host
	 * bundle until the Framework is restarted or the host bundle is refreshed.
	 *
	 * When a set of bundles are refreshed using the Wiring API then each bundle
	 * in the set must have an UNRESOLVED BundleEvent published.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFragmentUpdate() throws Exception {
		// Add BundleEvent listener
		purgeEvents();
		bundleContext.addBundleListener(this);

		// Install the host bundle
		Bundle tb1a = bundleInstaller.installBundle("fragments.tb1a.jar", false);

		// Install fragment bundle
		Bundle tb1g = bundleInstaller.installBundle("fragments.tb1g.jar", false);

		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb1g));
		// Start the host bundle
		tb1a.start();

		try {
			// Verify that fragment bundle is in RESOLVED state
			assertThat(tb1g)
					.as("Expecting fragment bundle to be in RESOLVED state.").isInState(Bundle.RESOLVED);

			// Update fragment bundle
			InputStream in = bundleContext.getBundle().getResource(
					"fragments.tb1h.jar").openStream();
			tb1g.update(in);
			in.close();

			// Test that class resource is accessible
			try {
				tb1a
						.loadClass("org.osgi.test.cases.framework.fragments.tb1g.TestClass");
			}
			catch (ClassNotFoundException e) {
				fail("The class should exist in the host.");
			}

			// Refresh the host bundle
			FrameworkWirings.refreshBundles(bundleContext, new Bundle[] {tb1a});

			// Verify if UNRESOLVED event was published for each bundle
			Thread.sleep(2000); // wait a while
			if (!hasEventOccurred(tb1a, BundleEvent.class,
					BundleEvent.UNRESOLVED)
					&& !hasEventOccurred(tb1g, BundleEvent.class,
							BundleEvent.UNRESOLVED)) {
				fail("Expecting BundleEvent of type UNRESOLVED to be published");
			}

			// Test that class resource is not accessible
			assertThatExceptionOfType(ClassNotFoundException.class).as("The class should not exist in the host.")
					.isThrownBy(() -> { tb1a .loadClass("org.osgi.test.cases.framework.fragments.tb1g.TestClass"); });
		}
		finally {
			tb1a.stop();
			tb1a.uninstall();
			tb1g.uninstall();
			bundleContext.removeBundleListener(this);
			purgeEvents();
		}

	}

	/**
	 * Tests that a fragment Import-Package entry conflicts with a host
	 * Import-Package entry if it has the same package name and any of its
	 * directives or matching attributes are different. If a conflict is found,
	 * the fragment is not attached to the host bundle.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testConflictingImportPackage(
			@InjectInstalledBundle(value = "fragments.tb7c.jar", start = true) Bundle tb7c,
			@InjectInstalledBundle(value = "fragments.tb7d.jar", start = true) Bundle tb7d,
			@InjectInstalledBundle(value = "fragments.tb7b.jar", start = false) Bundle tb7b,
			@InjectInstalledBundle(value = "fragments.tb7a.jar", start = false) Bundle tb7a
			) throws Exception {
		// Install and start bundles that export packages

		// Install fragment bundle

		// Install the host bundle

		// Start the host bundle
		tb7a.start();

		// Verify that fragment bundle is still in INSTALLED state
		assertThat(tb7b)
				.as("Expecting fragment bundle to be in INSTALLED state.").isInState(Bundle.INSTALLED);
	}

	/**
	 * Tests a fragment Export-Package entry with a different version than a
	 * host Export-Package entry with the same package name. Test that it
	 * attaches normally and the export with a different version from the
	 * fragment is available for import.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testConflictingExportPackage(
			@InjectInstalledBundle(value = "fragments.tb7e.jar", start = false) Bundle tb7e,
			@InjectInstalledBundle(value = "fragments.tb7c.jar", start = false) Bundle tb7c,
			@InjectInstalledBundle(value = "fragments.tb7h.jar", start = false) Bundle tb7h
			) throws Exception {
		// Install fragment bundle

		// Install the host bundle

		// Start the host bundle
		tb7c.start();

		// Install the bundle that uses the package exported by the fragment

		// Verify that fragment bundle is in RESOLVED state
		assertThat(tb7e)
				.as("Expecting fragment bundle to be in RESOLVED state.").isInState(Bundle.RESOLVED);

		// Try starting the bundle that uses the package exported by the
		// fragment
		try {
			tb7h.start();
		}
		catch (BundleException e) {
			fail("Expecting the bundle to start.  The imported package from the fragment should resolve.");
		}
	}

	/**
	 * Tests that a fragment Require-Bundle entry conflicts with a host
	 * Require-Bundle entry only if it has the same bundle symbolic name but a
	 * different version range. If a conflict is found, the fragment is not
	 * attached to the host bundle.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testConflictingRequireBundle(
			@InjectInstalledBundle(value = "fragments.tb8b.jar", start = false) Bundle tb8bv1,
			@InjectInstalledBundle(value = "fragments.tb8c.jar", start = false) Bundle tb8bv2,
			@InjectInstalledBundle(value = "fragments.tb8d.jar", start = false) Bundle tb8d,
			@InjectInstalledBundle(value = "fragments.tb8a.jar", start = false) Bundle tb8a
			) throws Exception {
		// Install required bundles

		// Install the fragment bundle

		// Install the host bundle

		// Start the host bundle
		tb8a.start();

		// Verify that fragment bundle is in INSTALLED state
		assertThat(tb8d)
				.as("Expecting fragment bundle to be in INSTALLED state.").isInState(Bundle.INSTALLED);
	}

	/**
	 * Test that when a fragment bundle becomes unresolved the Framework
	 * detaches it from the host and reresolve the host bundle and reattaches
	 * the remaining attached fragments.
	 *
	 * Tests that when the bundle is resolved by the framework, the framework
	 * publishes a Bundle Event of type RESOLVED. A bundle may become unresolved
	 * at some future time. This could occur as a result of uninstalling the
	 * bundle, refreshing its package or some other reason. When a bundle
	 * becomes unresolved, the framework must publish a Bundle Event of type
	 * UNRESOLVED.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testFragmentBundleDetach() throws Exception {
		InputStream ins;
		BufferedReader bufr;
		String line;

		// Add BundleListener
		purgeEvents();
		bundleContext.addBundleListener(this);

		// Install fragment bundles
		Bundle tb1b = bundleInstaller.installBundle("fragments.tb1b.jar", false);
		Bundle tb1g = bundleInstaller.installBundle("fragments.tb1g.jar", false);

		// Install the host bundle
		Bundle tb1a = bundleInstaller.installBundle("fragments.tb1a.jar", false);

		// Start the host bundle
		tb1a.start();

		try {
			// Verify bundle events were published by the framework
			Thread.sleep(2000); // wait a while
			assertThat(hasEventOccurred(tb1b, BundleEvent.class,
							BundleEvent.RESOLVED))
					.as("Expecting BundleEvent of type RESOLVED.")
					.isTrue();
			assertThat(hasEventOccurred(tb1g, BundleEvent.class,
							BundleEvent.RESOLVED))
					.as("Expecting BundleEvent of type RESOLVED.")
					.isTrue();
			assertThat(hasEventOccurred(tb1a, BundleEvent.class,
							BundleEvent.RESOLVED))
					.as("Expecting BundleEvent of type RESOLVED.")
					.isTrue();

			// Verify that fragment bundles are in RESOLVED state
			assertThat(tb1b)
					.as("Expecting fragment bundle to be in RESOLVED state.").isInState(Bundle.RESOLVED);
			assertThat(tb1g)
					.as("Expecting fragment bundle to be in RESOLVED state.").isInState(Bundle.RESOLVED);

			// Verify resource from tb1b is accessible
			ins = tb1a.getResource("resources/notinhost.txt").openStream();
			bufr = new BufferedReader(new InputStreamReader(ins));
			line = bufr.readLine();
			assertThat(line)
					.as("Expecting resource from tb1b to be accessible.")
					.isEqualTo("tb1b");
			bufr.close();

			// Detach / uninstall fragment bundle tb1b
			tb1b.uninstall();

			// Verify bundle event was published by the framework
			Thread.sleep(2000); // wait a while
			assertThat(hasEventOccurred(tb1b, BundleEvent.class,
							BundleEvent.UNRESOLVED))
					.as("Expecting BundleEvent of type UNRESOLVED.")
					.isTrue();

			// Verify that tb1b is in UNINSTALLED state
			assertThat(tb1b)
					.as("Expecting fragment bundle to be in UNINSTALLED state.").isInState(Bundle.UNINSTALLED);

			// Refresh host bundle
			FrameworkWirings.refreshBundles(bundleContext, tb1a);
			Thread.sleep(2000); // wait a while

			// Verify resource from tb1b is not accessible
			ins = tb1a.getResource("resources/notinhost.txt").openStream();
			bufr = new BufferedReader(new InputStreamReader(ins));
			line = bufr.readLine();
			assertThat(line)
					.as("Expecting resource from tb1b to be inaccessible.")
					.isEqualTo("tb1g");
			bufr.close();
		}
		finally {
			tb1a.stop();
			tb1a.uninstall();
			tb1g.uninstall();
			bundleContext.removeBundleListener(this);
			purgeEvents();
		}
	}

	/**
	 * @param event
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {
		addEvent(event.getBundle(), BundleEvent.class, event.getType());
	}

	/**
	 * @param event
	 * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
	 */
	public void frameworkEvent(FrameworkEvent event) {
		addEvent(event.getBundle(), FrameworkEvent.class, event.getType());
	}

	/**
	 * Add an event to the table of events occurred since the last call to
	 * purgeEvents.
	 *
	 * @param bundle Bundle whose event has been captured
	 * @param eventClass Class of the event object
	 * @param eventType Event type published
	 */
	public void addEvent(Bundle bundle, Class< ? > eventClass, int eventType) {
		System.out.println("Captured " + eventClass.getName() + " for bundle "
				+ bundle.getSymbolicName() + " of type " + eventType);
		String key = bundle.getSymbolicName() + SEPARATOR
				+ eventClass.getName() + SEPARATOR + eventType;
		events.put(key, bundle);
	}

	/**
	 * Purge all events in the events table.
	 */
	public void purgeEvents() {
		events.clear();
	}

	/**
	 * Verify if the event has occurred (exists in the events table).
	 *
	 * @param bundle Bundle whose event has been captured
	 * @param eventClass Class of the event object
	 * @param eventType Event type published
	 * @return
	 */
	public boolean hasEventOccurred(Bundle bundle, Class< ? > eventClass,
			int eventType) {
		boolean retVal = false;
		String key = bundle.getSymbolicName() + SEPARATOR
				+ eventClass.getName() + SEPARATOR + eventType;
		retVal = events.containsKey(key);
		return retVal;
	}

	/**
	 * If a bundle is updated from a host to a fragment, after update, the
	 * bundle must not be active.
	 */
	@Test
	public void testHostBecomesFragment(
			@InjectInstalledBundle(value = "fragments.tb20a.jar", start = false) Bundle tb20a
			) throws Exception {
		// Install host bundle
		// Try starting host
		tb20a.start();
		assertThat(tb20a)
				.as("tb20a failed to start for start().")
				.isInState(Bundle.ACTIVE);
		// Update to a fragment bundle
		InputStream tb20b = bundleContext.getBundle()
				.getResource("fragments.tb20b.jar").openStream();
		tb20a.update(tb20b);
		// make sure not active
		assertThat(tb20a)
				.as("tb20a updated to fragment started!")
				.isNotInState(Bundle.ACTIVE);
	}
}
