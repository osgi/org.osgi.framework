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


package org.osgi.test.cases.framework.secure.junit.permissions;

import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import org.junit.jupiter.api.Test;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;


public class AdminPermissionBundleTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	@Test
	public void testRecursion(
			@InjectInstalledBundle(value = "permissions.admin2.jar", start = false) Bundle admin2,
			@InjectInstalledBundle(value = "permissions.admin1.jar", start = false) Bundle admin1
			) throws Exception {
		admin2.start();
		admin1.start();
	}

	@Test
	public void testBundleSource() {
		AdminPermission granted = new AdminPermission("(id=*)", "class");
		AdminPermission requested = new AdminPermission(bundleContext
				.getBundle(), "class");
		assertThat(granted).implies(requested);
	}
}
