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
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Test cases for testing fragment bundles and extension bundles.
 *
 * @author $Id$
 */
public class ExtensionBundleTests extends LaunchTest {

	private static final String	class6	= "org.osgi.test.cases.framework.launch.fragments.tb6.FooTB6";
	private static final String	class21	= "org.osgi.test.cases.framework.launch.fragments.tb21.FooTB21";

	private Framework framework;

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		Map<String, String> configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE,
				getStorageArea(getName(), true).getAbsolutePath());
		framework = createFramework(configuration);
		initFramework(framework);
		Bundle tb20 = installBundle(framework, "/fragments.tb20.jar");
		tb20.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		stopFramework(framework);
		super.tearDown();
	}

	/**
	 * Tests a fragment bundle where extension directive is not system.bundle.
	 * The installation of the fragment must fail.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testBadExtensionBundle() throws Exception {
		// installing bad extension bundle
		// installation should fail
		assertThatExceptionOfType(BundleException.class)
				.as("Expected installation failure of /fragments.tb4.jar")
				.isThrownBy(() -> installBundle(framework, "/fragments.tb4.jar"));
	}

	/**
	 * Tests if a framework extension bundle's classpath is appended to the
	 * framework classpath. Will only perform this test if
	 * <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals <code>true</code>.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionBundle() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			Bundle tb6 = installBundle(framework, "/fragments.tb6.jar");
			framework.adapt(FrameworkWiring.class).resolveBundles(
					Collections.singleton(tb6));
			startFramework(framework);
			assertThat(tb6)
					.as("expected framework extension bundle to be resolved")
				.isInState(Bundle.RESOLVED);
			// check if classloader is framework classloader
			Class< ? > c = framework.loadClass(class6);
			assertThat(framework.getBundleContext().getClass()
							.getClassLoader())
					.as("expected class to be loaded by the framework classloader")
					.isEqualTo(c.getClassLoader());
		}
		else {
			// installing an extension bundle must fail
			assertThatExceptionOfType(BundleException.class)
					.as("framework extension bundle instalation should fail")
					.isThrownBy(() -> installBundle(framework, "/fragments.tb6.jar"));
			System.out.println("framework extension bundles not supported");
		}
	}

	/**
	 * Tests if a framework extension bundle is not able to load classes
	 * directly. Will only perform this test if
	 * <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals <code>true</code>.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionBundleLoadClass() throws Exception {
		String message = "expected class in extension bundle cannot be loaded";
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			Bundle tb6 = installBundle(framework, "/fragments.tb6.jar");
			framework.adapt(FrameworkWiring.class).resolveBundles(
					Collections.singleton(tb6));
			startFramework(framework);
			assertThat(tb6)
					.as("expected framework extension bundle to be resolved")
				.isInState(Bundle.RESOLVED);
			assertThatExceptionOfType(ClassNotFoundException.class)
					.as(message)
					.isThrownBy(() -> tb6.loadClass(class6));
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}

	/**
	 * Tests if an extension bundle is treated as a framework extension by
	 * default. Will only perform this test if
	 * <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals <code>true</code>.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionDefault() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			Bundle tb21 = installBundle(framework, "/fragments.tb21.jar");
			framework.adapt(FrameworkWiring.class).resolveBundles(
					Collections.singleton(tb21));
			startFramework(framework);
			assertThat(tb21)
					.as("expected framework extension bundle to be resolved")
				.isInState(Bundle.RESOLVED);
			// check if classloader is framework classloader
			Class< ? > c = framework.loadClass(class21);
			assertThat(framework.getBundleContext().getClass()
							.getClassLoader())
					.as("expected class to be loaded by the framework classloader")
					.isEqualTo(c.getClassLoader());
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}

	/**
	 * Tests if a framework extension bundle is not able to load native
	 * libraries. Will only perform this test if
	 * <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals <code>true</code>.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionNativeCode() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			String message = "expected extension bundle with Bundle-NativeCode to fail to install";
			assertThatExceptionOfType(BundleException.class)
					.as(message)
					.isThrownBy(() -> installBundle(framework, "/fragments.tb22.jar"));
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}

	/**
	 * Tests if a framework extension bundle is not able to require bundles.
	 * Will only perform this test if <code>SUPPORTS_FRAMEWORK_EXTENSION</code>
	 * equals <code>true</code>.
	 *
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionRequireBundle() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			String message = "expected extension bundle with Require-Bundle to fail to install";
			assertThatExceptionOfType(BundleException.class)
					.as(message)
					.isThrownBy(() -> installBundle(framework, "/fragments.tb10.jar"));
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}
}
