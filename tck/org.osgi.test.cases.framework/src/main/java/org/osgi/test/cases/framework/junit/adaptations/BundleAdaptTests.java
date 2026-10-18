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

package org.osgi.test.cases.framework.junit.adaptations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.test.common.annotation.InjectBundleContext;

public class BundleAdaptTests {

	@InjectBundleContext BundleContext bundleContext;

	@Test
	public void testBundleContextAdapt() {
		doAdaptTest(BundleContext.class, bundleContext.getBundle());
	}

	@Test
	public void testBundleRevisionAdapt() {
		doAdaptTest(BundleRevision.class, bundleContext.getBundle());
	}

	@Test
	public void testBundleRevisionsAdapt() {
		doAdaptTest(BundleRevisions.class, bundleContext.getBundle());
	}

	@Test
	public void testBundleStartLevelAdapt() {
		doAdaptTest(BundleStartLevel.class, bundleContext.getBundle());
	}

	@Test
	public void testBundleWiringAdapt() {
		doAdaptTest(BundleWiring.class, bundleContext.getBundle());
	}

	@Test
	public void testFrameworktAdapt() {
		doAdaptTest(Framework.class, bundleContext.getBundle(0));
		assertThat(bundleContext.getBundle().adapt(Framework.class))
				.as("Expecting null adapt for Framework type.")
				.isNull();
	}

	@Test
	public void testFrameworktStartLevel() {
		doAdaptTest(FrameworkStartLevel.class, bundleContext.getBundle(0));
		assertThat(bundleContext.getBundle().adapt(FrameworkStartLevel.class))
				.as("Expecting null adapt for Framework type.")
				.isNull();
	}

	@Test
	public void testFrameworktWiring() {
		doAdaptTest(FrameworkWiring.class, bundleContext.getBundle(0));
		assertThat(bundleContext.getBundle().adapt(FrameworkWiring.class))
				.as("Expecting null adapt for Framework type.")
				.isNull();
	}

	private <T> void doAdaptTest(Class<T> clazz, Bundle b) {
		T adapted = b.adapt(clazz);
		assertThat(adapted)
				.as("Adapted type is null: " + clazz.getName())
				.isNotNull();
		assertThat(clazz.isAssignableFrom(adapted.getClass()))
				.as("Wrong adapted type: " + adapted.getClass())
				.isTrue();
	}
}
