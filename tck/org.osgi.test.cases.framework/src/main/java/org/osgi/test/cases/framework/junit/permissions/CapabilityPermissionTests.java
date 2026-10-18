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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.CapabilityPermission;

public class CapabilityPermissionTests {

	@Test
	public void testInvalid() {
		invalidCapabilityPermission("a.b.c", "x");
		invalidCapabilityPermission("a.b.c", "   require  ,  x   ");
		invalidCapabilityPermission("a.b.c", "");
		invalidCapabilityPermission("a.b.c", "      ");
		invalidCapabilityPermission("a.b.c", null);
		invalidCapabilityPermission("a.b.c", ",");
		invalidCapabilityPermission("a.b.c", ",xxx");
		invalidCapabilityPermission("a.b.c", "xxx,");
		invalidCapabilityPermission("a.b.c", "require,");
		invalidCapabilityPermission("a.b.c", "provide,   ");
		invalidCapabilityPermission("a.b.c", "requireme,");
		invalidCapabilityPermission("a.b.c", "provideme,");
		invalidCapabilityPermission("a.b.c", ",require");
		invalidCapabilityPermission("a.b.c", ",provide");
		invalidCapabilityPermission("a.b.c", "   requireme   ");
		invalidCapabilityPermission("a.b.c", "   provideme     ");
		invalidCapabilityPermission("a.b.c", "   requir");
		invalidCapabilityPermission("a.b.c", "   provid");
		invalidCapabilityPermission("()", "require");
		invalidCapabilityPermission("(capability.namespace=a.b.c)",
				"provide,REQUIRE");
		invalidCapabilityPermission("(capability.namespace=a.b.c)", "provide");
		invalidCapabilityPermission("(capability.namespace=a.b.c)",
				"requiRE,provide");

		invalidCapabilityPermission("(capability.namespace=a.b.c)", null, null,
				"require");
		invalidCapabilityPermission("a.b.c", null, null, "require");
		invalidCapabilityPermission("(capability.namespace=a.b.c)",
				Collections.<String, Object> emptyMap(), null, "require");
		invalidCapabilityPermission("a.b.c",
				Collections.<String, Object> emptyMap(), null,
				"require");
		invalidCapabilityPermission("a.b.c",
				Collections.<String, Object> emptyMap(),
				newMockBundle(2, "test.bsn", "test.location", null), "provide");
		invalidCapabilityPermission("a.b.c",
				Collections.<String, Object> emptyMap(),
				newMockBundle(2, "test.bsn", "test.location", null),
				"require, provide");
	}

	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read");

		CapabilityPermission p11 = new CapabilityPermission(
				"com.foo.capability1",
				"    REQUIRE,provide   ");
		CapabilityPermission p12 = new CapabilityPermission(
				"com.foo.capability1",
				"PROVIDE  ,   require");
		CapabilityPermission p13 = new CapabilityPermission(
				"com.foo.capability1",
				"provIDE   ");
		CapabilityPermission p14 = new CapabilityPermission(
				"com.foo.capability1",
				"    Require    ");

		assertThat(p11.getActions()).isEqualTo("require,provide");
		assertThat(p12.getActions()).isEqualTo("require,provide");
		assertThat(p13.getActions()).isEqualTo("provide");
		assertThat(p14.getActions()).isEqualTo("require");

		assertThat(p11).implies(p11);
		assertThat(p11).implies(p12);
		assertThat(p11).implies(p13);
		assertThat(p11).implies(p14);

		assertThat(p14).implies(p14);

		assertThat(p14).doesNotImply(p11);
		assertThat(p14).doesNotImply(p12);
		assertThat(p14).doesNotImply(p13);

		assertThat(p11).doesNotImply(op);

