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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

public class ConditionServicePermisionTest {

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;
	private final List<Bundle>								bundles			= new ArrayList<>();

	public Bundle install(String bundle) {
		Bundle result = bundleInstaller.installBundle(bundle, false);
		if (!bundles.contains(result))
			bundles.add(result);
		return result;
	}

	@BeforeEach
	public void setUp() throws Exception {
		bundles.clear();
	}

	@AfterEach
	public void tearDown() throws Exception {
		for (Iterator<Bundle> iBundles = bundles.iterator(); iBundles
				.hasNext();)
			try {
				iBundles.next().uninstall();
			} catch (BundleException e) {
				// nothing
			} catch (IllegalStateException e) {
				// happens if the test uninstalls the bundle itself
			}
		bundles.clear();
	}

	@Test
	public void testImpliedConditonServicePermision() throws BundleException {
		// install bundle with osgi.ee requirement
		Bundle tester = install("permissions.condition.jar");
		tester.start();
	}

}
