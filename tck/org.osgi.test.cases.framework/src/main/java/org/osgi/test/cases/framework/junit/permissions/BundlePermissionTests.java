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

package org.osgi.test.cases.framework.junit.permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundlePermission;

public class BundlePermissionTests {

	@Test
	public void testInvalid() {
		invalidBundlePermission("a.b.c", "x");
		invalidBundlePermission("a.b.c", "   host  ,  x   ");
		invalidBundlePermission("a.b.c", "");
		invalidBundlePermission("a.b.c", "      ");
		invalidBundlePermission("a.b.c", null);
		invalidBundlePermission("a.b.c", ",");
		invalidBundlePermission("a.b.c", ",xxx");
		invalidBundlePermission("a.b.c", "xxx,");
		invalidBundlePermission("a.b.c", "provide,");
		invalidBundlePermission("a.b.c", "require,   ");
		invalidBundlePermission("a.b.c", "hostme,");
		invalidBundlePermission("a.b.c", "fragmentme,");
		invalidBundlePermission("a.b.c", ",host");
		invalidBundlePermission("a.b.c", ",fragment");
		invalidBundlePermission("a.b.c", "   provideme   ");
		invalidBundlePermission("a.b.c", "   requireme     ");
		invalidBundlePermission("a.b.c", "   hos");
		invalidBundlePermission("a.b.c", "   fragmen"); 
	}

	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read"); 

		BundlePermission p11 = new BundlePermission("com.foo.service1",
				"    REQUIRE,provide   ");
		BundlePermission p12 = new BundlePermission("com.foo.service1",
				"PROVIDE  ,   require");
		BundlePermission p13 = new BundlePermission("com.foo.service1",
				"proVIDE   ");
		BundlePermission p14 = new BundlePermission("com.foo.service1",
				"    Require    "); 
		BundlePermission p15 = new BundlePermission("com.foo.service1",
				"    fraGMent    ");
		BundlePermission p16 = new BundlePermission("com.foo.service1",
				"    HosT    "); 

		assertThat(p11).implies(p11);
		assertThat(p11).implies(p12);
		assertThat(p11).implies(p13);
		assertThat(p11).implies(p14);
		assertThat(p11).doesNotImply(p15);
		assertThat(p11).doesNotImply(p16);

		assertThat(p12).implies(p11);
		assertThat(p12).implies(p12);
		assertThat(p12).implies(p13);
		assertThat(p12).implies(p14);
		assertThat(p12).doesNotImply(p15);
		assertThat(p12).doesNotImply(p16);

		assertThat(p13).implies(p11);
		assertThat(p13).implies(p12);
		assertThat(p13).implies(p13);
		assertThat(p13).implies(p14);
		assertThat(p13).doesNotImply(p15);
		assertThat(p13).doesNotImply(p16);

		assertThat(p14).implies(p14);
		assertThat(p14).doesNotImply(p11);
		assertThat(p14).doesNotImply(p12);
		assertThat(p14).doesNotImply(p13);
		
		assertThat(p15).implies(p15);
		assertThat(p15).doesNotImply(p11);
		assertThat(p15).doesNotImply(p12);
		assertThat(p15).doesNotImply(p13);

		assertThat(p16).implies(p16);
		assertThat(p16).doesNotImply(p11);
		assertThat(p16).doesNotImply(p12);
		assertThat(p16).doesNotImply(p13);

		assertThat(p11).doesNotImply(op);

