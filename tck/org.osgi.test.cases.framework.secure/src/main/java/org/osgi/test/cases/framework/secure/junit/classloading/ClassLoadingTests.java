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
package org.osgi.test.cases.framework.secure.junit.classloading;



import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;
import org.osgi.test.common.wiring.FrameworkWirings;

/**
 * This class contains tests related with the framework class loading policies.
 * 
 * @author left
 * @author $Id$
 */
public class ClassLoadingTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	@BeforeEach
	public void refreshBeforeTest() {
		// injected bundles are uninstalled after @AfterEach: refresh before the next test
		FrameworkWirings.refreshBundles(bundleContext);
	}

	@AfterEach
	public void tearDown() {
		FrameworkWirings.refreshBundles(bundleContext);
	}

	// Service Registry --------------------------

	/**
	 * Since multiple modules may export permission classes with the same class
	 * name, the framework must make sure that permission checks are performed
	 * using the correct class. The net result is that the framework needs to
	 * look up permissions based on class rather than class name and when it
	 * needs to instantiate a permission it needs to use the class of the
	 * permission being checked to do the instantiation. This is a complication
	 * for framework implementers; bundle programmers are not affected.
	 * 
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPermissionChecking001(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb6a.jar", start = true) Bundle tb6a,
			@InjectInstalledBundle(value = "classloading.tb6b.jar", start = true) Bundle tb6b,
			@InjectInstalledBundle(value = "classloading.tb6c.jar", start = true) Bundle tb6c,
			@InjectInstalledBundle(value = "classloading.tb6d.jar", start = false) Bundle tb6d
			) throws Exception {
		try {
			System.out.println("Changing a permission class to avoid authorization checking");
			tb6d.start();
			tb6d.stop();
		}
		catch (IllegalStateException ex) {
			fail(ex.getMessage());
		}
	}

	// Exporting System Classes --------------------

	/**
	 * Test that in order to be allowed to require a name bundle, the requiring
	 * bundle must have:
	 * 
	 * BundlePermission[ <required bundle symbolic name>, REQUIRE_BUNDLE]
	 * 
	 * Test that when the resolution directive has a value of "mandatory" the
	 * required bundle must be resolved if the requiring module is resolved.
	 * 
	 * @osgi.spec Bundle.start()
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle003(
			@InjectInstalledBundle(value = "classloading.tb16b.jar", start = false) Bundle tb16b,
			@InjectInstalledBundle(value = "classloading.tb16d.jar", start = false) Bundle tb16d
			) throws Exception {
		try {
			tb16d.start();
			tb16d.stop();
			fail("Expecting BundleException");
		}
		catch (BundleException e) {
			// expected
		}
	}

}
