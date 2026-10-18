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
package org.osgi.test.cases.framework.launch.secure.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

/**
 * Test cases for testing fragment bundles and extension bundles.
 * 
 * @author $Id$
 */
public class ExtensionBundleTests extends LaunchTest {

	private Framework framework;

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		Map<String, String> configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_SECURITY, "osgi");
		configuration.put(Constants.FRAMEWORK_STORAGE,
				getStorageArea(getName(), true).getAbsolutePath());
		framework = createFramework(configuration);
		initFramework(framework);
		assertThat(System.getSecurityManager())
				.as("Null SecurityManager")
				.isNotNull();
		startFramework(framework);
	}

	@AfterEach
	public void tearDown() throws Exception {
		stopFramework(framework);
		super.tearDown();
	}

	/**
	 * Tests if a framework extension bundle has to have
	 * <code>AllPermission</code> permission to be installed. Will only
	 * perform this test if <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals
	 * <code>true</code>.
	 * 
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionPermission() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			String message = "expected extension bundle to fail install due to lack of permission";
			assertThatExceptionOfType(BundleException.class)
					.as(message)
					.isThrownBy(() -> installBundle(framework, "/fragments.tb11.jar"));
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}

	/**
	 * Tests if a bundle has to have at least
	 * <code>AdminPermission[<bundle>, EXTENSIONLIFECYCLE]</code> permission
	 * to install a framework extension bundle. Will only perform this test if
	 * <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals <code>true</code>.
	 * 
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionInvokerPermission() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			// install regular bundle
			Bundle tb16a = installBundle(framework, "/fragments.tb16a.jar");
			Dictionary<String, String> props = new Hashtable<String, String>();
			props.put("bundle", "fragments.tb16b.jar");
			InputStream tb16b = getBundleInput("/fragments.tb16b.jar")
					.openStream();
			framework.getBundleContext().registerService(InputStream.class,
					tb16b, props);
			// start regular bundle that tries to install a framework
			// extension bundle
			tb16a.start();
			// installation inside start should fail, since
			// bundles have to have
			// AdminPermission[<bundle>, EXTENSIONLIFECYCLE]
			// to install extension bundles
			System.out.println("prevented bundle without permission from installing "
							+ "a framework extension bundle");
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}

	/**
	 * Tests if a bundle has to have at least
	 * <code>AdminPermission[<bundle>, EXTENSIONLIFECYCLE]</code> permission
	 * to install a framework extension bundle. Will only perform this test if
	 * <code>SUPPORTS_FRAMEWORK_EXTENSION</code> equals <code>true</code>.
	 * 
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testFrameworkExtensionInvokerPermissionOk() throws Exception {
		if ("true".equals(framework.getBundleContext().getProperty(
				Constants.SUPPORTS_FRAMEWORK_EXTENSION))) {
			// install regular bundle
			Bundle tb18 = installBundle(framework, "/fragments.tb18.jar");
			Dictionary<String, String> props = new Hashtable<String, String>();
			props.put("bundle", "fragments.tb16b.jar");
			InputStream tb16b = getBundleInput("/fragments.tb16b.jar")
					.openStream();
			framework.getBundleContext().registerService(InputStream.class,
					tb16b, props);
			// start regular bundle that tries to install a framework
			// extension bundle
			tb18.start();
			// installation inside start should not fail, since
			// bundle has AdminPermission[<bundle>, EXTENSIONLIFECYCLE]
			System.out.println("bundle with permission installed "
					+ "a framework extension bundle");
		}
		else {
			System.out.println(
					"framework extension bundles not supported");
		}
	}
}
