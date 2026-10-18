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
import static org.osgi.test.cases.framework.junit.permissions.PermissionMocks.newMockServiceReference;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;

public class ServicePermissionTests {

	@Test
	public void testInvalid() {
		invalidServicePermission("a.b.c", "x");
		invalidServicePermission("a.b.c", "   get  ,  x   ");
		invalidServicePermission("a.b.c", "");
		invalidServicePermission("a.b.c", "      ");
		invalidServicePermission("a.b.c", null);
		invalidServicePermission("a.b.c", ",");
		invalidServicePermission("a.b.c", ",xxx");
		invalidServicePermission("a.b.c", "xxx,");
		invalidServicePermission("a.b.c", "get,");
		invalidServicePermission("a.b.c", "register,   ");
		invalidServicePermission("a.b.c", "getme,");
		invalidServicePermission("a.b.c", "registerme,");
		invalidServicePermission("a.b.c", ",get");
		invalidServicePermission("a.b.c", ",register");
		invalidServicePermission("a.b.c", "   getme   ");
		invalidServicePermission("a.b.c", "   registerme     ");
		invalidServicePermission("a.b.c", "   ge");
		invalidServicePermission("a.b.c", "   registe"); 
		invalidServicePermission("()", "get");
		invalidServicePermission("(objectClass=a.b.c)", "register");
		invalidServicePermission("(objectClass=a.b.c)", "register,GET");
		invalidServicePermission("(objectClass=a.b.c)", "geT,Register");

		invalidServicePermission((ServiceReference< ? >) null, "get");
		Map<String,Object> properties = new HashMap<>();
		properties.put("service.id", Long.valueOf(1));
		properties.put("objectClass", new String[] {"test.Service"});
		invalidServicePermission(newMockServiceReference(null, properties),
				"register");
		invalidServicePermission(newMockServiceReference(null, properties),
				"register,get");
	}

	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read"); 

		ServicePermission p11 = new ServicePermission("com.foo.service1",
				"    GET,register   ");
		ServicePermission p12 = new ServicePermission("com.foo.service1",
				"REGISTER  ,   get");
		ServicePermission p13 = new ServicePermission("com.foo.service1",
				"regisTER   ");
		ServicePermission p14 = new ServicePermission("com.foo.service1",
				"    Get    "); 

		assertThat(p11).implies(p11);
		assertThat(p11).implies(p12);
		assertThat(p11).implies(p13);
		assertThat(p11).implies(p14);

		assertThat(p12).implies(p11);
		assertThat(p12).implies(p12);
		assertThat(p12).implies(p13);
		assertThat(p12).implies(p14);

		assertThat(p13).implies(p13);
		assertThat(p14).implies(p14);

		assertThat(p13).doesNotImply(p11);
		assertThat(p13).doesNotImply(p12);

		assertThat(p14).doesNotImply(p11);
		assertThat(p14).doesNotImply(p12);

		assertThat(p13).doesNotImply(p14);
		assertThat(p14).doesNotImply(p13);

		assertThat(p11).doesNotImply(op);