		assertThat(p11).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p11).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p11).isEqualTo(p13).hasSameHashCodeAs(p13);
		assertThat(p12).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p12).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p12).isEqualTo(p13).hasSameHashCodeAs(p13);
		assertThat(p13).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p13).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p13).isEqualTo(p13).hasSameHashCodeAs(p13);
		assertThat(p14).isEqualTo(p14).hasSameHashCodeAs(p14);
		assertThat(p15).isEqualTo(p15).hasSameHashCodeAs(p15);
		assertThat(p16).isEqualTo(p16).hasSameHashCodeAs(p16);

		assertThat(p11).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p12).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p13).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p13).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);
		assertThat(p13).isNotEqualTo(p16).doesNotHaveSameHashCodeAs(p16);
		assertThat(p14).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);
		assertThat(p14).isNotEqualTo(p12).doesNotHaveSameHashCodeAs(p12);
		assertThat(p14).isNotEqualTo(p13).doesNotHaveSameHashCodeAs(p13);
		assertThat(p14).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);
		assertThat(p14).isNotEqualTo(p16).doesNotHaveSameHashCodeAs(p16);

		PermissionCollection pc = p13.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p11);

		assertThat(pc).accepts(p14);
		assertThat(pc).implies(p14);
		assertThat(pc).doesNotImply(p11);
		assertThat(pc).doesNotImply(p12);
		assertThat(pc).doesNotImply(p13);

		assertThat(pc).accepts(p13);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);

		assertThat(pc).rejects(op);
		assertThat(pc).isSerializable();

		pc = p13.newPermissionCollection();

		assertThat(pc).accepts(p13);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).isSerializable();

		pc = p11.newPermissionCollection();

		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);

		pc.setReadOnly();

		assertThat(pc).rejects(p12);

		assertThat(pc).hasElements();

		assertThat(p11).isSerializable();
		assertThat(p12).isSerializable();
		assertThat(p13).isSerializable();
		assertThat(p14).isSerializable();
		assertThat(p15).isSerializable();
		assertThat(p16).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testNames() {
		BundlePermission p21 = new BundlePermission("com.foo.service2",
				"host");
		BundlePermission p22 = new BundlePermission("com.foo.*", "host");
		BundlePermission p23 = new BundlePermission("com.*", "host");
		BundlePermission p24 = new BundlePermission("org.foo.service2", "host");
		BundlePermission p25 = new BundlePermission("*", "host"); 

		assertThat(p21).implies(p21);
		assertThat(p22).implies(p21);
		assertThat(p23).implies(p21);
		assertThat(p24).doesNotImply(p21);
		assertThat(p25).implies(p21);

		assertThat(p22).implies(p22);
		assertThat(p23).implies(p22);
		assertThat(p24).doesNotImply(p22);
		assertThat(p25).implies(p22);

		assertThat(p23).implies(p23);
		assertThat(p24).doesNotImply(p23);
		assertThat(p25).implies(p23);

		assertThat(p24).doesNotImply(p25);
		assertThat(p25).implies(p25);

		assertThat(p25).implies(p24);

		assertThat(p21).doesNotImply(p22);
		assertThat(p21).doesNotImply(p23);
		assertThat(p21).doesNotImply(p24);
		assertThat(p21).doesNotImply(p25);

		assertThat(p22).doesNotImply(p23);
		assertThat(p22).doesNotImply(p24);
		assertThat(p22).doesNotImply(p25);

		assertThat(p23).doesNotImply(p24);
		assertThat(p23).doesNotImply(p25);

		PermissionCollection pc = p21.newPermissionCollection();

		assertThat(pc).accepts(p21);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).doesNotImply(p25);

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).doesNotImply(p25);

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).doesNotImply(p25);

		assertThat(pc).accepts(p24);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).doesNotImply(p25);

		assertThat(pc).accepts(p25);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p25);
		assertThat(pc).implies(p24);
		assertThat(pc).isSerializable();

		pc = p22.newPermissionCollection();

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).doesNotImply(p25);
		assertThat(pc).isSerializable();

		pc = p23.newPermissionCollection();

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).doesNotImply(p25);
		assertThat(pc).isSerializable();

		pc = p24.newPermissionCollection();

		assertThat(pc).accepts(p24);
		assertThat(pc).doesNotImply(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).doesNotImply(p25);
		assertThat(pc).isSerializable();

		pc = p25.newPermissionCollection();
		
		assertThat(pc).accepts(p25);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).implies(p25);

		assertThat(p21).isSerializable();
		assertThat(p22).isSerializable();
		assertThat(p23).isSerializable();
		assertThat(p24).isSerializable();
		assertThat(p25).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testActionImplications() {
		BundlePermission provide = new BundlePermission("*", "provide");
		BundlePermission require = new BundlePermission("*", "require");
		BundlePermission host = new BundlePermission("*", "host");
		BundlePermission fragment = new BundlePermission("*", "fragment");

		assertThat(provide).implies(provide);
		assertThat(provide).implies(require);
		assertThat(provide).doesNotImply(host);
		assertThat(provide).doesNotImply(fragment);
		
		assertThat(require).doesNotImply(provide);
		assertThat(require).implies(require);
		assertThat(require).doesNotImply(host);
		assertThat(require).doesNotImply(fragment);

		assertThat(host).doesNotImply(provide);
		assertThat(host).doesNotImply(require);
		assertThat(host).implies(host);
		assertThat(host).doesNotImply(fragment);

		assertThat(fragment).doesNotImply(provide);
		assertThat(fragment).doesNotImply(require);
		assertThat(fragment).doesNotImply(host);
		assertThat(fragment).implies(fragment);
		
		BundlePermission comfoohost = new BundlePermission("com.foo.*", "host");
		BundlePermission comfoobarfragment = new BundlePermission(
				"com.foo.bar.*", "fragment");
		BundlePermission all = new BundlePermission("com.foo.bar.baz",
				"provide,require,host,fragment");
		PermissionCollection pc = provide.newPermissionCollection();
		assertThat(pc).accepts(provide);
		assertThat(pc).accepts(comfoohost);
		assertThat(pc).accepts(comfoobarfragment);
		assertThat(pc).implies(all);
	}
	
	private static void invalidBundlePermission(String name, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new BundlePermission(name, actions));
	}
}
