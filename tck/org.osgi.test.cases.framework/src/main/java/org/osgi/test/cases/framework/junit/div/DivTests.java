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
package org.osgi.test.cases.framework.junit.div;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;
import org.osgi.test.cases.framework.div.tb6.BundleClass;
import org.osgi.test.cases.framework.junit.tck.TckProperties;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.event.EventRecorder;
import org.osgi.test.common.event.EventRecorders;
import org.osgi.test.common.event.TimedEvent;
import org.osgi.test.common.install.BundleInstaller;

/**
 * This is the bundle initially installed and started by the TestCase when
 * started. It performs the various tests and reports back to the TestCase.
 *
 * @author Ericsson Radio Systems AB
 */
public class DivTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;
	public static final String	basePath	= "/org/osgi/test/cases/framework/div/";
	public static final String	basePkg		= "org.osgi.test.cases.framework.div.";

	public static final Collection<Map<String, Object>> MATCH_NATIVE_ATTRIBUTES;
	static {
		Collection<Map<String, Object>> temp = new ArrayList<Map<String, Object>>();
		Map<String, Object> commonAttrs = new HashMap<String, Object>();
		commonAttrs.put("org.osgi.test.cases.framework.div.tb12", "abc");
		commonAttrs.put("org.osgi.test.cases.framework.div.tb15", "abc");
		commonAttrs.put("org.osgi.test.cases.framework.div.tb16", "xyz");
		commonAttrs.put(NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE, "en");
		commonAttrs.put(NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE,
				new Version(1, 0, 0));

		Map<String, Object> linuxX86 = new HashMap<String, Object>(
				commonAttrs);
		linuxX86.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "linux");
		linuxX86.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "X86");
		temp.add(Collections.unmodifiableMap(linuxX86));

		Map<String, Object> linuxX86_64 = new HashMap<String, Object>(
				commonAttrs);
		linuxX86_64.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "linux");
		linuxX86_64.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE,
				"X86-64");
		temp.add(Collections.unmodifiableMap(linuxX86_64));

		Map<String, Object> qnxX86 = new HashMap<String, Object>(commonAttrs);
		qnxX86.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "qnx");
		qnxX86.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "X86");
		temp.add(Collections.unmodifiableMap(qnxX86));

		Map<String, Object> qnxPPC = new HashMap<String, Object>(commonAttrs);
		qnxPPC.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "qnx");
		qnxPPC.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "powerPC");
		temp.add(Collections.unmodifiableMap(qnxPPC));

		Map<String, Object> qnxARMLE = new HashMap<String, Object>(
				commonAttrs);
		qnxARMLE.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "qnx");
		qnxARMLE.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "ARM_le");
		temp.add(Collections.unmodifiableMap(qnxARMLE));

		Map<String, Object> winX86 = new HashMap<String, Object>(commonAttrs);
		winX86.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "win32");
		winX86.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "X86");
		temp.add(Collections.unmodifiableMap(winX86));

		Map<String, Object> solarisSparc = new HashMap<String, Object>(
				commonAttrs);
		solarisSparc
				.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "solaris");
		solarisSparc.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE,
				"Sparc");
		temp.add(Collections.unmodifiableMap(solarisSparc));

		Map<String, Object> sunOSSparc = new HashMap<String, Object>(
				commonAttrs);
		sunOSSparc.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "sunOS");
		sunOSSparc.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "Sparc");
		temp.add(Collections.unmodifiableMap(sunOSSparc));

		Map<String, Object> winCE = new HashMap<String, Object>(commonAttrs);
		winCE.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "windows CE");
		winCE.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "ARM_le");
		temp.add(Collections.unmodifiableMap(winCE));

		Map<String, Object> macX86 = new HashMap<String, Object>(commonAttrs);
		macX86.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "Mac OS X");
		macX86.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "X86");
		temp.add(Collections.unmodifiableMap(macX86));

		Map<String, Object> macX86_64 = new HashMap<String, Object>(
				commonAttrs);
		macX86_64.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "Mac OS X");
		macX86_64.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "X86-64");
		temp.add(Collections.unmodifiableMap(macX86_64));

		Map<String, Object> macPPC = new HashMap<String, Object>(commonAttrs);
		macPPC.put(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, "Mac OS X");
		macPPC.put(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, "PPC");
		temp.add(Collections.unmodifiableMap(macPPC));

		MATCH_NATIVE_ATTRIBUTES = Collections.unmodifiableCollection(temp);
	}

	/**
	 * Logs the manifest headers.
	 */
	@Test
	public void testManifestHeaders(
			@InjectInstalledBundle(value = "div.tb1.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
			Dictionary<String,String> h = tb.getHeaders("");
			assertThat(h.get("5-")).isEqualTo("numeric first char");
			assertThat(h
					.get("bundle-activator")).isEqualTo(basePkg + "tb1.CheckManifest");
			assertThat(h
					.get("bundle-category")).isEqualTo("should contain the bundle category");
			assertThat(h.get("bundle-classpath")).isEqualTo("., foo/bar/dummy.jar");
			assertThat(h.get("bundle-contactaddress")).isEqualTo("info@ericsson.com");
			assertThat(h
					.get("bundle-copyright")).isEqualTo("should contain the bundle copyright");
			assertThat(h
					.get("bundle-description")).isEqualTo("Contains the manifest checked by the test case.");
			assertThat(h.get("bundle-docurl")).isEqualTo("http://www.ericsson.com");
			assertThat(h.get("bundle-name")).isEqualTo(basePkg + "tb1");
			assertThat(h.get("bundle-updatelocation")).isEqualTo("www.ericsson.se");
			assertThat(h.get("bundle-vendor")).isEqualTo("Ericsson Radio Systems AB");
			assertThat(h
					.get("bundle-version")).isEqualTo("Improper value for bundle manifest version 2");
			assertThat(h.get("continue")).isEqualTo("12                          34");
			assertThat(h.get("export-package")).isEqualTo("org.osgi.dummy1;                         version=\"0.0\", org.osgi.dummy2,org.osgi.dummy3;version=\"19.67.34\"");
			assertThat(h.get("export-service")).isEqualTo("should contain the exported services, not used by framework");
			assertThat(h.get("fakeheader")).isEqualTo("This bundle is defined by developer and should be ignored by framework");
			assertThat(h.get("import-service")).isEqualTo("should contain the imported services, not used by framework");
			assertThat(h.get("manifest-version")).isEqualTo("1.0");
			assertThat(h.get("max-length")).isEqualTo("xxxxxxxxx xxxxxxxxx xxxxxxxxx xxxxxxxxx xxxxxxxxx xxxxxxEND");
			assertThat(h.get("unicode-test")).isEqualTo("\u00d0\u00de");

			tb.stop();
		}
		catch (BundleException e) {
			fail("Exception in manifest headers", e);
		}
	}

	/**
	 * Tests empty manifest headers.
	 */
	@Test
	public void testMissingManifestHeaders(
			@InjectInstalledBundle(value = "div.tb5.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
			Dictionary<String,String> h = tb.getHeaders();
			assertThat(h.size()).isOne();
			assertThat(h.get("manifest-version")).isEqualTo("1.0");
			tb.stop();
		}
		catch (BundleException e) {
			fail("Exception in testing missing manifest headers", e);
		}
	}

	/**
	 * Tests extended classpath
	 */
	@Test
	public void testBundleClassPath() {
		// instantiate an object from tbcinner.jar
		BundleClass dummy = null;
		try {
			dummy = new BundleClass();
		}
		catch (Throwable e) {
			fail("We didn't manage to instansiate a class from the extended bundle class path", e);
		}
		assertThat(this.getClass().getClassLoader())
				.as("different class loader")
				.isEqualTo(dummy.getClass()
				.getClassLoader());
	}

	/**
	 * Tests that location remains the same after an update
	 */
	@Test
	public void testBundleLocation() throws Exception {
		// update() re-reads the location, so install by URL rather than by stream
		Bundle tb = bundleContext.installBundle(bundleContext.getBundle().getEntry("div.tb5.jar").toExternalForm());
		String originalLocation = tb.getLocation();
		long originalLastModified = tb.getLastModified();
		Thread.sleep(250);
		tb.update();
		assertThat(tb.getLocation())
				.as("bundle location changed after update.")
				.isEqualTo(originalLocation);
		assertThat(originalLastModified == tb.getLastModified())
				.as("bundle last modified did not change")
				.isFalse();
	}

	private void assertNativeNamespace(Bundle b, boolean isOptional,
			boolean isResolved) {
		BundleRevision revision = b.adapt(BundleRevision.class);
		assertThat(revision)
				.as("Bundle has no revision")
				.isNotNull();
		List<BundleRequirement> nativeRequirements = revision
				.getDeclaredRequirements(NativeNamespace.NATIVE_NAMESPACE);
		assertThat(nativeRequirements)
				.as("Unexpected number of native requirements").hasSize(1);
		BundleRequirement nativeRequirement = nativeRequirements.get(0);

		// check the optional directive
		String optionalDirective = nativeRequirement.getDirectives().get(
				Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
		if (optionalDirective == null) {
			optionalDirective = Namespace.RESOLUTION_MANDATORY;
		}
		assertThat(optionalDirective)
				.as("Wrong resolution type")
				.isEqualTo(isOptional ? Namespace.RESOLUTION_OPTIONAL
						: Namespace.RESOLUTION_MANDATORY);
		// check for a valid filter directive
		String filterDirective = nativeRequirement.getDirectives().get(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		assertThat(filterDirective)
				.as("Null filter directive: " + nativeRequirement)
				.isNotNull();
		Filter filter = null;
		try {
			filter = bundleContext.createFilter(filterDirective);
		} catch (InvalidSyntaxException e) {
			fail("Bad filter: " + nativeRequirement, e);
		}

		// check that the filter matches as expected
		for (Map<String, ?> matchingMap : MATCH_NATIVE_ATTRIBUTES) {
			if (isResolved) {
				assertThat(filter.matches(matchingMap))
						.as("filter does not match map: " + filter + ".matches("
						+ matchingMap + ")")
						.isTrue();
			} else {
				assertThat(filter.matches(matchingMap))
						.as("filter does match map: " + filter + ".matches("
						+ matchingMap + ")")
						.isFalse();
			}
		}

		if (isResolved) {
			BundleWiring wiring = b.adapt(BundleWiring.class);
			assertThat(wiring)
					.as("Bundle has no wiring: " + b)
					.isNotNull();
			if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				List<BundleWire> hostWires = wiring
						.getRequiredWires(HostNamespace.HOST_NAMESPACE);
				assertThat(hostWires)
						.as("Wrong number of hosts.").hasSize(1);
				wiring = hostWires.get(0).getProviderWiring();
			}
			List<BundleWire> nativeWires = wiring
					.getRequiredWires(NativeNamespace.NATIVE_NAMESPACE);
			assertThat(nativeWires)
					.as("Unexpected number of native wires: " + b).hasSize(isOptional ? 0 : 1);
			BundleWire nativeWire = nativeWires.get(0);
			assertThat(nativeWire.getRequirement())
					.as("Found wrong requirement in native wire.")
					.isEqualTo(nativeRequirement);

			// make sure we are wired to a capability from the system bundle
			assertThat(nativeWire.getProvider().getBundle().getBundleId())
					.as("Native provider is not the system bundle").isZero();

			// this is a bit of a strange test, but it ensures the
			// capability that is wired to has the arbitrary matching
			// attributes configured into the framework
			Map<String, ?> wiredCapAttrs = nativeWire.getCapability()
					.getAttributes();
			assertThat(filter.matches(wiredCapAttrs))
					.as("filter does not match the wired to capability attrs: "
					+ filter + ".matches(" + wiredCapAttrs + ")")
					.isTrue();
		}
	}

	/**
	 * Tests basic native code invocation.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCode(
			@InjectInstalledBundle(value = "div.tb2.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
			assertNativeNamespace(tb, false, true);
		}
		catch (BundleException be) {
			fail("Native code not installed. " + reportProcessorOS(), be);
		}
	}


	/**
	 * Tests to add a FrameworkListener.
	 */
	@Test
	public void testFrameworkListener() throws Exception {
		Bundle tb = bundleInstaller.installBundle("div.tb3.jar", false);
		List<TimedEvent<FrameworkEvent>> result;
		try (EventRecorder<FrameworkEvent> recorder = EventRecorders
				.frameworkEvents(bundleContext, FrameworkEvent.ERROR)) {
			tb.start();
			tb.uninstall();
			result = recorder.collect(1, Duration.ofMillis(
					10000 * TckProperties.getScaling()));
		}
		assertThat(result)
				.as("No FrameworkEvent received").hasSize(1);
		FrameworkEvent fe = result.get(0).event();
		assertThat(fe.getBundle())
				.as("No FrameworkEvent received")
				.isEqualTo(tb);
		Throwable t = fe.getThrowable();
		assertThat(t)
				.isInstanceOf(BundleException.class);
	}

	/**
	 * Tests the file system.
	 */
	@Test
	public void testFileAccess() throws Exception {
		File file = bundleContext.getDataFile("testfile");
		if (file == null) {
			System.out.println("Framework lacks filesystem support, no error.");
			return;
		}
		PrintWriter out = new PrintWriter(new FileWriter(file));
		out.println("Line 1");
		out.println("Line 2");
		out.println("Line 3");
		out.close();
		BufferedReader in = new BufferedReader(new FileReader(file));
		assertThat(in.readLine()).isEqualTo("Line 1");
		assertThat(in.readLine()).isEqualTo("Line 2");
		assertThat(in.readLine()).isEqualTo("Line 3");
		in.close();
		assertThat(file.delete()).isTrue();
		assertThatExceptionOfType(FileNotFoundException.class)
				.as("File was not gone, Error!")
				.isThrownBy(() -> new BufferedReader(new FileReader(file)));
	}

	@Test
	public void testBundleZero() {
		// Bundle(0).update is tested in permission/tc5
		try {
			bundleContext.getBundle(0).start();
		}
		catch (BundleException e) {
			fail("bundle(0).start threw Exception", e);
		}
		assertThatExceptionOfType(BundleException.class)
				.as("bundle(0).uninstall returned without Exception")
				.isThrownBy(() -> bundleContext.getBundle(0).uninstall());
	}

	@Test
	public void testEERequirement() throws Exception {
		@SuppressWarnings("deprecation")
		final String ee = bundleContext
				.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
		System.out.println("EE: " + ee);
		Bundle tb = bundleInstaller.installBundle("div.tb7a.jar", false);
		try {
			tb.start();
		}
		catch (BundleException e) {
			fail("Required Execution Environment is available", e);
		}
		finally {
			tb.uninstall();
		}

		tb = bundleInstaller.installBundle("div.tb7b.jar", false);
		try {
			tb.start();
			fail("Required Execution Environment is not available");
		}
		catch (BundleException e) {
            // expecting exception, but bundle should not have resolved
            assertThat(tb)
            		.as("Bundle should not be resolved!").isInState(Bundle.INSTALLED);
		}
		finally {
			tb.uninstall();
		}

	}

	/**
	 * Tests native code selection filter. The bundle should be loaded even if
	 * no native code clause matches the selection filter, since there's an
	 * optional clause present (*).
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeFilterOptional(
			@InjectInstalledBundle(value = "div.tb12.jar", start = false) Bundle tb
			) throws Exception {
		tb.start();
		assertNativeNamespace(tb, true, false);
	}

	/**
	 * Tests native code selection filter. The bundle should NOT be loaded if no
	 * native code clause matches the selection filter, since there's no
	 * optional clause present (*).
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeFilterNoOptional(
			@InjectInstalledBundle(value = "div.tb15.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
			fail("Bundle should not start!");
		}
		catch (BundleException be) {
            // expecting exception, but bundle should not have resolved
            assertThat(tb)
            		.as("Bundle should not be resolved!").isInState(Bundle.INSTALLED);
			assertNativeNamespace(tb, false, false);
		}
	}

	/**
	 * Tests native code selection filter. The bundle should only be loaded if
	 * at least one native code clause matches the selection filter, since
	 * there's no optional clause present (*). This test also checks if the new
	 * osname alias (win32) matches properly (OSGi R4).
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeFilterAlias(
			@InjectInstalledBundle(value = "div.tb16.jar", start = false) Bundle tb
			) throws Exception {
		tb.start();
		assertNativeNamespace(tb, false, true);
	}

	/**
	 * Tests native code from a fragment bundle. The native code should be
	 * loaded from a fragment bundle of the host bundle.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeFragment(
			@InjectInstalledBundle(value = "div.tb18.jar", start = false) Bundle tbFragment,
			@InjectInstalledBundle(value = "div.tb17.jar", start = false) Bundle tb
			) throws Exception {
		tb.start();
		assertNativeNamespace(tbFragment, false, true);
	}

	/**
	 * Tests native code language filter. The bundle should NOT be loaded if no
	 * native code clause matches the os language, since there's no optional
	 * clause present (*).
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeLanguage(
			@InjectInstalledBundle(value = "div.tb19.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
			fail("Error: Bundle should NOT be loaded: language should not match");
		}
		catch (BundleException be) {
            // expecting exception, but bundle should not have resolved
            assertThat(tb)
            		.as("Bundle should not be resolved!").isInState(Bundle.INSTALLED);
			assertNativeNamespace(tb, false, false);
		}
	}

	/**
	 * Tests native code language filter. The bundle should be loaded since all
	 * valid languages are included in the filter.
	 *
	 * @see http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt for valid
	 *      language codes.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeLanguageSuccess(
			@InjectInstalledBundle(value = "div.tb20.jar", start = false) Bundle tb
			) throws Exception {
		tb.start();
		assertNativeNamespace(tb, false, true);
	}

	/**
	 * Tests native code os version. The bundle should NOT be loaded if no
	 * native code clause matches the os version range, since there's no
	 * optional clause present (*).
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeVersion(
			@InjectInstalledBundle(value = "div.tb21.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
			fail("Error: Bundle should NOT be loaded: os version out of range");
		}
		catch (BundleException be) {
            // expecting exception, but bundle should not have resolved
            assertThat(tb)
            		.as("Bundle should not be resolved!").isInState(Bundle.INSTALLED);
			assertNativeNamespace(tb, false, false);
		}
	}

	/**
	 * Tests successful native code os version. The bundle should be loaded
	 * since the version range should contain all valid os versions.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.start()
	 * @osgi.spec Bundle.uninstall()
	 */
	@Test
	public void testNativeCodeVersionSuccess(
			@InjectInstalledBundle(value = "div.tb22.jar", start = false) Bundle tb
			) throws Exception {
		tb.start();
		assertNativeNamespace(tb, false, true);
	}

	/**
	 * Tests that a bundle can require the osgi.native namespace directly in a
	 * Require-Capability header.
	 * 
	 * @throws BundleException
	 */
	@Test
	public void testNativeCodeNamespaceRequirement(
			@InjectInstalledBundle(value = "div.tb26.jar", start = false) Bundle tb
			) throws BundleException {
		try {
			tb.start();
			assertNativeNamespace(tb, false, true);
		} finally {
			tb.uninstall();
		}
	}

	@Test
	public void testNativeCodeNamespaceCapability() throws BundleException {
		// the install must fail; a bundle installed by mistake is removed by osgi-test
		BundleException e = catchThrowableOfType(
				() -> bundleInstaller.installBundle("div.tb27.jar", false),
				BundleException.class);
		assertThat(e)
				.as("Expected to fail installation of bundle that declares a native capability.")
				.isNotNull();
		assertThat(e.getType())
				.as("Wrong error code.")
				.isEqualTo(BundleException.MANIFEST_ERROR);
	}

	@Test
	public void testExecutionEnvironmentNamespaceCapability()
			throws BundleException {
		// the install must fail; a bundle installed by mistake is removed by osgi-test
		BundleException e = catchThrowableOfType(
				() -> bundleInstaller.installBundle("div.tb28.jar", false),
				BundleException.class);
		assertThat(e)
				.as("Expected to fail installation of bundle that declares an execution environment capability.")
				.isNotNull();
		assertThat(e.getType())
				.as("Wrong error code.")
				.isEqualTo(BundleException.MANIFEST_ERROR);
	}

	@Test
	public void testBundleEventConstants() {
		assertConstant(Integer.valueOf(0x00000001), "INSTALLED", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000002), "STARTED", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000004), "STOPPED", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000008), "UPDATED", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000010), "UNINSTALLED",
				BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000020), "RESOLVED", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000040), "UNRESOLVED", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000080), "STARTING", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000100), "STOPPING", BundleEvent.class);
		assertConstant(Integer.valueOf(0x00000200), "LAZY_ACTIVATION",
				BundleEvent.class);
	}

	@Test
	public void testFrameworkEventConstants() {
		assertConstant(Integer.valueOf(0x00000001), "STARTED", FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000002), "ERROR", FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000004), "PACKAGES_REFRESHED",
				FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000008), "STARTLEVEL_CHANGED",
				FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000010), "WARNING", FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000020), "INFO", FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000040), "STOPPED", FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000080), "STOPPED_UPDATE",
				FrameworkEvent.class);
		assertConstant(Integer.valueOf(0x00000200), "WAIT_TIMEDOUT",
				FrameworkEvent.class);
	}

	@Test
	public void testBundleGetEntry(
			@InjectInstalledBundle(value = "div.tb10.jar", start = false) Bundle bundle
			) throws Exception {
		try {

			URL url = bundle.getEntry(basePath + "tb10/Foo.class");
			assertThat(url)
					.as("Testing the method invocation with an existing entry")
					.isNotNull();

			url = bundle.getEntry(basePath + "tb10/Nonexistent");
			assertThat(url)
					.as("Testing the method invocation with an nonexistent entries")
					.isNull();

			bundle.uninstall();

			try {
				bundle.getEntry(basePath + "tb10/Nonexistent");
				fail("Testing the method invocation with an uninstalled bundle");
			}
			catch (IllegalStateException ex) {
				// This is an expected exception and can be ignored
				bundle = null;
			}
		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBundleGetEntryPaths() throws Exception {
		String[] expectedEntryPaths = {
				"org/osgi/test/cases/framework/div/tb10/Activator.class",
				"org/osgi/test/cases/framework/div/tb10/Bar.class",
				"org/osgi/test/cases/framework/div/tb10/Foo.class",
				"org/osgi/test/cases/framework/div/tb10/TestService.class",
				"org/osgi/test/cases/framework/div/tb10/TestServiceImpl.class"};

		Bundle bundle = bundleInstaller.installBundle("div.tb10.jar", false);
		try {
			Enumeration<String> enumeration = bundle
					.getEntryPaths(basePath + "tb10");
			assertThat(enumeration)
					.as("Check if some resource is returned")
					.isNotNull();

			int count = 0;
			while (enumeration.hasMoreElements()) {
				String entryPath = enumeration.nextElement();

				for (int i = 0; i < expectedEntryPaths.length; i++) {
					if (entryPath.equals(expectedEntryPaths[i])) {
						count++;
					}
				}
			}

			assertThat(count)
					.as("Checking the returned entries")
					.isEqualTo(expectedEntryPaths.length);

			enumeration = bundle.getEntryPaths(basePath + "tb10/nonexistent");
			assertThat(enumeration)
					.as("Testing the method invocation with nonexistent entries")
					.isNull();

			bundle.uninstall();

			try {
				bundle.getEntryPaths(basePath + "tb10/incorrect");
				fail("Testing the method invocation with an uninstalled bundle");
			}
			catch (IllegalStateException ex) {
				// Ignore this exception
				bundle = null;
			}

		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBundleGetResource(
			@InjectInstalledBundle(value = "div.tb10.jar", start = false) Bundle bundle,
			@InjectInstalledBundle(value = "div.tb13.jar", start = false) Bundle fragment
			) throws Exception {
		try {
			URL url = bundle.getResource(basePath + "tb10/Foo.class");
			assertThat(url)
					.as("Testing the method invocation with an existing resource (using a absolute path)")
					.isNotNull();
			url = bundle.getResource(basePath + "tb10/Nonexistent");
			assertThat(url)
					.as("Testing the method invocation with a nonexistent resource")
					.isNull();

			url = fragment.getResource(basePath + "tb13/Foo.class");
			assertThat(url)
					.as("A fragment bundle cannot return a resource using the method getResource()")
					.isNull();

			bundle.uninstall();

			try {
				bundle
						.getResource("/org/osgi/test/cases/framework/div/tb10/Foo.class");
				fail("Testing  the method invocation after uninstall the bundle");
			}
			catch (IllegalStateException ex) {
				// This is an expected exception and can be ignored
				bundle = null;
			}

		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
			if (fragment != null) {
				fragment.uninstall();
			}
		}
	}

	@Test
	public void testBundleGetResourcesResolved() {
		doTestBundleGetResources(true);
	}

	@Test
	public void testBundleGetResourcesUnresolved() {
		doTestBundleGetResources(false);
	}

	private void doTestBundleGetResources(boolean resolved) {
		Bundle tb25 = null;
		try {
			tb25 = bundleInstaller.installBundle(resolved ? "div.tb25.resolved.jar" : "div.tb25.unresolved.jar", false);
		} catch (Exception e) {
			fail("Unexpected error installing test bundle.", e);
		}
		try {
			// sanity check for the root resources
			URL rootEntry = tb25.getEntry("resources/root.txt");
			assertThat(rootEntry)
					.as("root.txt not found")
					.isNotNull();
			assertThat(getValue(rootEntry))
					.as("Wrong resource")
					.isEqualTo("root.txt");
			rootEntry = tb25.getEntry("resources/all.txt");
			assertThat(getValue(rootEntry))
					.as("Wrong resource")
					.isEqualTo("root.all.txt");
			assertThat(rootEntry)
					.as("root.all.txt not found")
					.isNotNull();
			// Bundle-ClassPath of div.tb25 does not specify '.'
			// the root resources must not be found.
			URL resource = tb25.getResource("resources/root.txt");
			assertThat(resource)
					.as("Found unexpected resource.")
					.isNull();

			// 'a' resources must be found first for duplicate resources
			resource = tb25.getResource("resources/all.txt");
			assertThat(resource)
					.as("Did not find resource.")
					.isNotNull();
			assertThat(getValue(resource))
					.as("Wrong resource")
					.isEqualTo("a.all.txt");

			// test non shadowed resources
			resource = tb25.getResource("resources/a.txt");
			assertThat(resource)
					.as("Did not find resource.")
					.isNotNull();
			resource = tb25.getResource("resources/b.txt");
			assertThat(resource)
					.as("Did not find resource.")
					.isNotNull();

			// test get resources for shadowed resource
			// again the root resource must not be found
			Enumeration<URL> resources = null;
			try {
				resources = tb25.getResources("resources/all.txt");
			} catch (IOException e) {
				fail("Unexpected io exception.", e);
			}
			assertThat(resources)
					.as("Did not find resources.")
					.isNotNull();
			// there are only two resources from 'a' and 'b' in that order.
			try {
				resource = resources.nextElement();
				assertThat(resource)
						.as("Did not find resource.")
						.isNotNull();
				assertThat(getValue(resource))
						.as("Wrong resource")
						.isEqualTo("a.all.txt");
				resource = resources.nextElement();
				assertThat(resource)
						.as("Did not find resource.")
						.isNotNull();
				assertThat(getValue(resource))
						.as("Wrong resource")
						.isEqualTo("b.all.txt");
				assertThat(resources.hasMoreElements())
						.as("Expecting no more resources.")
						.isFalse();
			} catch (NoSuchElementException e) {
				fail("Wrong number of elements.", e);
			}
			// after all the getResource calls the bundle must be RESOLVED or INSTALLED depending on resolved param
			assertThat(tb25.getState())
					.as("Wrong state for bundle.")
					.isEqualTo(resolved ? Bundle.RESOLVED : Bundle.INSTALLED);
		} finally {
			try {
				tb25.uninstall();
			} catch (BundleException e) {
				// ignore
			}
		}
	}

	private String getValue(URL url) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			return reader.readLine();
		} catch (IOException e) {
			return null;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	@Test
	public void testBundleGetResources(
			@InjectInstalledBundle(value = "div.tb10.jar", start = false) Bundle bundle,
			@InjectInstalledBundle(value = "div.tb13.jar", start = false) Bundle fragment
			) throws Exception {
		try {
			Enumeration<URL> enumeration = bundle.getResources(basePath
					+ "tb10/Foo.class");
			assertThat(enumeration)
					.as("Testing the method invocation with an existing resource (using a absolute path)")
					.isNotNull();
			enumeration = bundle.getResources(basePath + "tb10/Nonexistent");
			assertThat(enumeration)
					.as("Testing the method invocation with a nonexistent resource")
					.isNull();

			enumeration = fragment.getResources(basePath + "tb13/Foo.class");
			assertThat(enumeration)
					.as("A fragment bundle cannot return a resource using the method getResource()")
					.isNull();

			bundle.uninstall();

			try {
				bundle.getResources(basePath + "tb10/Foo.class");
				fail("Testing  the method invocation after uninstall the bundle");
			}
			catch (IllegalStateException ex) {
				// This is an expected exception and can be ignored
				bundle = null;
			}

		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
			if (fragment != null) {
				fragment.uninstall();
			}
		}
	}

	@Test
	public void testBundleGetSymbolicName1(
			@InjectInstalledBundle(value = "div.tb10.jar", start = false) Bundle bundle
			) throws Exception {
		try {
			String bsn = bundle.getSymbolicName();
			assertThat(bsn)
					.as("Testing the method getSymbolicName() with a symbolic name in the manifest")
					.isEqualTo(basePkg + "tb10");

			bundle.uninstall();
			bsn = bundle.getSymbolicName();
			bundle = null;
			assertThat(bsn)
					.as("Testing the method getSymbolicName() with a symbolic name in the manifest")
					.isEqualTo(basePkg + "tb10");

		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBundleGetSymbolicName2(
			@InjectInstalledBundle(value = "div.tb11.jar", start = false) Bundle bundle
			) throws Exception {
		try {
			String bsn = bundle.getSymbolicName();
			assertThat(bsn)
					.as("Testing the method getSymbolicName() without a symbolic name in the manifest")
					.isNull();

			bundle.uninstall();
			bsn = bundle.getSymbolicName();
			bundle = null;

			assertThat(bsn)
					.as("Testing the method getSymbolicName() after uninstall the bundle (without a symbolic name in the manifest)")
					.isNull();
		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBundleGetBundleContext1(
			@InjectInstalledBundle(value = "div.tb10.jar", start = false) Bundle bundle
			) throws Exception {
		try {
			assertThat(bundle.getBundleContext())
					.as("BundleContext for installed bundle must be null")
					.isNull();

			bundle.start();
			assertThat(bundle.getBundleContext())
					.as("BundleContext for started bundle must not be null")
					.isNotNull();

			assertThat(bundle.getBundleContext().getBundle()
							.getBundleId())
					.as("Bundle id via BundleContext must equal original id")
					.isEqualTo(bundle.getBundleId());

			bundle.stop();
			assertThat(bundle
					.getBundleContext())
					.as("BundleContext for stopped bundle must be null")
					.isNull();
		}
		finally {
			assertThat(bundle.getBundleContext())
					.as("BundleContext for uninstalled bundle must be null")
					.isNull();
		}

		bundle = bundleContext.getBundle(0);

		assertThat(bundle.getBundleContext())
				.as("BundleContext for system bundle must not be null")
				.isNotNull();

		assertThat(bundle
				.getBundleContext().getBundle().getBundleId())
				.as("Bundle id via BundleContext must equal zero").isZero();

		// the injected context is a proxy: compare the bundles behind both contexts
		assertThat(bundleContext.getBundle().getBundleContext().getBundle())
				.as("BundleContext for test case should match context passed to activator")
				.isEqualTo(bundleContext.getBundle());
	}

	@Test
	public void testBundleGetBundleContext2(
			@InjectInstalledBundle(value = "div.tb18.jar", start = false) Bundle fragment,
			@InjectInstalledBundle(value = "div.tb17.jar", start = false) Bundle host
			) throws Exception {
		host.start(); // resolve the bundles
		assertThat(host.getBundleContext())
				.as("BundleContext for host bundle must not be null")
				.isNotNull();
		assertThat(fragment.getBundleContext())
				.as("BundleContext for fragment bundle must be null")
				.isNull();

		host.stop();
		assertThat(host.getBundleContext())
				.as("BundleContext for stopped host bundle must be null")
				.isNull();
		assertThat(fragment.getBundleContext())
				.as("BundleContext for fragment bundle must be null")
				.isNull();
	}

	@Test
	public void testBundleLoadClass1(
			@InjectInstalledBundle(value = "div.tb10.jar", start = false) Bundle bundle
			) throws Exception {
		try {
			bundle.start();
			Class< ? > clazz = null;
			try {
				clazz = bundle.loadClass(basePkg + "tb10.Foo");
			}
			catch (ClassNotFoundException ex) {
				fail("Testing the method loadClass() with an installed bundle and a existing class", ex);
			}

			ServiceReference< ? > sr = bundleContext
					.getServiceReference(
					basePkg + "tb10.TestService");

			Object service = bundleContext.getService(sr);
			ClassLoader classLoader = (ClassLoader) service.getClass()
					.getMethod("getClassLoader", (Class[]) null).invoke(service, (Object[]) null);
			assertThat(classLoader)
					.as("Expecting the ClassLoader of the class and the bundle to be the same")
					.isEqualTo(clazz.getClassLoader());

			try {
				clazz = bundle
						.loadClass(basePkg + "tb10.NonExistent");
				fail("Testing the method loadClass() with an installed bundle and a nonexistent class");
			}
			catch (ClassNotFoundException ex) {
				// This is an expected exception and can be ignored
			}

			bundle.uninstall();
			try {
				bundle.loadClass(basePkg + "tb10.Foo");
				fail("Testing the method after uninstall the bundle");
			}
			catch (IllegalStateException ex) {
				// This is an expected exception and can be ignored
				bundle = null;
			}
		}
		finally {
			if (bundle != null) {
				bundle.uninstall();
			}
		}
	}

	@Test
	public void testBundleLoadClass2(
			@InjectInstalledBundle(value = "div.tb13.jar", start = false) Bundle bundle
			) throws Exception {
		assertThatExceptionOfType(ClassNotFoundException.class)
				.as("Testing the method loadClass() with a fragment bundle")
				.isThrownBy(() -> bundle.loadClass(basePkg + "tb13.Foo"));

		assertThatExceptionOfType(ClassNotFoundException.class)
				.as("Testing the method loadClass() with a fragment bundle")
				.isThrownBy(() -> bundle.loadClass(basePkg + "tb13.Nonexistent"));
	}

	/**
	 * Tests service registration.
	 */
	@Test
	public void testBundleContextRegisterService(
			@InjectInstalledBundle(value = "div.tb24a.jar", start = false) Bundle tb24a,
			@InjectInstalledBundle(value = "div.tb24b.jar", start = false) Bundle tb24b,
			@InjectInstalledBundle(value = "div.tb24c.jar", start = false) Bundle tb24c
			) throws Exception {
		tb24a.start();
		tb24b.start();

		try {
			tb24c.start();
			tb24c.stop();
		}
		catch (BundleException ex) {
			fail("A bundle can register a service when the package is shared");
		}
	}

	private String reportProcessorOS() {
		String os = bundleContext.getProperty("org.osgi.framework.os.name");
		String proc = bundleContext.getProperty("org.osgi.framework.processor");
		StringBuffer sb = new StringBuffer();
		sb.append("Current osname=\"").append(os).append("\" processor=\"")
				.append(proc);
		sb
				.append("\". For allowed constants see https://docs.osgi.org/reference/");
		return sb.toString();
	}

	/**
	 * The field is a public static final constant of the given value.
	 */
	private static void assertConstant(Object expected, String fieldName, Class< ? > fieldClass) {
		Field field;
		try {
			field = fieldClass.getField(fieldName);
		} catch (NoSuchFieldException e) {
			throw new AssertionError("missing field: " + fieldName, e);
		}
		assertThat(field.getModifiers()).as("modifiers of %s", fieldName)
				.matches(Modifier::isPublic, "public")
				.matches(Modifier::isStatic, "static")
				.matches(Modifier::isFinal, "final");
		try {
			assertThat(field.get(null)).as(fieldName).isEqualTo(expected);
		} catch (IllegalAccessException e) {
			throw new AssertionError("bad field: " + fieldName, e);
		}
	}
}
