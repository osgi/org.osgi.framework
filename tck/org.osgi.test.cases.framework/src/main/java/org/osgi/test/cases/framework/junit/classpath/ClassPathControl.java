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
package org.osgi.test.cases.framework.junit.classpath;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;

public class ClassPathControl {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	/** *** Test methods **** */
	/**
	 * Tries to import a package that has not been installed.
	 */
	@Test
	public void testImportUninstalledCode() throws Exception {
		String s = bundleContext.getBundle().getEntry("classpath.tb1.jar").toExternalForm();
		System.out.println("Web req " + s);
		try {
			URL url = new URL(s);
			System.out.println("Length "
					+ url.openConnection().getContentLength());
			Bundle tb = bundleContext.installBundle(s);
			try {
				assertThat(tb).isInState(Bundle.INSTALLED);
				assertThatExceptionOfType(BundleException.class).as("No exception thrown, Error!")
						.isThrownBy(() -> tb.start());
			}
			finally {
				tb.uninstall();
			}
		}
		catch (Exception e) {
			fail("unexpected exception", e);
		}
	}

	/**
	 * Tests export of a package.
	 */
	@Test
	public void testExport(
			@InjectInstalledBundle(value = "classpath.tb2.jar", start = false) Bundle tb
			) throws Exception {
		try {
			tb.start();
		}
		catch (BundleException be) {
			if (tb.getState() == Bundle.INSTALLED)
				fail("Export failed!", be);
			else
				fail("BundleException thrown, Error!", be);
		}
	}

	/**
	 * Tests reinstallation of the same bundle.
	 */
	@Test
	public void testReinstall(
			@InjectInstalledBundle(value = "classpath.tb3.jar", start = false) Bundle tb1,
			@InjectInstalledBundle(value = "classpath.tb3.jar", start = false) Bundle tb2
			) throws Exception {
		assertThat(tb2)
				.as("They are not the same")
				.isEqualTo(tb1);
	}

	/**
	 * Tests removal of exported package.
	 */
	@Test
	public void testImportGone(
			@InjectInstalledBundle(value = "classpath.tb4a.jar", start = false) Bundle tba,
			@InjectInstalledBundle(value = "classpath.tb4b.jar", start = false) Bundle tbb
			) throws Exception {
		tba.start();
		assertThat(tba).isInState(Bundle.ACTIVE);
		tbb.start();
		assertThat(tbb).isInState(Bundle.ACTIVE);
	}
	
}
