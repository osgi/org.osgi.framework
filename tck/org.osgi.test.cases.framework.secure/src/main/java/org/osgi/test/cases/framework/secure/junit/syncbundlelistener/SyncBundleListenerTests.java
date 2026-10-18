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
package org.osgi.test.cases.framework.secure.junit.syncbundlelistener;



import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;

/**
 * This is the bundle initially installed and started by the TestCase when
 * started. It performs the various tests and reports back to the TestCase.
 * 
 * @author Ericsson Telecom AB
 */
public class SyncBundleListenerTests {

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;
	/**
	 * control that a security exception is thrown if we try to add a
	 * synchronous bundle listener without adminPermission
	 */
	@Test
	public void testPermission(
			@InjectInstalledBundle(value = "syncbundlelistener.tb1a.jar", start = false) Bundle permissionBundle
			) throws Exception {
		try {
			permissionBundle.start();
		}
		catch (BundleException e) {
			fail("Could not add SynchronousBundleListener", e);
		}
	}

	/**
	 * control that a security exception is thrown if we try to add a
	 * synchronous bundle listener without adminPermission
	 */
	@Test
	public void testNoPermission(
			@InjectInstalledBundle(value = "syncbundlelistener.tb1b.jar", start = false) Bundle permissionBundle
			) throws Exception {
		try {
			permissionBundle.start();
		}
		catch (BundleException e) {
			Throwable cause = e.getCause();
			if (cause instanceof SecurityException) {
				return;
			}
		}
		fail("Was able to add SynchronousBundleListener");
	}

}