		assertThat(p11).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p11).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p12).isEqualTo(p11).hasSameHashCodeAs(p11);

		assertThat(p11).isNotEqualTo(p13).doesNotHaveSameHashCodeAs(p13);
		assertThat(p11).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);

		assertThat(p13).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);

		assertThat(p14).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);

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
		assertThat(pc).isSerializable();

		pc = p13.newPermissionCollection();
		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);

		assertThat(pc).rejects(op);
		assertThat(pc).isSerializable();

		pc.setReadOnly();

		assertThat(pc).rejects(p12);

		assertThat(pc).hasElements();

		assertThat(p11).isSerializable();
		assertThat(p12).isSerializable();
		assertThat(p13).isSerializable();
		assertThat(p14).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testNames() {
		CapabilityPermission p21 = new CapabilityPermission(
				"com.foo.capability2",
				"require");
		CapabilityPermission p22 = new CapabilityPermission("com.foo.*",
				"require");
		CapabilityPermission p23 = new CapabilityPermission("com.*", "require");
		CapabilityPermission p24 = new CapabilityPermission("*", "require");

		assertThat(p21).implies(p21);
		assertThat(p22).implies(p21);
		assertThat(p23).implies(p21);
		assertThat(p24).implies(p21);

		assertThat(p22).implies(p22);
		assertThat(p23).implies(p22);
		assertThat(p24).implies(p22);

		assertThat(p23).implies(p23);
		assertThat(p24).implies(p23);

		assertThat(p24).implies(p24);

		assertThat(p21).doesNotImply(p22);
		assertThat(p21).doesNotImply(p23);
		assertThat(p21).doesNotImply(p24);

		assertThat(p22).doesNotImply(p23);
		assertThat(p22).doesNotImply(p24);

		assertThat(p23).doesNotImply(p24);

		PermissionCollection pc = p21.newPermissionCollection();

		assertThat(pc).accepts(p21);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p24);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).isSerializable();

		pc = p22.newPermissionCollection();

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).isSerializable();

		pc = p23.newPermissionCollection();

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).isSerializable();

		pc = p24.newPermissionCollection();

		assertThat(pc).accepts(p24);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		assertThat(p21).isSerializable();
		assertThat(p22).isSerializable();
		assertThat(p23).isSerializable();
		assertThat(p24).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testActionImplications() {
		CapabilityPermission provide = new CapabilityPermission("*", "provide");
		CapabilityPermission require = new CapabilityPermission("*", "require");

		assertThat(provide.getActions()).isEqualTo("provide");
		assertThat(require.getActions()).isEqualTo("require");

		assertThat(require).implies(require);
		assertThat(provide).implies(provide);

		assertThat(require).doesNotImply(provide);

		assertThat(provide).doesNotImply(require);

		CapabilityPermission comfooexport = new CapabilityPermission(
				"com.foo.*", "provide");
		CapabilityPermission both = new CapabilityPermission("com.foo.bar",
				"require,provide");
		PermissionCollection pc = require.newPermissionCollection();
		assertThat(pc).accepts(require);
		assertThat(pc).accepts(comfooexport);
		assertThat(pc).implies(both);
	}

	@Test
	public void testFiltersName() {
		CapabilityPermission p31 = new CapabilityPermission(
				"  (capability.namespace  =com.foo.capability2)", "require");
		CapabilityPermission p32 = new CapabilityPermission(
				"(capability.namespace=com.foo.*)", "require");
		CapabilityPermission p33 = new CapabilityPermission(
				"(capability.namespace=com.*)", "require");
		CapabilityPermission p34 = new CapabilityPermission(
				"(capability.namespace=*)",
				"require");
		CapabilityPermission p35 = new CapabilityPermission(
				"com.foo.capability2",
				"require");
		CapabilityPermission p36 = new CapabilityPermission("com.foo.*",
				"require");
		CapabilityPermission p37 = new CapabilityPermission("com.*", "require");
		CapabilityPermission p38 = new CapabilityPermission("*", "require");

		assertThat(p31).implies(p35);
		assertThat(p32).implies(p35);
		assertThat(p33).implies(p35);
		assertThat(p34).implies(p35);
		assertThat(p36).implies(p35);
		assertThat(p37).implies(p35);
		assertThat(p38).implies(p35);

		assertThat(p31).doesNotImply(p31);
		assertThat(p31).doesNotImply(p32);
		assertThat(p31).doesNotImply(p33);
		assertThat(p31).doesNotImply(p34);

		assertThat(p31).doesNotImply(p36);
		assertThat(p32).implies(p36);
		assertThat(p33).implies(p36);
		assertThat(p34).implies(p36);
		assertThat(p35).doesNotImply(p36);
		assertThat(p36).implies(p36);
		assertThat(p37).implies(p36);
		assertThat(p38).implies(p36);

		assertThat(p31).doesNotImply(p37);
		assertThat(p32).doesNotImply(p37);
		assertThat(p33).implies(p37);
		assertThat(p34).implies(p37);
		assertThat(p35).doesNotImply(p37);
		assertThat(p36).doesNotImply(p37);
		assertThat(p37).implies(p37);
		assertThat(p38).implies(p37);

		assertThat(p31).doesNotImply(p38);
		assertThat(p32).doesNotImply(p38);
		assertThat(p33).doesNotImply(p38);
		assertThat(p34).implies(p38);
		assertThat(p35).doesNotImply(p38);
		assertThat(p36).doesNotImply(p38);
		assertThat(p37).doesNotImply(p38);
		assertThat(p38).implies(p38);

		PermissionCollection pc = p31.newPermissionCollection();
		assertThat(pc).hasNoElements();

		assertThat(pc).accepts(p31);

		assertThat(pc).doesNotImply(p31);
		assertThat(pc).doesNotImply(p32);
		assertThat(pc).doesNotImply(p33);
		assertThat(pc).doesNotImply(p34);

		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p36);
		assertThat(pc).doesNotImply(p37);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p32);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p36);
		assertThat(pc).doesNotImply(p37);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p33);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p36);
		assertThat(pc).implies(p37);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p34);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p36);
		assertThat(pc).implies(p37);
		assertThat(pc).implies(p38);

		assertThat(pc).hasElements();
		assertThat(pc).isSerializable();

		pc = p32.newPermissionCollection();
		assertThat(pc).accepts(p32);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p36);
		assertThat(pc).doesNotImply(p37);
		assertThat(pc).doesNotImply(p38);
		assertThat(pc).isSerializable();

		pc = p33.newPermissionCollection();
		assertThat(pc).accepts(p33);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p36);
		assertThat(pc).implies(p37);
		assertThat(pc).doesNotImply(p38);
		assertThat(pc).isSerializable();

		pc = p34.newPermissionCollection();
		assertThat(pc).accepts(p34);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p36);
		assertThat(pc).implies(p37);
		assertThat(pc).implies(p38);

		assertThat(p31).isSerializable();
		assertThat(p32).isSerializable();
		assertThat(p33).isSerializable();
		assertThat(p34).isSerializable();
		assertThat(p35).isSerializable();
		assertThat(p36).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testFiltersAttributesBundle() {
		CapabilityPermission p41 = new CapabilityPermission("(id=2)", "require");
		CapabilityPermission p42 = new CapabilityPermission(
				"(location=test.*)", "require");
		CapabilityPermission p43 = new CapabilityPermission("(name=test.*)",
				"require");
		CapabilityPermission p44 = new CapabilityPermission(
				"(signer=\\*, o=ACME, c=US)", "require");
		CapabilityPermission p45 = new CapabilityPermission(
				"(capability.namespace=com.foo.*)", "require");
		CapabilityPermission p46 = new CapabilityPermission("(attr1=foo*)",
				"require");

		Map<String,Object> attributes = new HashMap<>();
		attributes.put("attr1", "foo1");
		CapabilityPermission p47 = new CapabilityPermission(
				"com.foo.capability2", attributes,
				newMockBundle(2, "test.bsn", "test.location",
						"cn=Bugs Bunny, o=ACME, c=US"), "require");

		attributes.put("attr1", "bar1");
		CapabilityPermission p48 = new CapabilityPermission(
				"com.bar.capability2", attributes,
				newMockBundle(3, "not.bsn", "not.location",
						"cn=Bugs Bunny, o=NOT, c=US"), "require");

		assertThat(p41).implies(p47);
		assertThat(p42).implies(p47);
		assertThat(p43).implies(p47);
		assertThat(p44).implies(p47);
		assertThat(p45).implies(p47);
		assertThat(p46).implies(p47);

		assertThat(p41).doesNotImply(p48);
		assertThat(p42).doesNotImply(p48);
		assertThat(p43).doesNotImply(p48);
		assertThat(p44).doesNotImply(p48);
		assertThat(p45).doesNotImply(p48);
		assertThat(p46).doesNotImply(p48);

		assertThat(p41).doesNotImply(p41);
		assertThat(p41).doesNotImply(p42);
		assertThat(p41).doesNotImply(p43);
		assertThat(p41).doesNotImply(p44);
		assertThat(p41).doesNotImply(p45);
		assertThat(p41).doesNotImply(p46);

		PermissionCollection pc = p41.newPermissionCollection();
		assertThat(pc).hasNoElements();

		assertThat(pc).accepts(p44);
		assertThat(pc).implies(p47);
		assertThat(pc).doesNotImply(p48);

		assertThat(pc).doesNotImply(p41);
		assertThat(pc).doesNotImply(p42);
		assertThat(pc).doesNotImply(p43);
		assertThat(pc).doesNotImply(p44);
		assertThat(pc).doesNotImply(p45);
		assertThat(pc).doesNotImply(p46);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p46);
		assertThat(pc).implies(p47);
		assertThat(pc).doesNotImply(p48);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p45);
		assertThat(pc).implies(p47);
		assertThat(pc).doesNotImply(p48);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p43);
		assertThat(pc).implies(p47);
		assertThat(pc).doesNotImply(p48);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p42);
		assertThat(pc).implies(p47);
		assertThat(pc).doesNotImply(p48);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p41);
		assertThat(pc).implies(p47);
		assertThat(pc).doesNotImply(p48);
		assertThat(pc).hasElements();

		// throw IllegalArgumentException If specified permission to be
		// added was constructed with bundles.
		assertThat(pc).rejects(p47);
		assertThat(pc).rejects(p48);

		assertThat(p41).isSerializable();
		assertThat(p42).isSerializable();
		assertThat(p43).isSerializable();
		assertThat(p44).isSerializable();
		assertThat(p45).isSerializable();
		assertThat(p46).isSerializable();
		assertThat(p47).isNotSerializable();
		assertThat(p48).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testPermissionCollection() {
		CapabilityPermission p51 = new CapabilityPermission(
				"  (capability.namespace  =com.foo.capability2)", "require");
		CapabilityPermission p52 = new CapabilityPermission(
				"(capability.namespace=com.foo.*)", "require");
		CapabilityPermission p53 = new CapabilityPermission(
				"(capability.namespace=com.*)", "require");
		CapabilityPermission p54 = new CapabilityPermission(
				"(capability.namespace=*)",
				"require");
		CapabilityPermission p55 = new CapabilityPermission(
				"com.foo.capability2",
				"require");

		CapabilityPermission p56 = new CapabilityPermission(
				"com.bar.capability2",
				"require");
		CapabilityPermission p57 = new CapabilityPermission("com.bar.*",
				"require");
		CapabilityPermission p58 = new CapabilityPermission("com.*", "require");
		CapabilityPermission p59 = new CapabilityPermission("*", "require");

		CapabilityPermission p5a = new CapabilityPermission(
				"com.foo.capability2", Collections.<String, Object> emptyMap(),
				newMockBundle(2, "test.bsn", "test.location",
						"cn=Bugs Bunny, o=ACME, c=US"), "require");
		CapabilityPermission p5b = new CapabilityPermission(
				"com.bar.capability2", Collections.<String, Object> emptyMap(),
				newMockBundle(3, "not.bsn", "not.location",
						"cn=Bugs Bunny, o=NOT, c=US"), "require");

		PermissionCollection pc;
		pc = p51.newPermissionCollection();
		assertThat(pc).accepts(p51);
		assertThat(p51).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p51).implies(p55);
		assertThat(pc).implies(p55);
		assertThat(p51).doesNotImply(p5b);
		assertThat(pc).doesNotImply(p5b);
		assertThat(p51).doesNotImply(p56);
		assertThat(pc).doesNotImply(p56);
		assertThat(pc).isSerializable();

		pc = p56.newPermissionCollection();
		assertThat(pc).accepts(p56);
		assertThat(p56).doesNotImply(p5a);
		assertThat(pc).doesNotImply(p5a);
		assertThat(p56).doesNotImply(p55);
		assertThat(pc).doesNotImply(p55);
		assertThat(p56).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(p56).implies(p56);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p51.newPermissionCollection();
		assertThat(pc).accepts(p51);
		assertThat(pc).accepts(p56);
		assertThat(pc).implies(p5a);
		assertThat(pc).implies(p55);
		assertThat(pc).implies(p5b);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p52.newPermissionCollection();
		assertThat(pc).accepts(p52);
		assertThat(pc).accepts(p57);
		assertThat(pc).implies(p5a);
		assertThat(pc).implies(p55);
		assertThat(pc).implies(p5b);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p52.newPermissionCollection();
		assertThat(pc).accepts(p52);
		assertThat(p52).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p52).implies(p55);
		assertThat(pc).implies(p55);
		assertThat(p52).doesNotImply(p5b);
		assertThat(pc).doesNotImply(p5b);
		assertThat(p52).doesNotImply(p56);
		assertThat(pc).doesNotImply(p56);
		assertThat(pc).isSerializable();

		pc = p57.newPermissionCollection();
		assertThat(pc).accepts(p57);
		assertThat(p57).doesNotImply(p5a);
		assertThat(pc).doesNotImply(p5a);
		assertThat(p57).doesNotImply(p55);
		assertThat(pc).doesNotImply(p55);
		assertThat(p57).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(p57).implies(p56);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p53.newPermissionCollection();
		assertThat(pc).accepts(p53);
		assertThat(pc).accepts(p58);
		assertThat(pc).implies(p5a);
		assertThat(pc).implies(p55);
		assertThat(pc).implies(p5b);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p53.newPermissionCollection();
		assertThat(pc).accepts(p53);
		assertThat(p53).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p53).implies(p55);
		assertThat(pc).implies(p55);
		assertThat(p53).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(p53).implies(p56);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p58.newPermissionCollection();
		assertThat(pc).accepts(p58);
		assertThat(p58).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p58).implies(p55);
		assertThat(pc).implies(p55);
		assertThat(p58).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(p58).implies(p56);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p54.newPermissionCollection();
		assertThat(pc).accepts(p54);
		assertThat(pc).accepts(p59);
		assertThat(pc).implies(p5a);
		assertThat(pc).implies(p55);
		assertThat(pc).implies(p5b);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p54.newPermissionCollection();
		assertThat(pc).accepts(p54);
		assertThat(p54).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p54).implies(p55);
		assertThat(pc).implies(p55);
		assertThat(p54).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(p54).implies(p56);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();

		pc = p59.newPermissionCollection();
		assertThat(pc).accepts(p59);
		assertThat(p59).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p59).implies(p55);
		assertThat(pc).implies(p55);
		assertThat(p59).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(p59).implies(p56);
		assertThat(pc).implies(p56);
		assertThat(pc).isSerializable();
	}

	@Test
	public void testAttributes() {
		CapabilityPermission p61 = new CapabilityPermission(
				"(&(@@at=atat)(@name=expected)(name=test.bsn)(@capability.namespace=expected)(capability.namespace=com.foo.capability2))",
				"require");
		Bundle bundle = newMockBundle(2, "test.bsn", "test.location",
				"cn=Bugs Bunny, o=ACME, c=US");
		Map<String,Object> attributes = new HashMap<>();
		attributes.put("name", "expected");
		attributes.put("capability.namespace", "expected");
		attributes.put("@at", "atat");
		CapabilityPermission p62 = new CapabilityPermission(
				"com.foo.capability2", attributes, bundle, "require");
		assertThat(p61).implies(p62);
	}

	private static void invalidCapabilityPermission(String name, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new CapabilityPermission(name, actions));
	}

	private static void invalidCapabilityPermission(String name,
			Map<String,Object> attributes, Bundle bundle, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> {
					new CapabilityPermission(name, attributes,
							bundle, actions);
				});
	}
}