		assertThat(p11).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p11).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p12).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p12).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p13).isEqualTo(p13).hasSameHashCodeAs(p13);
		assertThat(p14).isEqualTo(p14).hasSameHashCodeAs(p14);

		assertThat(p11).isNotEqualTo(p13).doesNotHaveSameHashCodeAs(p13);
		assertThat(p11).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p12).isNotEqualTo(p13).doesNotHaveSameHashCodeAs(p13);
		assertThat(p12).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p13).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);
		assertThat(p13).isNotEqualTo(p12).doesNotHaveSameHashCodeAs(p12);
		assertThat(p13).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p14).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);
		assertThat(p14).isNotEqualTo(p12).doesNotHaveSameHashCodeAs(p12);
		assertThat(p14).isNotEqualTo(p13).doesNotHaveSameHashCodeAs(p13);

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
		assertThat(pc).implies(p13);
		assertThat(pc).doesNotImply(p11);
		assertThat(pc).doesNotImply(p12);
		assertThat(pc).doesNotImply(p14);

		assertThat(pc).accepts(p14);
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
		assertThat(pc).isSerializable();
	}

	@Test
	public void testNames() {
		ServicePermission p21 = new ServicePermission("com.foo.service2", "get");
		ServicePermission p22 = new ServicePermission("com.foo.*", "get");
		ServicePermission p23 = new ServicePermission("com.*", "get");
		ServicePermission p24 = new ServicePermission("*", "get"); 

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
		ServicePermission register = new ServicePermission("*", "register");
		ServicePermission get = new ServicePermission("*", "get");

		assertThat(register).implies(register);
		assertThat(register).doesNotImply(get);
		assertThat(get).doesNotImply(register);
		assertThat(get).implies(get);

		ServicePermission comfooget = new ServicePermission("com.foo.*", "get");
		ServicePermission both = new ServicePermission("com.foo.Bar",
				"register,get");
		PermissionCollection pc = register.newPermissionCollection();
		assertThat(pc).accepts(register);
		assertThat(pc).accepts(comfooget);
		assertThat(pc).implies(both);
	}

	@Test
	public void testFiltersName() {
		ServicePermission p31 = new ServicePermission(
				"  (objectClass  =com.foo.Service2)", "get");
		ServicePermission p32 = new ServicePermission(
				"(objectClass=com.foo.*)", "get");
		ServicePermission p33 = new ServicePermission("(objectClass=com.*)",
				"get");
		ServicePermission p34 = new ServicePermission("(objectClass=*)",
				"get");
		ServicePermission p35 = new ServicePermission("com.foo.Service2",
				"get");
		ServicePermission p36 = new ServicePermission("com.foo.*", "get");
		ServicePermission p37 = new ServicePermission("com.*", "get");
		ServicePermission p38 = new ServicePermission("*", "get");

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
	public void testFiltersServiceReference() {
		ServicePermission p41 = new ServicePermission("(id=2)", "get");
		ServicePermission p42 = new ServicePermission("(location=test.*)",
				"get");
		ServicePermission p43 = new ServicePermission("(name=test.*)", "get");
		ServicePermission p44 = new ServicePermission(
				"(signer=\\*, o=ACME, c=US)", "get");
		ServicePermission p45 = new ServicePermission(
				"(objectClass=com.foo.*)", "get");

		Bundle b46 = newMockBundle(2, "test.bsn", "test.location",
				"cn=Bugs Bunny, o=ACME, c=US");
		Map<String,Object> m46 = new HashMap<>();
		m46.put("service.id", Long.valueOf(2));
		m46.put("objectClass", new String[] {"com.foo.Service2"});
		ServiceReference< ? > r46 = newMockServiceReference(b46, m46);
		ServicePermission p46 = new ServicePermission(r46, "get");

		Bundle b47 = newMockBundle(3, "not.bsn", "not.location",
				"cn=Bugs Bunny, o=NOT, c=US");
		Map<String,Object> m47 = new HashMap<>();
		m47.put("service.id", Long.valueOf(3));
		m47.put("objectClass", new String[] {"com.bar.Service2"});
		ServiceReference< ? > r47 = newMockServiceReference(b47, m47);
		ServicePermission p47 = new ServicePermission(r47, "get");

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
		// added was constructed with service references.
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

	@Test
	public void testPermissionCollection() {
		ServicePermission p51 = new ServicePermission(
				"  (objectClass  =com.foo.Service2)", "get");
		ServicePermission p52 = new ServicePermission(
				"(objectClass=com.foo.*)", "get");
		ServicePermission p53 = new ServicePermission("(objectClass=com.*)",
				"get");
		ServicePermission p54 = new ServicePermission("(objectClass=*)",
				"get");
		ServicePermission p55 = new ServicePermission("com.foo.Service2",
				"get");

		ServicePermission p56 = new ServicePermission("com.bar.Service2",
				"get");
		ServicePermission p57 = new ServicePermission("com.bar.*", "get");
		ServicePermission p58 = new ServicePermission("com.*", "get");
		ServicePermission p59 = new ServicePermission("*", "get");

		Bundle b5a = newMockBundle(2, "test.bsn", "test.location",
		"cn=Bugs Bunny, o=ACME, c=US");
		Map<String,Object> m5a = new HashMap<>();
		m5a.put("service.id", Long.valueOf(2));
		m5a.put("objectClass", new String[] {"com.foo.Service2"});
		ServiceReference< ? > r5a = newMockServiceReference(b5a, m5a);
		ServicePermission p5a = new ServicePermission(r5a, "get");
		
		Bundle b5b = newMockBundle(3, "not.bsn", "not.location",
				"cn=Bugs Bunny, o=NOT, c=US");
		Map<String,Object> m5b = new HashMap<>();
		m5b.put("service.id", Long.valueOf(3));
		m5b.put("objectClass", new String[] {"com.bar.Service1",
				"com.bar.Service2"});
		ServiceReference< ? > r5b = newMockServiceReference(b5b, m5b);
		ServicePermission p5b = new ServicePermission(r5b, "get");

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
	public void testServiceProperties() {
		ServicePermission p61 = new ServicePermission(
				"(&(@Name=expected)(@@AT=atat)(name=test.bsn))", "get");
		Bundle b62 = newMockBundle(2, "test.bsn", "test.location",
				"cn=Bugs Bunny, o=ACME, c=US");
		Map<String,Object> m62 = new HashMap<>();
		m62.put("service.id", Long.valueOf(2));
		m62.put("objectClass", new String[] {"com.foo.Service2"});
		m62.put("name", "expected");
		m62.put("@at", "atat");
		ServiceReference< ? > r62 = newMockServiceReference(b62, m62);
		ServicePermission p62 = new ServicePermission(r62, "get");
		assertThat(p61).implies(p62);
	}
	
	private static void invalidServicePermission(String name, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new ServicePermission(name, actions));
	}
	
	private static void invalidServicePermission(ServiceReference< ? > service,
			String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new ServicePermission(service, actions));
	}
}
