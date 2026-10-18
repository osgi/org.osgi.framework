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
import static org.osgi.test.cases.framework.junit.permissions.PermissionMocks.newMockBundle;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.Bundle;

/**
 */
public class AdaptPermissionTests {

	/**
	 * 
	 */
	@Test
	public void testInvalid() {
		invalidAdaptPermission("(a=b)", "x");
		invalidAdaptPermission("(a=b)", "   adapt  ,  x   ");
		invalidAdaptPermission("(a=b)", "");
		invalidAdaptPermission("(a=b)", "      ");
		invalidAdaptPermission("(a=b)", null);
		invalidAdaptPermission("(a=b)", ",");
		invalidAdaptPermission("(a=b)", ",xxx");
		invalidAdaptPermission("(a=b)", "xxx,");
		invalidAdaptPermission("(a=b)", "adapt,");
		invalidAdaptPermission("(a=b)", "adaptme,");
		invalidAdaptPermission("(a=b)", ",adapt");
		invalidAdaptPermission("(a=b)", "   adaptme   ");
		invalidAdaptPermission("(a=b)", "   adap");
		invalidAdaptPermission("a.b.c", "adapt");
		invalidAdaptPermission("()", "adapt");
		invalidAdaptPermission(null, "adapt");

		invalidAdaptPermission("(adaptClass=a.b.c)", null, "adapt");
		invalidAdaptPermission(null,
				newMockBundle(2, "test.bsn", "test.location", null), "adapt");
	}

	/**
	 * 
	 */
	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read");
		Bundle b1 = newMockBundle(2, "test.bsn", "test.location", null);

		AdaptPermission p11 = new AdaptPermission(
				"(adaptClass=com.foo.coord1)", "    adapt   ");
		AdaptPermission p12 = new AdaptPermission(
				"(adaptClass=com.foo.coord1)", "ADAPT   ");

		AdaptPermission p21 = new AdaptPermission("com.foo.coord1", b1,
				"    adapt   ");
		AdaptPermission p22 = new AdaptPermission("com.foo.coord1", b1,
				"adAPT   ");

		assertThat(p11.getActions()).isEqualTo("adapt");
		assertThat(p21.getActions()).isEqualTo("adapt");
		assertThat(p12.getActions()).isEqualTo("adapt");
		assertThat(p22.getActions()).isEqualTo("adapt");

		assertThat(p11).implies(p21);
		assertThat(p11).implies(p22);

		assertThat(p12).implies(p21);
		assertThat(p12).implies(p22);

		assertThat(p11).doesNotImply(p11);
		assertThat(p11).doesNotImply(p12);
		assertThat(p12).doesNotImply(p11);
		assertThat(p12).doesNotImply(p12);

		assertThat(p21).doesNotImply(p11);
		assertThat(p21).doesNotImply(p12);
		assertThat(p21).doesNotImply(p21);
		assertThat(p21).doesNotImply(p22);

		assertThat(p22).doesNotImply(p11);
		assertThat(p22).doesNotImply(p12);
		assertThat(p22).doesNotImply(p21);
		assertThat(p22).doesNotImply(p22);

		assertThat(p11).doesNotImply(op);

