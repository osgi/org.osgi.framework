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
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.test.cases.framework.launch.junit.tck.TckProperties;

/**
 * Test cases for testing fragment bundles and extension bundles.
 *
 * @author $Id$
 */
public class ExtensionDependencyTests extends LaunchTest {

	private static final String	EXTENSION_TEST_NAMESPACE	= "extension.test";

	private static final String	V1					= ".v1";
	private static final String	V2					= ".v2";
	private static final String	V3					= ".v3";
	private static final String	JAR					= ".jar";

	private static final String	EXTENSION_1				= "dependency.tb1";
	private static final String	EXTENSION_1_V1_JAR		= "/" + EXTENSION_1 + V1
			+ JAR;
	private static final String	EXTENSION_1_V2_JAR		= "/" + EXTENSION_1 + V2
			+ JAR;
	private static final String	EXTENSION_1_V3_JAR		= "/" + EXTENSION_1 + V3
			+ JAR;

	private static final String	EXTENSION_1_CLASS_NAME	= "org.osgi.test.cases.framework.launch.dependency.tb1.TB1";
	private static final String	EXTENSION_2_RESOURCE	= "org/osgi/test/cases/framework/launch/dependency/tb1/tb1.txt";

	private static final String	EXTENSION_2				= "dependency.tb2";
	private static final String	EXTENSION_2_V1_JAR		= "/" + EXTENSION_2 + V1
			+ JAR;

	private static final String	EXTENSION_3				= "dependency.tb3";
	private static final String	EXTENSION_3_V1_JAR		= "/" + EXTENSION_3 + V1
			+ JAR;

	private static final String	EXTENSION_4				= "dependency.tb4";
	private static final String	EXTENSION_4_V1_JAR		= "/" + EXTENSION_4 + V1
			+ JAR;

