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
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.PackagePermission;

public class PackagePermissionTests {

	@Test
	public void testInvalid() {
		invalidPackagePermission("a.b.c", "x");
		invalidPackagePermission("a.b.c", "   import  ,  x   ");
		invalidPackagePermission("a.b.c", "");
		invalidPackagePermission("a.b.c", "      ");
		invalidPackagePermission("a.b.c", null);
		invalidPackagePermission("a.b.c", ",");
		invalidPackagePermission("a.b.c", ",xxx");
		invalidPackagePermission("a.b.c", "xxx,");
		invalidPackagePermission("a.b.c", "import,");
		invalidPackagePermission("a.b.c", "export,   ");
		invalidPackagePermission("a.b.c", "exportonly,   ");
		invalidPackagePermission("a.b.c", "importme,");
		invalidPackagePermission("a.b.c", "exportme,");
		invalidPackagePermission("a.b.c", "exportonlyme,");
		invalidPackagePermission("a.b.c", ",import");
		invalidPackagePermission("a.b.c", ",export");
		invalidPackagePermission("a.b.c", ",exportonly");
		invalidPackagePermission("a.b.c", "   importme   ");
		invalidPackagePermission("a.b.c", "   exportme     ");
		invalidPackagePermission("a.b.c", "   exportonlyme     ");
		invalidPackagePermission("a.b.c", "   impor");
		invalidPackagePermission("a.b.c", "   expor");
		invalidPackagePermission("a.b.c", "   exportonl");
		invalidPackagePermission("()", "import");
		invalidPackagePermission("(package.name=a.b.c)", "exportonly");
		invalidPackagePermission("(package.name=a.b.c)", "exportonly,IMPORT");
		invalidPackagePermission("(package.name=a.b.c)", "export");
		invalidPackagePermission("(package.name=a.b.c)", "export,exportonly");
		invalidPackagePermission("(package.name=a.b.c)", "impoRT,exportonly");
		invalidPackagePermission("(package.name=a.b.c)", "impoRT,export");
		invalidPackagePermission("(package.name=a.b.c)",
				"export,impoRT,exportonly");

		invalidPackagePermission("(package.name=a.b.c)", null, "import");
		invalidPackagePermission("a.b.c", null, "import");
		invalidPackagePermission("a.b.c", newMockBundle(2, "test.bsn",
				"test.location", null), "export");
		invalidPackagePermission("a.b.c", newMockBundle(2, "test.bsn",
				"test.location", null), "exportonly");
		invalidPackagePermission("a.b.c", newMockBundle(2, "test.bsn",
				"test.location", null), "import, export");
		invalidPackagePermission("a.b.c", newMockBundle(2, "test.bsn",
				"test.location", null), "IMporT, exportonly");
		invalidPackagePermission("a.b.c", newMockBundle(2, "test.bsn",
				"test.location", null), "ExporT, exportonly,IMport");
	}

	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read");

		PackagePermission p11 = new PackagePermission("com.foo.service1",
				"    IMPORT,exportonly   ");
		PackagePermission p12 = new PackagePermission("com.foo.service1",
				"EXPORTONLY  ,   import");
		PackagePermission p13 = new PackagePermission("com.foo.service1",
				"expORT   ");

		PackagePermission p14 = new PackagePermission("com.foo.service1",
				"    Import    ");
		PackagePermission p15 = new PackagePermission("com.foo.service1",
				"expORTonly   ");
		PackagePermission p16 = new PackagePermission("com.foo.service1",
				"expORTonly,export");
		PackagePermission p17 = new PackagePermission("com.foo.service1",
				"impORt, EXport");
		PackagePermission p18 = new PackagePermission("com.foo.service1",
				"exPORTonly, impORt, EXport");

		assertThat(p11.getActions()).isEqualTo("exportonly,import");
		assertThat(p12.getActions()).isEqualTo("exportonly,import");
		assertThat(p13.getActions()).isEqualTo("exportonly,import");
		assertThat(p14.getActions()).isEqualTo("import");
		assertThat(p15.getActions()).isEqualTo("exportonly");
		assertThat(p16.getActions()).isEqualTo("exportonly,import");
		assertThat(p17.getActions()).isEqualTo("exportonly,import");
		assertThat(p18.getActions()).isEqualTo("exportonly,import");

