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

package org.osgi.test.cases.framework.junit.frameworkutil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.test.common.annotation.InjectBundleContext;

public class BundleReferenceTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@Test
	public void testBundleReference() {
		Bundle me = bundleContext.getBundle();
		Class< ? > self = getClass();
		ClassLoader cl = self.getClassLoader();
		assertThat(cl instanceof BundleReference)
				.as("classloader is not an instanceof BundleReference")
				.isTrue();
		BundleReference ref = (BundleReference) cl;
		assertThat(ref.getBundle())
				.as("wrong bundle returned")
				.isEqualTo(me);
	}

	@Test
	public void testGetBundleFromClass() {
		Bundle me = bundleContext.getBundle();
		Class< ? > self = getClass();
		assertThat(FrameworkUtil.getBundle(self))
				.as("wrong bundle returned")
				.isEqualTo(me);
	}

	@Test
	public void testGetBundleFromClassLoader() {
		Bundle me = bundleContext.getBundle();
		BundleWiring wiring = me.adapt(BundleWiring.class);
		assertThat(wiring).as("bundle wiring").isNotNull();
		ClassLoader cl = wiring.getClassLoader();
		assertThat(cl).as("bundle wiring class loader").isNotNull();
		assertThat(FrameworkUtil.getBundle(cl)).as("bundle returned")
				.hasValue(me);
	}

	@Test
	public void testBootClass() {
		assertThat(FrameworkUtil.getBundle(Object.class))
				.as("expected null")
				.isNull();
	}

	@Test
	public void testGetBundleFromClassNull() {
		assertThatThrownBy(() -> FrameworkUtil.getBundle((Class<?>) null)).isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testGetBundleFromClassLoaderNull() {
		assertThatThrownBy(() -> FrameworkUtil.getBundle((ClassLoader) null)).isInstanceOf(RuntimeException.class);
	}
}