	private Framework framework;
	private int					originalExtensionCount;

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		framework = createFramework(true);
		originalExtensionCount = getExtensionCount(framework);
	}

	private Framework createFramework(boolean delete) {
		Map<String, String> configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE,
				getStorageArea(getName(), delete).getAbsolutePath());
		Framework newFramework = createFramework(configuration);
		initFramework(newFramework);
		return newFramework;
	}

	@AfterEach
	public void tearDown() throws Exception {
		stopFramework(framework);
		super.tearDown();
	}

	/**
	 * Tests if an extension refresh results in a framework restart. Will only
	 * perform this test if <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals
	 * <code>true</code>.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionRefresh() throws Exception {
		if (!isFrameworkExtensionSupported()) {
			return;
		}
		Bundle extensionA_V1 = installAndResolve(framework, true,
				EXTENSION_1_V1_JAR)
				.get(0);
		int currentExtensionCount = getExtensionCount(framework);
		assertThat(currentExtensionCount)
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount + 1);

		Future<FrameworkEvent> stopFuture = waitForStop(framework);

		// uninstall and refresh the extension which causes a framework stop
		extensionA_V1.uninstall();
		framework.adapt(FrameworkWiring.class)
				.refreshBundles(Collections.singleton(extensionA_V1));

		// wait for stop
		assertSystemRefreshedEvent(stopFuture);

		reCreateFramework();

		// make sure there are no fragments attached
		currentExtensionCount = getExtensionCount(framework);
		assertThat(currentExtensionCount)
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount);
	}

	@Test
	public void testMultipleVersions() throws BundleException, IOException,
			InterruptedException, ExecutionException, ClassNotFoundException {
		if (!isFrameworkExtensionSupported()) {
			return;
		}

		Bundle extensionA_V1 = installAndResolve(framework, true,
				EXTENSION_1_V1_JAR).get(0);

		int currentExtensionCount = getExtensionCount(framework);
		assertThat(currentExtensionCount)
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount + 1);

		assertExtensionClass(EXTENSION_1_CLASS_NAME, EXTENSION_2_RESOURCE);

		List<Bundle> v2ANDv3 = installAndResolve(framework, false,
				EXTENSION_1_V2_JAR, EXTENSION_1_V3_JAR);

		String location_V3 = v2ANDv3.get(1).getLocation();

		Future<FrameworkEvent> stopFuture = waitForStop(framework);

		// now update and refresh V1; causes a framework stop
		extensionA_V1.update(getBundleInput(EXTENSION_1_V1_JAR).openStream());
		framework.adapt(FrameworkWiring.class)
				.refreshBundles(Collections.singleton(extensionA_V1));
		// wait for stop
		assertSystemRefreshedEvent(stopFuture);

		reCreateFramework();
		currentExtensionCount = getExtensionCount(framework);
		assertThat(currentExtensionCount)
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount + 1);

		Bundle extensionA_V3 = framework.getBundleContext()
				.getBundle(location_V3);
		assertWires(framework.adapt(BundleWiring.class),
				extensionA_V3.adapt(BundleWiring.class),
				HostNamespace.HOST_NAMESPACE);

		assertExtensionClass(EXTENSION_1_CLASS_NAME, EXTENSION_2_RESOURCE);
	}

	@Test
	public void testImportPackage()
			throws BundleException, IOException, InvalidSyntaxException {
		if (!isFrameworkExtensionSupported()) {
			return;
		}

		List<Bundle> extensions = installAndResolve(framework, true,
				EXTENSION_2_V1_JAR, EXTENSION_1_V1_JAR);
		BundleRevision extension1 = extensions.get(1)
				.adapt(BundleRevision.class);
		BundleRevision extension2 = extensions.get(0)
				.adapt(BundleRevision.class);

		// make sure the build included package and ee requirements for
		// extension2
		assertThat(extension2
				.getRequirements(PackageNamespace.PACKAGE_NAMESPACE))
				.as("No package requirements found.").isNotEmpty();

		// make sure both extensions are attached and the host and ee
		// requirements wired
		BundleWiring hostWiring = framework.adapt(BundleWiring.class);
		assertWires(hostWiring, extension1.getWiring(),
				HostNamespace.HOST_NAMESPACE);
		assertWires(hostWiring, extension1.getWiring(),
				ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		assertWires(hostWiring, extension2.getWiring(),
				HostNamespace.HOST_NAMESPACE);
		assertWires(hostWiring, extension2.getWiring(),
				ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);

		// make sure the package wires did not get created.
		// system bundle can only export packages.
		List<BundleWire> packageWires = hostWiring
				.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(packageWires)
				.as("Found some package wires: " + packageWires).hasSize(0);

		// ensure the service from extension 2 is registered
		ServiceReference< ? >[] serviceRef = framework.getBundleContext()
				.getServiceReferences((String) null, "(test=tb2)");
		assertThat(serviceRef)
				.as("No service found.")
				.isNotNull();
		assertThat(serviceRef.length)
				.as("Wrong number of services.").isOne();
	}

	@Test
	public void testRequireCapabilityFromExtension()
			throws BundleException, IOException {
		if (!isFrameworkExtensionSupported()) {
			return;
		}
		List<Bundle> extensions = installAndResolve(framework, true,
				EXTENSION_1_V1_JAR, EXTENSION_2_V1_JAR, EXTENSION_3_V1_JAR);
		Bundle extension3 = extensions.get(2);

		BundleWiring hostWiring = framework.adapt(BundleWiring.class);
		assertWires(hostWiring, extension3.adapt(BundleWiring.class),
				EXTENSION_TEST_NAMESPACE);
	}

	@Test
	public void testNormalBundleRequireCapabilityFromExtension()
			throws BundleException, IOException {
		if (!isFrameworkExtensionSupported()) {
			return;
		}
		List<String> bundles = Arrays.asList(new String[] {
				EXTENSION_1_V1_JAR, "/launch.tb5.jar"
		});
		List<Bundle> installed = new ArrayList<Bundle>();

		for (String bundle : bundles) {
			installed.add(installBundle(framework, bundle));
		}
		framework.adapt(FrameworkWiring.class).resolveBundles(installed);
		for (Bundle bundle : installed) {
			assertThat(bundle)
					.as("Expected bundle to resolve: " + bundle)
				.isInState(Bundle.RESOLVED);
		}

		Bundle normalBundle = installed.get(1);
		BundleWiring provider = framework.adapt(BundleWiring.class);
		BundleWiring requirer = normalBundle
				.adapt(BundleWiring.class);

		BundleWire extensionWire = provider.getProvidedWires(EXTENSION_TEST_NAMESPACE)
				.get(0);
		BundleCapability capability = provider
				.getCapabilities(EXTENSION_TEST_NAMESPACE)
				.get(0);
		BundleRequirement requirement = requirer
				.getRequirements(EXTENSION_TEST_NAMESPACE)
				.get(0);

		assertThat(extensionWire.getProvider())
				.as("Wrong provider")
				.isEqualTo(provider.getResource());
		assertThat(extensionWire.getRequirer())
				.as("Wrong requirer")
				.isEqualTo(requirer.getResource());
		assertThat(extensionWire.getProviderWiring())
				.as("Wrong provider wiring")
				.isEqualTo(provider);
		assertThat(extensionWire.getRequirerWiring())
				.as("Wrong requirer wiring")
				.isEqualTo(requirer);
		assertThat(extensionWire.getCapability())
				.as("Wrong capability")
				.isEqualTo(capability);
		assertThat(extensionWire.getRequirement())
				.as("Wrong requirement")
				.isEqualTo(requirement);
		assertThat(extensionWire.getRequirement().matches(extensionWire.getCapability()))
				.as("Requirement does not match capability")
				.isTrue();
	}

	@Test
	public void testMissingCapabilityFromExtension()
			throws BundleException, IOException, InvalidSyntaxException {
		if (!isFrameworkExtensionSupported()) {
			return;
		}
		List<Bundle> extensions = installAndResolve(framework, false,
				EXTENSION_3_V1_JAR);
		Bundle extension3 = extensions.get(0);

		assertThat(getExtensionCount(framework))
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount);

		installAndResolve(framework, true, EXTENSION_1_V1_JAR);
		assertThat(getExtensionCount(framework))
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount + 1);

		installAndResolve(framework, true, EXTENSION_2_V1_JAR);
		// request extension3 to resolve incase it was not pulled in by the
		// framework
		framework.adapt(FrameworkWiring.class)
				.resolveBundles(Collections.singleton(extension3));

		assertThat(getExtensionCount(framework))
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount + 3);
		BundleWiring hostWiring = framework.adapt(BundleWiring.class);
		assertWires(hostWiring, extension3.adapt(BundleWiring.class),
				EXTENSION_TEST_NAMESPACE);

		// ensure the service from extension 2 is registered
		ServiceReference< ? >[] serviceRef = framework.getBundleContext()
				.getServiceReferences((String) null, "(test=tb2)");
		assertThat(serviceRef)
				.as("No service found.")
				.isNotNull();
		assertThat(serviceRef.length)
				.as("Wrong number of services.").isOne();

		// ensure the service from extension 3 is registered
		serviceRef = framework.getBundleContext()
				.getServiceReferences((String) null, "(test=tb3)");
		assertThat(serviceRef)
				.as("No service found.")
				.isNotNull();
		assertThat(serviceRef.length)
				.as("Wrong number of services.").isOne();
	}

	@Test
	public void testRequireIdentity() throws BundleException, IOException {
		if (!isFrameworkExtensionSupported()) {
			return;
		}
		List<Bundle> extensions = installAndResolve(framework, true,
				EXTENSION_1_V1_JAR,
				EXTENSION_4_V1_JAR);
		assertThat(getExtensionCount(framework))
				.as("Wrong number of fragments.")
				.isEqualTo(originalExtensionCount + 2);
		Bundle extension1 = extensions.get(0);
		Bundle extension4 = extensions.get(1);

		BundleWiring fwkBundleWiring = framework.adapt(BundleWiring.class);
		BundleWiring extension1Wiring = extension1.adapt(BundleWiring.class);
		BundleWiring extension4Wiring = extension4.adapt(BundleWiring.class);
		// The osgi.identity capability is not payload.
		// Check that extension1 wiring is providing the wires.
		assertThat(fwkBundleWiring
				.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE))
				.as("Framework is providing another identity.").hasSize(1);
		assertThat(extension4Wiring
				.getRequiredWires(IdentityNamespace.IDENTITY_NAMESPACE))
				.as("Extension4 wiring is requireing another identity.").hasSize(0);
		List<BundleWire> identityRequiredWires = fwkBundleWiring
				.getRequiredWires(IdentityNamespace.IDENTITY_NAMESPACE);
		assertThat(identityRequiredWires)
				.as("Wrong number of required wires found.").hasSize(1);
		assertThat(identityRequiredWires)
				.as("Wrong wires found.")
				.isEqualTo(extension1Wiring.getProvidedResourceWires(
						IdentityNamespace.IDENTITY_NAMESPACE));
	}

	private void assertExtensionClass(String extensionAClassName,
			String extensionAResource)
			throws ClassNotFoundException, IOException {
		ClassLoader fwkClassLoader = framework.adapt(BundleWiring.class)
				.getClassLoader();
		Class< ? > extensionClass = fwkClassLoader
				.loadClass(extensionAClassName);
		assertThat(extensionClass)
				.as("Failed to find extension class: " + extensionAClassName)
				.isNotNull();

		// make sure there is only one resource found;
		// trying to make sure the other versions are not on the classpath
		List<URL> resourceList = new ArrayList<>();
		for (Enumeration<URL> resources = fwkClassLoader
				.getResources(extensionAResource); resources
						.hasMoreElements();) {
			resourceList.add(resources.nextElement());
		}
		assertThat(resourceList)
				.as("Wrong number of resources found.").hasSize(1);
	}

	private static int getExtensionCount(Framework framework) {
		return framework.adapt(BundleWiring.class)
				.getProvidedWires(HostNamespace.HOST_NAMESPACE)
				.size();
	}

	private final void reCreateFramework() {
		// close the factory to throw away the class loader
		frameworkFactory.close();
		// create a new framework with a new class loader; but don't delete the
		// storage area
		framework = createFramework(false);
	}

	private final List<Bundle> installAndResolve(Framework framework,
			boolean expectResolve, String... bundles)
			throws BundleException, IOException {
		List<Bundle> installed = new ArrayList<Bundle>();
		for (String bundle : bundles) {
			installed.add(installBundle(framework, bundle));
		}
		framework.adapt(FrameworkWiring.class).resolveBundles(installed);
		for (Bundle bundle : installed) {
			if (expectResolve) {
				assertThat(bundle)
						.as("Expected bundle to resolve: " + bundle)
				.isInState(Bundle.RESOLVED);
				assertWires(framework.adapt(BundleWiring.class),
						bundle.adapt(BundleWiring.class),
						HostNamespace.HOST_NAMESPACE);
			} else {
				assertThat(bundle)
						.as("Expected bundle to not resolve: " + bundle)
				.isInState(Bundle.INSTALLED);
			}
		}
		return installed;
	}

	private void assertWires(BundleWiring hostWiring,
			BundleWiring fragmentWiring, String namespace) {
		boolean payloadReq = !(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE
				.equals(namespace)
				|| HostNamespace.HOST_NAMESPACE.equals(namespace));
		assertThat(hostWiring)
				.as("No host wiring!")
				.isNotNull();
		assertThat(fragmentWiring)
				.as("No fragment wiring!")
				.isNotNull();
		List<BundleRequirement> fragmentReqs = fragmentWiring.getRevision()
				.getDeclaredRequirements(namespace);
		assertThat(fragmentReqs)
				.as("No requirements found by fragment.").isNotEmpty();
		List<BundleWire> providedWires = hostWiring
				.getProvidedWires(namespace);
		assertThat(providedWires)
				.as("No provided wires found.").isNotEmpty();
		List<BundleWire> requiredWires = payloadReq
				? hostWiring.getRequiredWires(namespace)
				: fragmentWiring.getRequiredWires(namespace);
		assertThat(requiredWires)
				.as("No required wires found.").isNotEmpty();
		int numFound = 0;
		for (BundleWire requiredWire : requiredWires) {
			if (fragmentReqs.contains(requiredWire.getRequirement())) {
				if (providedWires.contains(requiredWire)) {
					numFound++;
				}
			}
		}
		assertThat(numFound)
				.as("The expected wires are not found: " + providedWires
						+ requiredWires)
				.isEqualTo(fragmentReqs.size());
	}

	private static void assertSystemRefreshedEvent(
			Future<FrameworkEvent> stopFuture)
			throws InterruptedException, ExecutionException {
		FrameworkEvent stopEvent = stopFuture.get();
		assertThat(stopEvent)
				.as("No event found.")
				.isNotNull();
		assertThat(stopEvent.getType())
				.as("Wrong event: " + stopEvent)
				.isEqualTo(FrameworkEvent.STOPPED_SYSTEM_REFRESHED);
	}

	private static Future<FrameworkEvent> waitForStop(
			final Framework framework) {
		FutureTask<FrameworkEvent> stoppedFuture = new FutureTask<>(
				new Callable<FrameworkEvent>() {
					@Override
					public FrameworkEvent call() throws Exception {
						return framework.waitForStop(TckProperties.getTimeout() * TckProperties.getScaling());
					}
				});
		new Thread(stoppedFuture, "Waiting for stop.").start();
		return stoppedFuture;
	}

	private boolean isFrameworkExtensionSupported() {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			return true;
		}
		System.out.println("framework extension bundles not supported");
		return false;
	}
}
