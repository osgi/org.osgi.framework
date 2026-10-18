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
package org.osgi.test.cases.framework.launch.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import javax.net.SocketFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

public class FrameworkLaunchTests extends LaunchTest {

	private static final String PACKAGE_ADMIN = "org.osgi.service.packageadmin.PackageAdmin";
	private static final String STARTLEVEL = "org.osgi.service.startlevel.StartLevel";
	private static final String PERMISSION_ADMIN = "org.osgi.service.permissionadmin.PermissionAdmin";
	private static final String CONDPERM_ADMIN = "org.osgi.service.condpermadmin.ConditionalPermissionAdmin";

	private static class BootClassLoader extends ClassLoader {
		protected BootClassLoader() {
			super(null);
		}
	}
	private static ClassLoader bootClassLoader = new BootClassLoader();

	private void updateFramework(Framework framework) {
		int previousState = framework.getState();
		FrameworkEvent[] result = new FrameworkEvent[1];
		Exception[] failureException = new Exception[1];
		Thread waitForStop = waitForStopThread(framework, 10000, result, failureException);
		waitForStop.start();
		try {
			Thread.sleep(500);
			framework.update();
		} catch (BundleException e) {
			fail("Failed to update the framework", e); //$NON-NLS-1$
		}
		catch (InterruptedException e) {
			fail("Unexpected interruption", e);
		}
		try {
			waitForStop.join();
		} catch (InterruptedException e) {
			fail("Unexpected interruption", e); //$NON-NLS-1$
		}
		if (failureException[0] != null)
			fail("Error occurred while waiting", failureException[0]); //$NON-NLS-1$
		assertThat(result[0])
				.as("Wait for stop event is null")
				.isNotNull(); //$NON-NLS-1$

		// if the framework was not STARTING or ACTIVE then we assume the waitForStop returned immediately with a FrameworkEvent.STOPPED
		int expectedFrameworkEvent = (previousState & (Bundle.STARTING | Bundle.ACTIVE)) != 0 ? FrameworkEvent.STOPPED_UPDATE : FrameworkEvent.STOPPED;
		assertThat(result[0].getType())
				.as("Wait for stop event type is wrong")
				.isEqualTo(expectedFrameworkEvent); //$NON-NLS-1$

		// hack; not sure how to listen for when a framework is done starting back up.
		for (int i = 0; i < 20; i++) {
			if (framework.getState() != previousState) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					// nothing
				}
			} else {
				break;
			}
		}
		assertThat(framework.getState())
				.as("Not back at previous state after update")
				.isEqualTo(previousState);
	}

	private Object getService(Framework framework, String serviceClass) {
		BundleContext context = framework.getBundleContext();
		ServiceReference< ? > ref = context.getServiceReference(serviceClass);
		return ref == null ? null : context.getService(ref);
	}

	private Thread waitForStopThread(final Framework framework, final long timeout, final FrameworkEvent[] success, final Exception[] failureException) {
		return new Thread(new Runnable() {
			public void run() {
				try {
					success[0] = framework.waitForStop(10000);
				} catch (InterruptedException e) {
					failureException[0] = e;
				}
			}
		}, "test waitForStop thread"); //$NON-NLS-1$

	}

	private void checkImporter(BundleCapability ep, Bundle testBundle) {
		if (ep == null)
			return;
		BundleWiring wiring = ep.getRevision().getWiring();
		assertThat(wiring)
				.as("exported package is not wired")
				.isNotNull();
		List<BundleWire> wires = wiring
				.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
		for (BundleWire wire : wires) {
			if (ep.equals(wire.getCapability())) {
				if (testBundle.equals(wire.getRequirerWiring().getBundle())) {
					return;
				}
			}
		}
		fail("Bundle is not an importer of the package: " + ep);
	}

	@Test
	public void testBasicCreate() {
		createFramework(getConfiguration(getName()));
	}

	@Test
	public void testInitStop() {
		Framework framework = createFramework(getConfiguration(getName()));
        initFramework(framework);
		stopFramework(framework);
	}

	@Test
	public void testStartStop() {
		Framework framework = createFramework(getConfiguration(getName()));
		startFramework(framework);
		stopFramework(framework);
	}

	@Test
	public void testRestarts() {
		Framework framework = createFramework(getConfiguration(getName()));
		// first stop without init/start
		stopFramework(framework);
		// first start/stop
		startFramework(framework);
		stopFramework(framework);

		// now init/stop
		initFramework(framework);
		stopFramework(framework);

		// now init/start/stop
		initFramework(framework);
		startFramework(framework);
		stopFramework(framework);
	}

	@Test
	public void testUpdate() {
		Framework framework = createFramework(getConfiguration(getName()));

		// update before first init (in INSTALLED state)
		updateFramework(framework);

		// update after init (in STARTING state)
		initFramework(framework);
		updateFramework(framework);
		stopFramework(framework);

		// update after start (in ACTIVE state)
		startFramework(framework);
		updateFramework(framework);
		stopFramework(framework);

		// update after stop (in RESOLVED state)
		updateFramework(framework);
	}

	@Test
	public void testStartLevel() {
		Map<String, String> configuration = getConfiguration(getName());
		configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "25");
		Framework framework = createFramework(configuration);
		initFramework(framework);
		FrameworkStartLevel fsl = framework.getBundleContext().getBundle()
				.adapt(FrameworkStartLevel.class);
		assertThat(fsl).isNotNull();
		startFramework(framework);
		assertThat(fsl.getStartLevel())
				.as("Wrong start level after start")
				.isEqualTo(25);
		stopFramework(framework);
	}

	@Test
	public void testSystemServices() {
		Framework framework = createFramework(getConfiguration(getName()));
		startFramework(framework);
		boolean hasPackageAdmin = getService(framework, PACKAGE_ADMIN) != null;
		boolean hasStartLevel = getService(framework, STARTLEVEL) != null;
		boolean hasPermissionAdmin = getService(framework, PERMISSION_ADMIN) != null;
		boolean hasCondPermAdmin = getService(framework, CONDPERM_ADMIN) != null;
		stopFramework(framework);
		if (!(hasPackageAdmin | hasStartLevel | hasPermissionAdmin | hasCondPermAdmin))
			return; // nothing to test; no system services available

		// check that the available system services are available when framework is initialized
		initFramework(framework);
		if (hasPackageAdmin)
			assertThat(getService(framework, PACKAGE_ADMIN))
					.as("PackageAdmin")
					.isNotNull();
		if (hasStartLevel)
			assertThat(getService(framework, STARTLEVEL))
					.as("StartLevel")
					.isNotNull();
		if (hasPermissionAdmin)
			assertThat(getService(framework, PERMISSION_ADMIN))
					.as("PermissionAdmin")
					.isNotNull();
		if (hasCondPermAdmin)
			assertThat(getService(framework, CONDPERM_ADMIN))
					.as("ConditionalPermissionAdmin")
					.isNotNull();
		stopFramework(framework);
	}

	@Test
	public void testAdaptations() {
		Framework framework = createFramework(getConfiguration(getName()));
		startFramework(framework);
		Bundle systemBundle = framework.getBundleContext().getBundle();
		assertThat(systemBundle.adapt(FrameworkStartLevel.class)).isNotNull();
		assertThat(systemBundle.adapt(FrameworkWiring.class)).isNotNull();
		stopFramework(framework);
		// check that the available adaptations are available when framework is
		// initialized
		initFramework(framework);
		systemBundle = framework.getBundleContext().getBundle();
		assertThat(systemBundle.adapt(FrameworkStartLevel.class)).isNotNull();
		assertThat(systemBundle.adapt(FrameworkWiring.class)).isNotNull();
		stopFramework(framework);
	}

	@Test
	public void testStorageArea() throws BundleException, IOException {
		String testBundleLocation = "/launch.tb4.jar";
		// install a bundle into a framework
		Framework framework = createFramework(getConfiguration(getName()));
		initFramework(framework);
		Bundle testBundle = installBundle(framework, testBundleLocation);
		long id = testBundle.getBundleId();
		testBundle.start();
		stopFramework(framework);

		// create another framework with same storage area; make sure bundle is still installed and active
		framework = createFramework(getConfiguration(getName(), false));
		initFramework(framework);
		testBundle = framework.getBundleContext().getBundle(id);
		assertThat(testBundle)
				.as("Missing installed bundle")
				.isNotNull();
		assertThat(testBundle.getLocation())
				.as("Wrong bundle")
				.isEqualTo(testBundleLocation);
		startFramework(framework);
		assertThat(testBundle)
				.as("Wrong state for test bundle.").isInState(Bundle.ACTIVE);
		stopFramework(framework);

		// create another framework with same storage area using clean ONFIRSTINIT; make sure bundle is not there
		Map<String, String> configuration = getConfiguration(getName(), false);
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		framework = createFramework(configuration);
		initFramework(framework);
		Bundle[] bundles = framework.getBundleContext().getBundles();
		assertThat(bundles)
				.as("No bundles")
				.isNotNull();
		assertThat(bundles.length)
				.as("Wrong number of bundles").isOne();
		testBundle = installBundle(framework, testBundleLocation);
		id = testBundle.getBundleId();
		testBundle.start();
		stopFramework(framework);

		// test that second init does not clean
		initFramework(framework);
		testBundle = framework.getBundleContext().getBundle(id);
		assertThat(testBundle)
				.as("Missing installed bundle")
				.isNotNull();
		assertThat(testBundle.getLocation())
				.as("Wrong bundle")
				.isEqualTo(testBundleLocation);
		startFramework(framework);
		assertThat(testBundle)
				.as("Wrong state for test bundle.").isInState(Bundle.ACTIVE);

		// update the framework and make sure the bundle is still installed and active
		updateFramework(framework);
		testBundle = framework.getBundleContext().getBundle(id);
		assertThat(testBundle)
				.as("Missing installed bundle")
				.isNotNull();
		assertThat(testBundle.getLocation())
				.as("Wrong bundle")
				.isEqualTo(testBundleLocation);
		assertThat(testBundle)
				.as("Wrong state for test bundle.").isInState(Bundle.ACTIVE);
		stopFramework(framework);
	}

	@Test
	public void testSystemPackagesExtra() throws BundleException, IOException {
		String pkg1 = "org.osgi.tests.pkg1";
		String pkg2 = "org.osgi.tests.pkg2";
		Map<String, String> configuration = getConfiguration(getName());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, pkg1 + ',' + pkg2);
		Framework framework = createFramework(configuration);

		startFramework(framework);
		BundleCapability ep1 = exportedPackage(framework.getBundleContext(), pkg1);
		BundleCapability ep2 = exportedPackage(framework.getBundleContext(), pkg2);

		assertThat(ep1)
				.as("pkg1 is null")
				.isNotNull();
		assertThat(ep1.getRevision().getBundle()
				.getBundleId())
				.as("Wrong Exporter").isZero();
		assertThat(ep2)
				.as("pkg2 is null")
				.isNotNull();
		assertThat(ep2.getRevision().getBundle()
				.getBundleId())
				.as("Wrong Exporter").isZero();
		Bundle testBundle = installBundle(framework, "/launch.tb1.jar");
		try {
			testBundle.start();
		} catch (BundleException e) {
			fail("Unexpected exception", e);
		}
		checkImporter(ep1, testBundle);
		checkImporter(ep2, testBundle);
		stopFramework(framework);
	}

	@Test
	public void testLibraryExtensions() throws BundleException, IOException {
		Map<String, String> configuration = getConfiguration(getName());
		configuration.put(Constants.FRAMEWORK_LIBRARY_EXTENSIONS, "1,test");
		configuration.put("nativecodetest", "1");
		Framework framework = createFramework(configuration);
		startFramework(framework);
		Bundle testBundle = installBundle(framework, "/launch.tb2.jar");
		testBundle.start();
		stopFramework(framework);

		configuration = getConfiguration(getName());
		configuration.put(Constants.FRAMEWORK_LIBRARY_EXTENSIONS, "2,test");
		configuration.put("nativecodetest", "2");
		framework = createFramework(configuration);
		startFramework(framework);
		testBundle = installBundle(framework, "/launch.tb2.jar");
		testBundle.start();
		stopFramework(framework);

		configuration = getConfiguration(getName());
		configuration.put(Constants.FRAMEWORK_LIBRARY_EXTENSIONS, "3,test");
		configuration.put("nativecodetest", "3");
		framework = createFramework(configuration);
		startFramework(framework);
		testBundle = installBundle(framework, "/launch.tb2.jar");
		testBundle.start();
		stopFramework(framework);
	}

	@Test
	public void testWaitForStop() {
		Framework framework = createFramework(getConfiguration(getName()));
		startFramework(framework);
		FrameworkEvent event = null;
		try {
            event = framework.waitForStop(1000);
		}
		catch (InterruptedException e) {
			fail("Unexpected interuption", e);
		}
		assertThat(event)
				.as("Wait for stop event is null")
				.isNotNull();
		assertThat(event
				.getType())
				.as("Wrong event type")
				.isEqualTo(FrameworkEvent.WAIT_TIMEDOUT);
		stopFramework(framework);
	}

	@Test
	public void testBootDelegation() throws BundleException, IOException {
		Class< ? > vmClass = null;
		try {
			vmClass = bootClassLoader.loadClass("javax.security.auth.x500.X500Principal");
		} catch (ClassNotFoundException e) {
			fail("Unexpected CNFE", e);
		}
		Map<String, String> configuration = getConfiguration(getName());
		doTestBootDelegation(vmClass, configuration, "javax.security.auth.x500", true);
		doTestBootDelegation(vmClass, configuration, "javax.security.auth.*", true);
		doTestBootDelegation(vmClass, configuration, "javax.security.*", true);
		doTestBootDelegation(vmClass, configuration, "*", true);
		doTestBootDelegation(vmClass, configuration, null, false);
		doTestBootDelegation(vmClass, configuration, "junk.*", false);
	}

	private void doTestBootDelegation(Class< ? > vmClass,
			Map<String, String> configuration, String bootDelegation,
			boolean fromBoot) throws BundleException, IOException {
		if (bootDelegation != null)
			configuration.put(Constants.FRAMEWORK_BOOTDELEGATION, bootDelegation);
		else
			configuration.remove(Constants.FRAMEWORK_BOOTDELEGATION);
		Framework framework = createFramework(configuration);
		startFramework(framework);
		Bundle testBundle = installBundle(framework, "/launch.tb3.jar");
		Class< ? > testClass = null;
		try {
			 testClass = testBundle.loadClass("javax.security.auth.x500.X500Principal");
		} catch (ClassNotFoundException e) {
			fail("Unexpected CNFE", e);
		}
		if (fromBoot)
			assertThat(testClass)
					.as("Unexpected class")
					.isEqualTo(vmClass);
		else
			assertThat(vmClass.equals(testClass))
					.as("Unexpected class")
					.isFalse();
		stopFramework(framework);
	}

	@Test
	public void testParentClassLoader() throws BundleException, IOException {
		Map<String, String> configuration = getConfiguration(getName());
		configuration.put(Constants.FRAMEWORK_BOOTDELEGATION, "*");
		doTestParentClassLoader(configuration, null, Bundle.class.getName(), true);
		doTestParentClassLoader(configuration, null,
				SocketFactory.class.getName(), false);
		doTestParentClassLoader(configuration, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT, Bundle.class.getName(), true);
		doTestParentClassLoader(configuration,
				Constants.FRAMEWORK_BUNDLE_PARENT_BOOT,
				SocketFactory.class.getName(), false);
		doTestParentClassLoader(configuration, Constants.FRAMEWORK_BUNDLE_PARENT_EXT, Bundle.class.getName(), true);
		doTestParentClassLoader(configuration,
				Constants.FRAMEWORK_BUNDLE_PARENT_EXT,
				SocketFactory.class.getName(), false);
		doTestParentClassLoader(configuration,
				Constants.FRAMEWORK_BUNDLE_PARENT_APP,
				SocketFactory.class.getName(), false);
		doTestParentClassLoader(configuration,
				Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK,
				SocketFactory.class.getName(), false);
	}

	private void doTestParentClassLoader(Map<String, String> configuration,
			String parentType, String className, boolean fail)
			throws BundleException, IOException {
		if (parentType != null)
			configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, parentType);
		else
			configuration.remove(Constants.FRAMEWORK_BUNDLE_PARENT);
		Framework framework = createFramework(configuration);
		startFramework(framework);
		Bundle testBundle = installBundle(framework, "/launch.tb3.jar");
		try {
			 testBundle.loadClass(className);
			 if (fail)
				 fail("Should not be able to load class: " + className);
		} catch (ClassNotFoundException e) {
			if (!fail)
				fail("Unexpected CNFE", e);
		}
		stopFramework(framework);
	}

	@Test
	public void testExecPermission() throws BundleException, IOException {
		File testOutputFile = new File(rootStorageArea, getName() + File.separator + "execPermissions.out");
		Map<String, String> configuration = getConfiguration(getName());
		String osName = System.getProperty("os.name");
		if (osName.toLowerCase().indexOf("windows") >= 0)
			configuration.put(Constants.FRAMEWORK_EXECPERMISSION, "cmd.exe /c copy ${abspath} " + testOutputFile.getAbsolutePath());
		else
			configuration.put(Constants.FRAMEWORK_EXECPERMISSION, "cp ${abspath} " + testOutputFile.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_LIBRARY_EXTENSIONS, "1,test");
		configuration.put("nativecodetest", "1");
		Framework framework = createFramework(configuration);
		startFramework(framework);
		Bundle testBundle = installBundle(framework, "/launch.tb2.jar");
		testBundle.start();
		stopFramework(framework);
		assertThat(testOutputFile)
				.as("File does not exist: " + testOutputFile.getAbsolutePath()).exists();
	}

	@Test
	public void testMultipBSNVersion() throws IOException, BundleException{
		// explicitly set bsnversion to multiple
		Map<String, String> configurationMulti = getConfiguration(getName()
				+ ".multiple");
		configurationMulti.put(Constants.FRAMEWORK_BSNVERSION, Constants.FRAMEWORK_BSNVERSION_MULTIPLE);
		Framework frameworkMultiBSN = createFramework(configurationMulti);
		startFramework(frameworkMultiBSN);

		installBundle(frameworkMultiBSN, "/launch.tb4.jar", "launch.tb4.a");
		installBundle(frameworkMultiBSN, "/launch.tb4.jar", "launch.tb4.b");
		stopFramework(frameworkMultiBSN);

		// don't set to anything; test default is single
		Map<String, String> configurationSingle1 = getConfiguration(getName()
				+ ".single1");
		Framework frameworkSingleBSN1 = createFramework(configurationSingle1);
		startFramework(frameworkSingleBSN1);
		installBundle(frameworkSingleBSN1, "/launch.tb4.jar", "launch.tb4.a");
		try {
			installBundle(frameworkSingleBSN1, "/launch.tb4.jar", "launch.tb4.b");
			fail("Should fail installing duplicate bundle.");
		} catch (BundleException e) {
			assertThat(e.getType())
					.as("Wrong exception type.")
					.isEqualTo(BundleException.DUPLICATE_BUNDLE_ERROR);
			// expected
		}
		stopFramework(frameworkSingleBSN1);

		// explicitly set bsnversion to single
		Map<String, String> configurationSingle2 = getConfiguration(getName()
				+ ".single2");
		configurationSingle2.put(Constants.FRAMEWORK_BSNVERSION, Constants.FRAMEWORK_BSNVERSION_SINGLE);
		Framework frameworkSingleBSN2 = createFramework(configurationSingle2);
		startFramework(frameworkSingleBSN2);
		installBundle(frameworkSingleBSN2, "/launch.tb4.jar", "launch.tb4.a");
		try {
			installBundle(frameworkSingleBSN2, "/launch.tb4.jar", "launch.tb4.b");
			fail("Should fail installing duplicate bundle.");
		} catch (BundleException e) {
			assertThat(e.getType())
					.as("Wrong exception type.")
					.isEqualTo(BundleException.DUPLICATE_BUNDLE_ERROR);
			// expected
		}
		stopFramework(frameworkSingleBSN2);
	}

	@Test
	public void testUUID() {
		// Test UUID values
		Map<String, String> config1 = getConfiguration(getName() + ".1");
		Framework framework1 = createFramework(config1);
		// get the UUID after first init
		initFramework(framework1);
		String uuid = framework1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid);
		stopFramework(framework1);
		// Keep a set of previously used uuids
		Set<String> uuids = new HashSet<String>();
		uuids.add(uuid);
		// Now try to re-init and start the framework and each init/shutdown cycle gives a unique uuid
		for (int i = 0; i < 20; i++) {
			initFramework(framework1);
			String uuid1 = framework1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
			verifyUUID(uuid1);
			assertThat(uuids.contains(uuid1))
					.as("Duplicate UUID")
					.isFalse();
			uuids.add(uuid1);
			startFramework(framework1);
			// after start the uuid should be the same as after init
			String uuid2 = framework1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
			verifyUUID(uuid2);
			assertThat(uuid2)
					.as("UUID changed after start")
					.isEqualTo(uuid1);
			stopFramework(framework1);
		}
	}

	private void verifyUUID(String uuid) {
		assertThat(uuid)
				.as("Null uuid.")
				.isNotNull();
		StringTokenizer st = new StringTokenizer(uuid, "-");
		String[] uuidSections = new String[5];
		// All UUIDs must have 5 sections
		for (int i = 0; i < uuidSections.length; i++) {
			try {
				uuidSections[i] = "0x" + st.nextToken();
			} catch (NoSuchElementException e) {
				fail("Wrong number of uuid sections: " + uuid, e);
			}
		}
		// make sure there is not an extra section.
		assertThatExceptionOfType(NoSuchElementException.class)
				.as("Too many sections in uuid: " + uuid)
				.isThrownBy(() -> st.nextToken());
		// now verify each section of the UUID can be decoded as a hex string and is the correct size
		for (int i = 0; i < uuidSections.length; i++) {
			int limit = 0;
			switch (i) {
				case 0: {
					limit = 10; // "0x" + 4*<hexOctet> == 10 len
					break;
				}
				case 1:
				case 2:
				case 3:{
					limit = 6; // "0x" + 2*<hexOctet> == 6 len
					break;
				}
				case 4:{
					limit = 14; // "0x" + 6*<hexOctet> == 14 len
					break;
				}
				default:
					break;
			}
			assertThat(uuidSections[i].length() <= limit)
					.as("UUISection is too big: " + uuidSections[i])
					.isTrue();
			try {
				Long.decode(uuidSections[i]);
			} catch (NumberFormatException e) {
				fail("Invalid section: " + uuidSections[i], e);
			}
		}
	}
	/**
	 * The highest-versioned capability exporting the package in the
	 * framework, or {@code null} if nothing exports it.
	 */
	private static BundleCapability exportedPackage(BundleContext context, String packageName) {
		BundleCapability result = null;
		Version resultVersion = null;
		for (Bundle bundle : context.getBundles()) {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring == null) {
				continue; // not resolved
			}
			for (BundleCapability capability : wiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE)) {
				if (!packageName.equals(capability.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE))) {
					continue;
				}
				Version version = (Version) capability.getAttributes().get(Constants.VERSION_ATTRIBUTE);
				if (version == null) {
					version = Version.emptyVersion;
				}
				if (result == null || version.compareTo(resultVersion) > 0) {
					result = capability;
					resultVersion = version;
				}
			}
		}
		return result;
	}
}
