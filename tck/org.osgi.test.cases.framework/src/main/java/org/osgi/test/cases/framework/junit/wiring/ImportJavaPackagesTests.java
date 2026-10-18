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
package org.osgi.test.cases.framework.junit.wiring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ImportJavaPackagesTests extends WiringTest {

	
	private static final String	JAVA_LANG	= "java.lang";
	private static final String	JAVA_UTIL	= "java.util";
	private Bundle				testBundle;

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();

	}

	@AfterEach
	public void tearDown() throws Exception {
		if (testBundle != null) {
			testBundle.uninstall();
		}
		super.tearDown();
	}

	private File createBundle(String bundleName, Map<String,String> headers)
			throws IOException {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue());
		}
		File file = bundleContext.getDataFile(bundleName + ".jar");
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), m);
		jos.flush();
		jos.close();
		return file;
	}

	@Test
	public void testExportPackageCannotContainJavaPackages() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		headers.put(Constants.EXPORT_PACKAGE, JAVA_LANG);
		File bundle = createBundle(getName(), headers);
		try {
			testBundle = bundleContext.installBundle(bundle.toURI().toString());
			testBundle.start();
			fail("Failed to test Export-Package header");
		} catch (BundleException e) {
			assertThat(e.getType())
					.as("It should throw a bundle exception of type manifest error")
				.isEqualTo(BundleException.MANIFEST_ERROR);
		}

	}

	@Test
	public void testImportPackageCanContainJavaPackages() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		headers.put(Constants.IMPORT_PACKAGE, JAVA_LANG);
		File bundle = createBundle(getName(), headers);
		try {
			testBundle = bundleContext.installBundle(bundle.toURI().toString());
			testBundle.start();
			Dictionary<String, String> testHeaders = testBundle.getHeaders();
			assertThat(testHeaders.get(Constants.IMPORT_PACKAGE).contains(JAVA_LANG))
					.as(Constants.IMPORT_PACKAGE + " does not contain the java.* package")
				.isTrue();
			List<BundleWire> pkgWires = testBundle.adapt(BundleWiring.class).getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
			assertThat(pkgWires.size())
					.as("Wrong number of package requiremens: ")
				.isEqualTo(1);
			assertThat(pkgWires.get(0).getCapability().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))
					.as("Wrong package found: " + pkgWires.get(0))
				.isEqualTo(JAVA_LANG);
			assertThat(pkgWires.get(0).getProvider())
					.as("Wrong exporter of java package.")
				.isEqualTo(bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleRevision.class));
		} catch (BundleException e) {
			fail("Failed to test Import-Package header");
		} 
	}

	@Test
	public void testSystemPackages() throws Exception {

		Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		BundleRevision br = systemBundle.adapt(BundleRevision.class);

		Dictionary<String, String> testHeaders = systemBundle.getHeaders();
		assertThat(testHeaders.get(Constants.EXPORT_PACKAGE).contains(JAVA_LANG))
				.as(Constants.EXPORT_PACKAGE + " does not contain the java.* package")
				.isTrue();
		assertThat(testHeaders.get(Constants.EXPORT_PACKAGE).contains(JAVA_UTIL))
				.as(Constants.EXPORT_PACKAGE + " does not contain the java.* package")
				.isTrue();

		String systemPackages = bundleContext.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
		assertThat(systemPackages.contains(JAVA_LANG))
				.as("System packages should include java.* packages")
				.isTrue();
		assertThat(systemPackages.contains(JAVA_UTIL))
				.as("System packages should include java.* packages")
				.isTrue();

		List<Capability> capabilities = br.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		int count = 0;
		for(Capability capability: capabilities) {
			if (capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).toString().startsWith("java.")) {
				count++;
			}
		}
		
		assertThat(count > 0)
				.as("No java.* packages found")
				.isTrue();

		Collection<BundleCapability> bundleCapabilities = frameworkWiring.findProviders(new Requirement() {
			@Override
			public Resource getResource() {
				return null;
			}
			
			@Override
			public String getNamespace() {
				return PackageNamespace.PACKAGE_NAMESPACE;
			}
			
			@Override
			public Map<String,String> getDirectives() {
				return Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "("+PackageNamespace.PACKAGE_NAMESPACE+"=java.*)");
			}
			
			@Override
			public Map<String,Object> getAttributes() {
				return Collections.emptyMap();
			}
		});

		assertThat(bundleCapabilities)
				.as("Capabities should be non empty").isNotEmpty();
		
		for(BundleCapability cap: bundleCapabilities) {
			assertThat(cap.getRevision())
					.as("java.* capabilities should only come from system bundle")
				.isEqualTo(br);
		}
	}
}
