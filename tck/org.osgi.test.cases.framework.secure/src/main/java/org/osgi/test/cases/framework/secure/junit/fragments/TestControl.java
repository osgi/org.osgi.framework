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
package org.osgi.test.cases.framework.secure.junit.fragments;


import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.net.URL;
import java.util.Hashtable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;

/**
 * Test cases for testing fragment bundles and extension bundles.
 * 
 * @author $Id$
 */
public class TestControl {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;
	private ServiceRegistration<?> bundleReg;

	@BeforeEach
	public void setUp() throws Exception {
		assertThat(System
				.getSecurityManager())
				.as("SecurityManager not installed")
				.isNotNull();
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("bundle", "fragments.tests");
		bundleReg = bundleContext.registerService(Bundle.class.getName(), bundleContext.getBundle(), props);
	}

	@AfterEach
	public void tearDown() throws Exception {
		bundleReg.unregister();
	}

	/**
	 * Tests that in order for a host bundle to allow fragments to attach, the
	 * host bundle must have BundlePermission[ <bundle symbolic name>,"host"].
	 * In order to be allowed to attach to a host bundle, a fragment bundle must
	 * have BundlePermission[ <bundle symbolic name>,"fragment"].
	 * 
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testBundlePermission(
			@InjectInstalledBundle(value = "fragments.tb2b.jar", start = false) Bundle tb2b,
			@InjectInstalledBundle(value = "fragments.tb2a.jar", start = true) Bundle tb2a
			) throws Exception {
		// Install fragment bundle with normal permissions

		// Install host without BundlePermission (host)

		assertThat(tb2a)
				.as("Expecting the state of host bundle to be ACTIVE").isInState(Bundle.ACTIVE);

		// Test that state of fragment is INSTALLED (not attached to host)
		assertThat(tb2b)
				.as("Expecting the state of fragment bundle to be INSTALLED").isInState(Bundle.INSTALLED);

		// Install fragment bundle without BundlePermission (fragment)
		Bundle tb2d = bundleInstaller.installBundle("fragments.tb2d.jar", false);

		// Install host with normal permissions
		Bundle tb2c = bundleInstaller.installBundle("fragments.tb2c.jar", false);
		tb2c.start();

		try {
			assertThat(tb2c)
					.as("Expecting the state of host bundle to be ACTIVE").isInState(Bundle.ACTIVE);

			// Test that state of fragment is INSTALLED (not attached to host)
			assertThat(tb2d)
					.as("Expecting the state of fragment bundle to be INSTALLED").isInState(Bundle.INSTALLED);
		}
		finally {
			tb2c.stop();
			tb2c.uninstall();
			tb2d.uninstall();
		}
	}

	/**
	 * When a URL object to a resource within a bundle is created, the caller is
	 * checked for AdminPermission to access the resource if the resource is
	 * located in another bundle. If the caller does not have the necessary
	 * permission, the resource is not accessible and SecurityException is
	 * thrown. If the caller has the necessary permission, then the URL object
	 * to the resource is successfully created. Once the URL object is created,
	 * no further permission checks are performed when the contents of the
	 * resource are accessed.
	 * 
	 * The java.net.URLStreamHandlers for bundle resource and entry URL objects
	 * must perform a permissions check to allow access to the URL. When a URL
	 * is constructed using the following java.net.URL constructors the parseURL
	 * method of the URLStreamHandler is called: URL(String spec); URL(URL
	 * context, String spec); URL(URL context, String spec, URLStreamHandler
	 * handler)
	 * 
	 * When one of these constructors is called for a URL that uses the bundle
	 * resource or entry protocol scheme then the parseURL method of the
	 * URLStreamHandler must check the caller for the necessary permissions. If
	 * the caller does not have the necessary permissions then a
	 * SecurityException must be thrown. If the caller has the necessary
	 * permissions, then the URL object is setup to access the bundle resource
	 * and the authority of the URL is set to a Framework defined value. When
	 * the content of the URL is accessed a check is done to see if the
	 * authority of the URL has been set to the Framework defined value. If it
	 * is set then the caller is allow access, if not then the caller is checked
	 * for the necessary permissions.
	 * 
	 * The following java.net.URL constructors do not call parseURL to setup a
	 * URL to use a specific protocol: URL(String protocol, String host, int
	 * port, String file); URL(String protocol, String host, int port, String
	 * file, URLStreamHandler handler); URL(String protocol, String host, String
	 * file)
	 * 
	 * When one of these constructors is called the authority of the constructed
	 * URL is not set. When the content of one of these URLs that use the bundle
	 * resource or entry protocol scheme is accessed then the caller is checked
	 * for the necessary permissions because the authority of the URL is not set
	 * to the Framework defined value.
	 * 
	 * @throws Exception if an error occurs or an assertion fails in the test.
	 * @osgi.spec Bundle.installBundle(String)
	 */
	@Test
	public void testResourceAccessPermission(
			@InjectInstalledBundle(value = "fragments.tb7g.jar", start = true) Bundle tb7g,
			@InjectInstalledBundle(value = "fragments.tb7f.jar", start = true) Bundle tb7f
			) throws Exception {
		// Install bundle that has the resource
		// Install bundle that will try to access the resource
		// Execute the method that will access the resource using reflection
		Class<?> classObj = tb7f
				.loadClass("org.osgi.test.cases.framework.secure.fragments.tb7f.TestClass");
		Object obj = classObj.getConstructor().newInstance();
		URL resourceURL = tb7g.getResource("/resources/resource.txt");
		classObj.getMethod("run", new Class[] {Bundle.class, URL.class})
				.invoke(obj, new Object[] {tb7g, resourceURL});
	}
}
