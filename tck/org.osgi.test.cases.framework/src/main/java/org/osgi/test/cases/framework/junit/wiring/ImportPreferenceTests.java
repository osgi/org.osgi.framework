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
import java.io.IOException;
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
 * An import that is not part of a class space conflict may either stay with
 * the preferred exporter or move together with the imports that have to
 * change. See the Package Constraints section of the Module Layer chapter.
 */
public class ImportPreferenceTests extends WiringTest {

	private static final String	PACKAGES	= "text;version=1.0,collections;version=1.0";

	private File createBundle(String name, String exports, String imports)
			throws IOException {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, name);
		if (exports != null) {
			attributes.putValue(Constants.EXPORT_PACKAGE, exports);
		}
		if (imports != null) {
			attributes.putValue(Constants.IMPORT_PACKAGE, imports);
		}
		File file = bundleContext.getDataFile(name + ".jar");
		try (JarOutputStream jos = new JarOutputStream(
				new FileOutputStream(file), m)) {
			jos.flush();
		}
		return file;
	}

	private Bundle installBundle(String name, String exports, String imports)
			throws Exception {
		Bundle result = bundleContext.installBundle(
				createBundle(name, exports, imports).toURI().toString());
		bundles.add(result);
		return result;
	}

	private String providerOf(Bundle importer, String packageName) {
		BundleWiring wiring = importer.adapt(BundleWiring.class);
		assertThat(wiring).as("importer is resolved").isNotNull();
		return wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)
				.stream()
				.filter(w -> packageName.equals(w.getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE)))
				.map(BundleWire::getProvider)
				.map(r -> r.getSymbolicName())
				.findFirst()
				.orElse(null);
	}

	/**
	 * The exporter C is installed first and therefore preferred. S exports the
	 * same packages and in addition functions, that uses collections. A
	 * imports all three packages. A consistent class space needs collections
	 * and functions from S. The specification leaves it open whether text
	 * stays with the preferred exporter C or moves to S together with the
	 * other imports, so both wirings are accepted.
	 * <p>
	 * If the specification decides on one of the two wirings, only the
	 * corresponding value is to be accepted for text.
	 */
	@Test
	public void testImportOutsideTheConflict() throws Exception {
		String c = getName() + ".c";
		String s = getName() + ".s";
		Bundle preferred = installBundle(c, PACKAGES, null);
		Bundle other = installBundle(s,
				PACKAGES + ",functions;version=1.0;uses:=\"collections\"",
				null);
		Bundle importer = installBundle(getName() + ".a", null,
				"text;version=\"[1.0,2.0)\",collections;version=\"[1.0,2.0)\","
						+ "functions;version=\"[1.0,2.0)\"");

		assertThat(preferred.getBundleId()).isLessThan(other.getBundleId());
		assertThat(frameworkWiring.resolveBundles(
				Arrays.asList(preferred, other, importer)))
						.as("the bundles resolve").isTrue();

		assertThat(providerOf(importer, "functions")).isEqualTo(s);
		assertThat(providerOf(importer, "collections"))
				.as("collections must match the uses constraint of functions")
				.isEqualTo(s);
		assertThat(providerOf(importer, "text"))
				.as("text stays with the preferred exporter or moves with the"
						+ " other imports")
				.isIn(c, s);
	}

	/**
	 * Without the second exporter offering functions there is no conflict and
	 * the preference order applies to every import.
	 */
	@Test
	public void testPreferenceWithoutConflict() throws Exception {
		String c = getName() + ".c";
		String s = getName() + ".s";
		Bundle preferred = installBundle(c, PACKAGES, null);
		Bundle other = installBundle(s, PACKAGES, null);
		Bundle importer = installBundle(getName() + ".a", null,
				"text;version=\"[1.0,2.0)\",collections;version=\"[1.0,2.0)\"");

		assertThat(frameworkWiring.resolveBundles(
				Arrays.asList(preferred, other, importer)))
						.as("the bundles resolve").isTrue();
		assertThat(providerOf(importer, "text")).isEqualTo(c);
		assertThat(providerOf(importer, "collections")).isEqualTo(c);
	}
}
