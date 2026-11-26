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
package org.osgi.test.cases.framework.junit.wiring;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * A package exported by a fragment is provided by the host. The
 * bundle-symbolic-name and bundle-version attributes associated with the
 * export are those of the host, not those of the fragment. See the Export
 * Package section of the Module Layer chapter.
 */
public class FragmentHostedAttributesTests extends WiringTest {

	private Bundle installBundle(String name, String version, String fragmentHost,
			String exports, String imports) throws Exception {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, name);
		attributes.putValue(Constants.BUNDLE_VERSION, version);
		if (fragmentHost != null) {
			attributes.putValue(Constants.FRAGMENT_HOST, fragmentHost);
		}
		if (exports != null) {
			attributes.putValue(Constants.EXPORT_PACKAGE, exports);
		}
		if (imports != null) {
			attributes.putValue(Constants.IMPORT_PACKAGE, imports);
		}
		File file = bundleContext.getDataFile(name + "-" + version + ".jar");
		try (JarOutputStream jos = new JarOutputStream(
				new FileOutputStream(file), m)) {
			jos.flush();
		}
		Bundle result = bundleContext.installBundle(file.toURI().toString());
		bundles.add(result);
		return result;
	}

	private Bundle providerOf(Bundle importer, String packageName) {
		BundleWiring wiring = importer.adapt(BundleWiring.class);
		assertThat(wiring).as("importer is resolved").isNotNull();
		return wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)
				.stream()
				.filter(w -> packageName.equals(w.getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE)))
				.map(BundleWire::getProvider)
				.map(r -> r.getBundle())
				.findFirst()
				.orElse(null);
	}

	@Test
	public void testImportByNameOfTheHost() throws Exception {
		String host = getName() + ".host";
		Bundle h = installBundle(host, "1.0.0", null, null, null);
		Bundle f = installBundle(getName() + ".fragment", "5.0.0",
				host + ";bundle-version=\"[1.0,2.0)\"", "foo;version=1.0",
				null);
		Bundle importer = installBundle(getName() + ".importer", "1.0.0", null,
				null, "foo;bundle-symbolic-name=\"" + host
						+ "\";bundle-version=\"[1.0,2.0)\"");

		assertThat(frameworkWiring.resolveBundles(Arrays.asList(h, f, importer)))
				.as("the bundles resolve").isTrue();
		assertThat(providerOf(importer, "foo")).isEqualTo(h);
	}

	@Test
	public void testImportByNameOfTheFragment() throws Exception {
		String host = getName() + ".host";
		String fragment = getName() + ".fragment";
		Bundle h = installBundle(host, "1.0.0", null, null, null);
		Bundle f = installBundle(fragment, "5.0.0",
				host + ";bundle-version=\"[1.0,2.0)\"", "foo;version=1.0",
				null);
		Bundle importer = installBundle(getName() + ".importer", "1.0.0", null,
				null, "foo;bundle-symbolic-name=\"" + fragment + "\"");

		frameworkWiring.resolveBundles(Arrays.asList(h, f, importer));
		assertThat(importer.adapt(BundleWiring.class))
				.as("the importer does not resolve").isNull();
	}

	@Test
	public void testBundleVersionIsTheOneOfTheHost() throws Exception {
		String host = getName() + ".host";
		Bundle h = installBundle(host, "1.0.0", null, null, null);
		Bundle f = installBundle(getName() + ".fragment", "5.0.0",
				host + ";bundle-version=\"[1.0,2.0)\"", "foo;version=1.0",
				null);
		Bundle byFragmentVersion = installBundle(getName() + ".byfragment",
				"1.0.0", null, null,
				"foo;bundle-version=\"[5.0,6.0)\"");

		frameworkWiring.resolveBundles(Arrays.asList(h, f, byFragmentVersion));
		assertThat(byFragmentVersion.adapt(BundleWiring.class))
				.as("the version of the fragment is not the bundle-version")
				.isNull();
	}

	@Test
	public void testFragmentAttachedToSeveralHosts() throws Exception {
		String host = getName() + ".host";
		Bundle h1 = installBundle(host, "1.0.0", null, null, null);
		Bundle h2 = installBundle(host, "2.0.0", null, null, null);
		Bundle f = installBundle(getName() + ".fragment", "5.0.0", host,
				"foo;version=1.0", null);
		Bundle importer = installBundle(getName() + ".importer", "1.0.0", null,
				null, "foo;bundle-symbolic-name=\"" + host
						+ "\";bundle-version=\"[1.0,2.0)\"");

		assertThat(frameworkWiring
				.resolveBundles(Arrays.asList(h1, h2, f, importer))).isTrue();
		assertThat(providerOf(importer, "foo")).isEqualTo(h1);
	}
}