		assertThat(p11).implies(p11);
		assertThat(p11).implies(p12);
		assertThat(p11).implies(p13);
		assertThat(p11).implies(p14);
		assertThat(p11).implies(p15);
		assertThat(p11).implies(p16);
		assertThat(p11).implies(p17);
		assertThat(p11).implies(p18);

		assertThat(p14).implies(p14);
		assertThat(p15).implies(p15);

		assertThat(p14).doesNotImply(p11);
		assertThat(p14).doesNotImply(p12);
		assertThat(p14).doesNotImply(p13);
		assertThat(p14).doesNotImply(p15);
		assertThat(p14).doesNotImply(p16);
		assertThat(p14).doesNotImply(p17);
		assertThat(p14).doesNotImply(p18);

		assertThat(p15).doesNotImply(p11);
		assertThat(p15).doesNotImply(p12);
		assertThat(p15).doesNotImply(p13);
		assertThat(p15).doesNotImply(p14);
		assertThat(p15).doesNotImply(p16);
		assertThat(p15).doesNotImply(p17);
		assertThat(p15).doesNotImply(p18);

		assertThat(p11).doesNotImply(op);

		assertThat(p11).isEqualTo(p11).hasSameHashCodeAs(p11);
		assertThat(p11).isEqualTo(p12).hasSameHashCodeAs(p12);
		assertThat(p11).isEqualTo(p13).hasSameHashCodeAs(p13);
		assertThat(p11).isEqualTo(p16).hasSameHashCodeAs(p16);
		assertThat(p11).isEqualTo(p17).hasSameHashCodeAs(p17);
		assertThat(p11).isEqualTo(p18).hasSameHashCodeAs(p18);