		assertThat(p11).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p11).isEqualTo(p12).hasSameHashCodeAs(p12);

		assertThat(p12).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p12).isEqualTo(p12).hasSameHashCodeAs(p12);

		assertThat(p21).isEqualTo(p21).hasSameHashCodeAs(p21);
		assertThat(p21).isEqualTo(p22).hasSameHashCodeAs(p22);

		assertThat(p22).isEqualTo(p21).hasSameHashCodeAs(p21);
		assertThat(p22).isEqualTo(p22).hasSameHashCodeAs(p22);

		PermissionCollection pc = p11.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p22);

		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);

		assertThat(pc).isSerializable();

		pc.setReadOnly();

		assertThat(pc).rejects(p12);

		assertThat(pc).hasElements();

		assertThat(p11).isSerializable();
		assertThat(p12).isSerializable();
		assertThat(p21).isNotSerializable();
		assertThat(p22).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testWildcard() {
		Bundle b1 = newMockBundle(2, "test.bsn", "test.location", null);
		AdaptPermission p21 = new AdaptPermission("(adaptClass=com.foo.*)",
				"adapt");
		AdaptPermission p22 = new AdaptPermission("com.foo.coord2", b1, "adapt");
		AdaptPermission p23 = new AdaptPermission("*", "adapt");
		AdaptPermission p24 = new AdaptPermission("com.bar.coord2", b1, "adapt");

		assertThat(p21).implies(p22);
		assertThat(p21).doesNotImply(p23);
		assertThat(p21).doesNotImply(p24);
		assertThat(p23).implies(p22);
		assertThat(p23).implies(p24);
		assertThat(p23).implies(p23);

		PermissionCollection pc = p21.newPermissionCollection();

		assertThat(pc).accepts(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		assertThat(pc).isSerializable();

		pc = p23.newPermissionCollection();

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).isSerializable();

		assertThat(p21).isSerializable();
		assertThat(p22).isNotSerializable();
		assertThat(p23).isSerializable();
		assertThat(p24).isNotSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testActionImplications() {
		AdaptPermission adapt = new AdaptPermission("*", "adapt");

		assertThat(adapt.getActions()).isEqualTo("adapt");
		adapt = new AdaptPermission(adapt.getName(), adapt.getActions());
		assertThat(adapt.getActions()).isEqualTo("adapt");

		PermissionCollection adaptpc = adapt.newPermissionCollection();
		adaptpc.add(adapt);

		assertThat(adapt).implies(adapt);
		assertThat(adaptpc).implies(adapt);
	}

	/**
	 * 
	 */
	@Test
	public void testFiltersName() {
		Bundle b1 = newMockBundle(2, "test.bsn", "test.location", null);
		AdaptPermission p31 = new AdaptPermission(
				"  (adaptClass  =com.foo.coord3)", "adapt");
		AdaptPermission p32 = new AdaptPermission("(adaptClass=com.foo.*)",
				"adapt");
		AdaptPermission p33 = new AdaptPermission("(adaptClass=com.*)", "adapt");
		AdaptPermission p34 = new AdaptPermission("(adaptClass=*)", "adapt");
		AdaptPermission p35 = new AdaptPermission("com.foo.coord3", b1, "adapt");
		AdaptPermission p38 = new AdaptPermission("*", "adapt");

		assertThat(p31).implies(p35);
		assertThat(p32).implies(p35);
		assertThat(p33).implies(p35);
		assertThat(p34).implies(p35);
		assertThat(p38).implies(p35);

		assertThat(p31).doesNotImply(p31);
		assertThat(p31).doesNotImply(p32);
		assertThat(p31).doesNotImply(p33);
		assertThat(p31).doesNotImply(p34);

		assertThat(p31).doesNotImply(p38);
		assertThat(p32).doesNotImply(p38);
		assertThat(p33).doesNotImply(p38);
		assertThat(p34).implies(p38);
		assertThat(p35).doesNotImply(p38);
		assertThat(p38).implies(p38);

		PermissionCollection pc = p31.newPermissionCollection();
		assertThat(pc).hasNoElements();

		assertThat(pc).accepts(p31);

		assertThat(pc).doesNotImply(p31);
		assertThat(pc).doesNotImply(p32);
		assertThat(pc).doesNotImply(p33);
		assertThat(pc).doesNotImply(p34);

		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p32);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p33);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p34);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p38);

		assertThat(pc).hasElements();
		assertThat(pc).isSerializable();

		pc = p32.newPermissionCollection();
		assertThat(pc).accepts(p32);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);
		assertThat(pc).isSerializable();

		pc = p33.newPermissionCollection();
		assertThat(pc).accepts(p33);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);
		assertThat(pc).isSerializable();

		pc = p34.newPermissionCollection();
		assertThat(pc).accepts(p34);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p38);

		assertThat(p31).isSerializable();
		assertThat(p32).isSerializable();
		assertThat(p33).isSerializable();
		assertThat(p34).isSerializable();
		assertThat(p35).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testFiltersBundle() {
		AdaptPermission p41 = new AdaptPermission("(id=2)", "adapt");
		AdaptPermission p42 = new AdaptPermission("(location=test.*)", "adapt");
		AdaptPermission p43 = new AdaptPermission("(name=test.*)", "adapt");
		AdaptPermission p44 = new AdaptPermission("(signer=\\*, o=ACME, c=US)",
				"adapt");
		AdaptPermission p45 = new AdaptPermission("(adaptClass=com.foo.*)",
				"adapt");

		AdaptPermission p46 = new AdaptPermission("com.foo.coord4",
				newMockBundle(2, "test.bsn", "test.location",
						"cn=Bugs Bunny, o=ACME, c=US"), "adapt");
		AdaptPermission p47 = new AdaptPermission("com.bar.coord4",
				newMockBundle(3, "not.bsn", "not.location",
						"cn=Bugs Bunny, o=NOT, c=US"), "adapt");

		assertThat(p41).implies(p46);
		assertThat(p42).implies(p46);
		assertThat(p43).implies(p46);
		assertThat(p44).implies(p46);
		assertThat(p45).implies(p46);

		assertThat(p41).doesNotImply(p47);
		assertThat(p42).doesNotImply(p47);
		assertThat(p43).doesNotImply(p47);
		assertThat(p44).doesNotImply(p47);
		assertThat(p45).doesNotImply(p47);

		assertThat(p41).doesNotImply(p41);
		assertThat(p41).doesNotImply(p42);
		assertThat(p41).doesNotImply(p43);
		assertThat(p41).doesNotImply(p44);
		assertThat(p41).doesNotImply(p45);

		PermissionCollection pc = p41.newPermissionCollection();
		assertThat(pc).hasNoElements();

		assertThat(pc).accepts(p44);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);

		assertThat(pc).doesNotImply(p41);
		assertThat(pc).doesNotImply(p42);
		assertThat(pc).doesNotImply(p43);
		assertThat(pc).doesNotImply(p44);
		assertThat(pc).doesNotImply(p45);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p43);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p42);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p41);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);
		assertThat(pc).hasElements();

		// throw IllegalArgumentException If specified permission to be
		// added was constructed with bundles.
		assertThat(pc).rejects(p46);
		assertThat(pc).rejects(p47);

		assertThat(p41).isSerializable();
		assertThat(p42).isSerializable();
		assertThat(p43).isSerializable();
		assertThat(p44).isSerializable();
		assertThat(p45).isSerializable();
		assertThat(p46).isNotSerializable();
		assertThat(p47).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testPermissionCollection() {
		AdaptPermission p51 = new AdaptPermission(
				"  (adaptClass  =com.foo.coord5)", "adapt");
		AdaptPermission p52 = new AdaptPermission("(adaptClass=com.foo.*)",
				"adapt");
		AdaptPermission p53 = new AdaptPermission("(adaptClass=com.*)", "adapt");
		AdaptPermission p54 = new AdaptPermission("(adaptClass=*)", "adapt");
		AdaptPermission p59 = new AdaptPermission("*", "adapt");

		AdaptPermission p5a = new AdaptPermission("com.foo.coord5",
				newMockBundle(2, "test.bsn", "test.location",
						"cn=Bugs Bunny, o=ACME, c=US"), "adapt");
		AdaptPermission p5b = new AdaptPermission("com.bar.coord5",
				newMockBundle(3, "not.bsn", "not.location",
						"cn=Bugs Bunny, o=NOT, c=US"), "adapt");

		PermissionCollection pc;
		pc = p51.newPermissionCollection();
		assertThat(pc).accepts(p51);
		assertThat(p51).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p51).doesNotImply(p5b);
		assertThat(pc).doesNotImply(p5b);
		assertThat(pc).isSerializable();

		pc = p51.newPermissionCollection();
		assertThat(pc).accepts(p51);
		assertThat(pc).implies(p5a);
		assertThat(pc).doesNotImply(p5b);
		assertThat(pc).isSerializable();

		pc = p52.newPermissionCollection();
		assertThat(pc).accepts(p52);
		assertThat(p52).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p52).doesNotImply(p5b);
		assertThat(pc).doesNotImply(p5b);
		assertThat(pc).isSerializable();

		pc = p53.newPermissionCollection();
		assertThat(pc).accepts(p53);
		assertThat(p53).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p53).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();

		pc = p54.newPermissionCollection();
		assertThat(pc).accepts(p54);
		assertThat(pc).accepts(p59);
		assertThat(pc).implies(p5a);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();

		pc = p54.newPermissionCollection();
		assertThat(pc).accepts(p54);
		assertThat(p54).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p54).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();

		pc = p59.newPermissionCollection();
		assertThat(pc).accepts(p59);
		assertThat(p59).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p59).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();
	}

	private static void invalidAdaptPermission(String name, String actions) {
		assertThatCode(() -> new AdaptPermission(name, actions))
				.as("AdaptPermission created with invalid arguments")
				.isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
	}

	private static void invalidAdaptPermission(String name, Bundle bundle,
			String actions) {
		assertThatCode(() -> new AdaptPermission(name, bundle, actions))
				.as("AdaptPermission created with invalid arguments")
				.isInstanceOfAny(IllegalArgumentException.class, NullPointerException.class);
	}
}
