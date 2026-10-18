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
package org.osgi.test.cases.framework.secure.junit.lifecycle;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.security.AccessControlContext;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServicePermission;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.test.cases.framework.secure.lifecycle.servicereferencegetter.ServiceReferenceGetter;
import org.osgi.test.cases.framework.secure.lifecycle.servicereferencegetter.TestResult;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;

/**
 * This is the bundle initially installed and started by the TestCase when
 * started. It performs the various tests and reports back to the TestCase.
 *
 *
 * The testPermissions test is currently removed because of unknown state. It
 * uses tb10 but this seems to be removed from the upper makefile.
 *
 * @author Ericsson Radio Systems AB
 */
public class TestBundleControl {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	/**
	 * Test the permissions in the framework
	 */
	@Test
	public void testPermissions() throws Throwable {
		// tb10 updates tb5 from its location, so install tb5 by URL rather than by stream
		Bundle tb = bundleContext.installBundle(bundleContext.getBundle().getEntry("lifecycle.tb5.jar").toExternalForm());
		Bundle tbPerm = bundleInstaller.installBundle("lifecycle.tb10.jar", false);
		try {
			tb.start();
			tbPerm.start();
			ServiceReferenceGetter serviceReferenceGetter = bundleContext.getService(
					bundleContext.getServiceReference(ServiceReferenceGetter.class));
			serviceReferenceGetter.setServiceReference(bundleContext
					.getServiceReference(PermissionAdmin.class.getName()));
			Throwable result = ((TestResult) serviceReferenceGetter).get();
			if (result != null)
				throw result;
		}
		finally {
			if ((tb.getState() & Bundle.UNINSTALLED) == 0) {
				tb.uninstall();
			}
		}
	}

	@Test
	public void testHasPermission(
			@InjectInstalledBundle(value = "lifecycle.tb5.jar", start = false) Bundle tb
			) throws Throwable {
		assertThat(tb.hasPermission(new ServicePermission(
						"org.osgi.test.cases.framework.secure.lifecycle.tb5.EventTest",
						ServicePermission.REGISTER)))
				.as("missing permission")
				.isTrue();
		assertThat(tb.hasPermission(new ServicePermission(
						"org.osgi.test.cases.framework.secure.lifecycle.tb5.EventTest",
						ServicePermission.GET)))
				.as("has extra permission")
				.isFalse();
	}

	@Test
	public void testAccessControlContext(
			@InjectInstalledBundle(value = "lifecycle.tb5.jar", start = false) Bundle tb
			) throws Throwable {
		AccessControlContext acc = tb.adapt(AccessControlContext.class);
		assertThat(acc)
				.as("no adapt to accesscontrolcontext")
				.isNotNull();
		try {
			acc.checkPermission(new ServicePermission(
					"org.osgi.test.cases.framework.secure.lifecycle.tb5.EventTest",
					ServicePermission.REGISTER));
		}
		catch (SecurityException se) {
			fail("unexpected exception", se);
		}
		assertThatExceptionOfType(SecurityException.class)
				.as("expected security exception")
				.isThrownBy(() -> {
					acc.checkPermission(new ServicePermission(
							"org.osgi.test.cases.framework.secure.lifecycle.tb5.EventTest",
							ServicePermission.GET));
				});
	}
}