		assertThat(p11).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p11).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);

		assertThat(p13).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);
		assertThat(p13).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);

		assertThat(p14).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);
		assertThat(p14).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);

		assertThat(p15).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);
		assertThat(p15).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);

		assertThat(p16).isNotEqualTo(p14).doesNotHaveSameHashCodeAs(p14);

		assertThat(p11).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);

		assertThat(p14).isNotEqualTo(p11).doesNotHaveSameHashCodeAs(p11);
		assertThat(p14).isNotEqualTo(p15).doesNotHaveSameHashCodeAs(p15);

		PermissionCollection pc = p13.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p11);

		assertThat(pc).accepts(p14);
		assertThat(pc).implies(p14);
		assertThat(pc).doesNotImply(p11);
		assertThat(pc).doesNotImply(p12);
		assertThat(pc).doesNotImply(p13);
		assertThat(pc).doesNotImply(p15);
		assertThat(pc).doesNotImply(p16);
		assertThat(pc).doesNotImply(p17);
		assertThat(pc).doesNotImply(p18);

		assertThat(pc).accepts(p13);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).implies(p15);
		assertThat(pc).implies(p16);
		assertThat(pc).implies(p17);
		assertThat(pc).implies(p18);
		assertThat(pc).isSerializable();

		pc = p13.newPermissionCollection();
		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).implies(p15);
		assertThat(pc).implies(p16);
		assertThat(pc).implies(p17);
		assertThat(pc).implies(p18);

		assertThat(pc).rejects(op);
		assertThat(pc).isSerializable();

		pc = p13.newPermissionCollection();

		assertThat(pc).accepts(p13);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).implies(p15);
		assertThat(pc).implies(p16);
		assertThat(pc).implies(p17);
		assertThat(pc).implies(p18);
		assertThat(pc).isSerializable();

		pc = p11.newPermissionCollection();

		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).implies(p15);
		assertThat(pc).implies(p16);
		assertThat(pc).implies(p17);
		assertThat(pc).implies(p18);

		pc.setReadOnly();

		assertThat(pc).rejects(p12);

		assertThat(pc).hasElements();

		assertThat(p11).isSerializable();
		assertThat(p12).isSerializable();
		assertThat(p13).isSerializable();
		assertThat(p14).isSerializable();
		assertThat(p15).isSerializable();
		assertThat(p16).isSerializable();
		assertThat(p17).isSerializable();
		assertThat(p18).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testNames() {
		PackagePermission p21 = new PackagePermission("com.foo.service2",
				"import");
		PackagePermission p22 = new PackagePermission("com.foo.*", "import");
		PackagePermission p23 = new PackagePermission("com.*", "import");
		PackagePermission p24 = new PackagePermission("*", "import");

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
		PackagePermission exportonly = new PackagePermission("*", "exportonly");
		PackagePermission export = new PackagePermission("*", "export");
		PackagePermission importx = new PackagePermission("*", "import");

		assertThat(exportonly.getActions()).isEqualTo("exportonly");
		assertThat(export.getActions()).isEqualTo("exportonly,import");
		assertThat(importx.getActions()).isEqualTo("import");

		assertThat(export).implies(export);
		assertThat(importx).implies(importx);
		assertThat(exportonly).implies(exportonly);

		assertThat(export).implies(importx);
		assertThat(export).implies(exportonly);

		assertThat(importx).doesNotImply(export);
		assertThat(importx).doesNotImply(exportonly);

		assertThat(exportonly).doesNotImply(importx);
		assertThat(exportonly).doesNotImply(export);

		PackagePermission comfooexport = new PackagePermission("com.foo.*",
				"exportonly");
		PackagePermission both = new PackagePermission("com.foo.bar",
				"import,exportonly");
		PermissionCollection pc = importx.newPermissionCollection();
		assertThat(pc).accepts(importx);
		assertThat(pc).accepts(comfooexport);
		assertThat(pc).implies(both);
	}

	@Test
	public void testFiltersName() {
		PackagePermission p31 = new PackagePermission(
				"  (package.name  =com.foo.service2)", "import");
		PackagePermission p32 = new PackagePermission(
				"(package.name=com.foo.*)", "import");
		PackagePermission p33 = new PackagePermission("(package.name=com.*)",
				"import");
		PackagePermission p34 = new PackagePermission("(package.name=*)",
				"import");
		PackagePermission p35 = new PackagePermission("com.foo.service2",
				"import");
		PackagePermission p36 = new PackagePermission("com.foo.*", "import");
		PackagePermission p37 = new PackagePermission("com.*", "import");
		PackagePermission p38 = new PackagePermission("*", "import");

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
	public void testFiltersBundle() {
		PackagePermission p41 = new PackagePermission("(id=2)",
				"import");
		PackagePermission p42 = new PackagePermission(
				"(location=test.*)",
				"import");
		PackagePermission p43 = new PackagePermission("(name=test.*)",
				"import");
		PackagePermission p44 = new PackagePermission(
				"(signer=\\*, o=ACME, c=US)", "import");
		PackagePermission p45 = new PackagePermission(
				"(package.name=com.foo.*)", "import");

		PackagePermission p46 = new PackagePermission("com.foo.service2",
				newMockBundle(2, "test.bsn", "test.location",
						"cn=Bugs Bunny, o=ACME, c=US"), "import");
		PackagePermission p47 = new PackagePermission("com.bar.service2",
				newMockBundle(3, "not.bsn", "not.location",
						"cn=Bugs Bunny, o=NOT, c=US"), "import");

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

	@Test
	public void testPermissionCollection() {
		PackagePermission p51 = new PackagePermission(
				"  (package.name  =com.foo.service2)", "import");
		PackagePermission p52 = new PackagePermission(
				"(package.name=com.foo.*)", "import");
		PackagePermission p53 = new PackagePermission("(package.name=com.*)",
				"import");
		PackagePermission p54 = new PackagePermission("(package.name=*)",
				"import");
		PackagePermission p55 = new PackagePermission("com.foo.service2",
				"import");
		
		PackagePermission p56 = new PackagePermission("com.bar.service2",
		"import");
		PackagePermission p57 = new PackagePermission("com.bar.*", "import");
		PackagePermission p58 = new PackagePermission("com.*", "import");
		PackagePermission p59 = new PackagePermission("*", "import");

		PackagePermission p5a = new PackagePermission("com.foo.service2",
				newMockBundle(2, "test.bsn", "test.location",
						"cn=Bugs Bunny, o=ACME, c=US"), "import");
		PackagePermission p5b = new PackagePermission("com.bar.service2",
				newMockBundle(3, "not.bsn", "not.location",
						"cn=Bugs Bunny, o=NOT, c=US"), "import");

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

	private static void invalidPackagePermission(String name, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new PackagePermission(name, actions));
	}

	private static void invalidPackagePermission(String name, Bundle bundle,
			String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new PackagePermission(name, bundle, actions));
	}
}
