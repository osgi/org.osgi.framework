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
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;

public class AdminPermissionTests {

	/*
	 * @param actions <code>class</code>, <code>execute</code>,
	 * <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 * <code>listener</code>, <code>metadata</code>, <code>resolve</code> ,
	 * <code>resource</code>, <code>startlevel</code> or <code>context</code>. A
	 * value of "*" or <code>null</code> indicates all actions.
	 */
	@Test
	public void testInvalid() {
		invalidAdminPermission("*", "x");
		invalidAdminPermission("*", "   class  ,  x   ");
		invalidAdminPermission("*", "");
		invalidAdminPermission("*", "      ");
		invalidAdminPermission("*", ",");
		invalidAdminPermission("*", ",xxx");
		invalidAdminPermission("*", "xxx,");
		invalidAdminPermission("*", "execute,");
		invalidAdminPermission("*", "extensionLifecycle,   ");
		invalidAdminPermission("*", "lifecycleme,");
		invalidAdminPermission("*", "listenerme,");
		invalidAdminPermission("*", ",metadata");
		invalidAdminPermission("*", ",resolve");
		invalidAdminPermission("*", "   resourceme   ");
		invalidAdminPermission("*", "   startlevelme     ");
		invalidAdminPermission("*", "   contex");
		invalidAdminPermission("*", "   weav");

		invalidAdminPermission("()", "*");
		invalidAdminPermission((Bundle) null, "*");
	}

	@Test
	public void testDefault() {
		AdminPermission p1 = new AdminPermission();
		AdminPermission p2 = new AdminPermission("*", "*");
		AdminPermission p3 = new AdminPermission((String) null, null);
		AdminPermission p4 = new AdminPermission((String) null, p2.getActions());
		Permission op = new PropertyPermission("java.home", "read");

		assertThat(p1).implies(p2);
		assertThat(p2).implies(p1);
		assertThat(p1).implies(p3);
		assertThat(p3).implies(p1);
		assertThat(p3).implies(p2);
		assertThat(p2).implies(p3);
		assertThat(p1).implies(p4);
		assertThat(p4).implies(p1);
		assertThat(p4).implies(p3);
		assertThat(p3).implies(p4);
		assertThat(p3).implies(p4);
		assertThat(p4).implies(p3);
		assertThat(p4).implies(p2);
		assertThat(p2).implies(p4);
		assertThat(p1).implies(p1);
		assertThat(p2).implies(p2);
		assertThat(p3).implies(p3);
		assertThat(p4).implies(p4);
		assertThat(p1).doesNotImply(op);

		assertThat(p1).isEqualTo(p2).hasSameHashCodeAs(p2);
		assertThat(p2).isEqualTo(p1).hasSameHashCodeAs(p1);
		assertThat(p1).isEqualTo(p3).hasSameHashCodeAs(p3);
		assertThat(p3).isEqualTo(p1).hasSameHashCodeAs(p1);
		assertThat(p2).isEqualTo(p3).hasSameHashCodeAs(p3);
		assertThat(p3).isEqualTo(p2).hasSameHashCodeAs(p2);
		assertThat(p3).isEqualTo(p4).hasSameHashCodeAs(p4);
		assertThat(p4).isEqualTo(p3).hasSameHashCodeAs(p3);
		assertThat(p2).isEqualTo(p4).hasSameHashCodeAs(p4);
		assertThat(p4).isEqualTo(p2).hasSameHashCodeAs(p2);
		assertThat(p1).isEqualTo(p4).hasSameHashCodeAs(p4);
		assertThat(p4).isEqualTo(p1).hasSameHashCodeAs(p1);
		assertThat(p1).isNotEqualTo(op).doesNotHaveSameHashCodeAs(op);

		PermissionCollection pc = p1.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p1);

		assertThat(pc).accepts(p1);
		assertThat(pc).accepts(p2);
		assertThat(pc).accepts(p3);
		assertThat(pc).accepts(p4);
		assertThat(pc).rejects(op);

		pc.setReadOnly();

		assertThat(pc).rejects(new AdminPermission());

		assertThat(pc).implies(p1);
		assertThat(pc).implies(p2);
		assertThat(pc).implies(p3);
		assertThat(pc).implies(p4);
		assertThat(pc).doesNotImply(op);

		assertThat(pc).hasElements();

		assertThat(p1).isSerializable();
		assertThat(p2).isSerializable();
		assertThat(p3).isSerializable();
		assertThat(p4).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testFilter() {
		AdminPermission p1 = new AdminPermission("(id=2)", "class");
		AdminPermission p2 = new AdminPermission(" (id =2)", "class");
		AdminPermission p3 = new AdminPermission(newMockBundle(2, "test.bsn",
				"test.location", null), "resolve");
		AdminPermission p4 = new AdminPermission("(name=test.*)", "resource");
		AdminPermission p5 = new AdminPermission("(location=test.*)", "*");
		assertThat(p1).implies(p3);
		assertThat(p1).implies(p3);
		assertThat(p4).implies(p3);
		assertThat(p5).implies(p3);
		assertThat(p1).doesNotImply(p2);
		assertThat(p2).doesNotImply(p1);
		assertThat(p3).doesNotImply(p2);
		assertThat(p3).doesNotImply(p1);

		assertThat(p1).isEqualTo(p2).hasSameHashCodeAs(p2);
		assertThat(p2).isEqualTo(p1).hasSameHashCodeAs(p1);
		assertThat(p1).isNotEqualTo(p3).doesNotHaveSameHashCodeAs(p3);
		assertThat(p2).isNotEqualTo(p3).doesNotHaveSameHashCodeAs(p3);

		PermissionCollection pc = p1.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p3);
		assertThat(pc).doesNotImply(p1);

		assertThat(pc).accepts(new AdminPermission("(id=2)", "class"));
		assertThat(pc).accepts(new AdminPermission("(id=2)", "resource"));

		Bundle testBundle1 = newMockBundle(2, "test.bsn", "test.location", null);
		Bundle testBundle2 = newMockBundle(1, "test.bsn", "test.location", null);
		assertThat(pc).implies(new AdminPermission(testBundle1, "resolve"));
		assertThat(pc).implies(new AdminPermission(testBundle1, "class"));
		assertThat(pc).implies(new AdminPermission(testBundle1, "resource"));
		assertThat(pc).doesNotImply(new AdminPermission(testBundle2, "resolve"));
		assertThat(pc).doesNotImply(new AdminPermission(testBundle2, "class"));
		assertThat(pc).doesNotImply(new AdminPermission(testBundle2, "resource"));
		assertThat(pc).doesNotImply(new AdminPermission("*", "resource"));

		assertThat(pc).accepts(new AdminPermission());
		assertThat(pc).implies(new AdminPermission(testBundle1, "resolve"));
		assertThat(pc).implies(new AdminPermission(testBundle1, "class"));
		assertThat(pc).implies(new AdminPermission(testBundle1, "resource"));
		assertThat(pc).implies(new AdminPermission(testBundle2, "resolve"));
		assertThat(pc).implies(new AdminPermission(testBundle2, "class"));
		assertThat(pc).implies(new AdminPermission(testBundle2, "resource"));
		assertThat(pc).implies(new AdminPermission("*", "resource"));

		assertThat(pc).doesNotImply(p1);

		assertThat(pc).hasElements();

		assertThat(p1).isSerializable();
		assertThat(p2).isSerializable();
		assertThat(p3).isNotSerializable();
		assertThat(p4).isSerializable();
		assertThat(p5).isSerializable();
		assertThat(pc).isSerializable();
	}

	@Test
	public void testSigners() {
		AdminPermission ap = new AdminPermission("(signer=\\*, o=ACME, c=US)",
				"*");

		assertThat(ap).implies(new AdminPermission(newMockBundle(1, "test.bsn",
				"test.location", "cn=Bugs Bunny, o=ACME, c=US"), "*"));
		assertThat(ap).implies(new AdminPermission(newMockBundle(2, "test.bsn",
				"test.location", "ou = Carrots, cn=Daffy Duck, o=ACME, c=US"),
		"*"));
		assertThat(ap).implies(new AdminPermission(newMockBundle(3, "test.bsn",
				"test.location", "dc=www,dc=acme,dc=com,o=ACME,c=US"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(4, "test.bsn",
				"test.location",
		"street = 9C\\, Avenue St. Drézéry, o=ACME, c=FR"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(5, "test.bsn",
				"test.location", "dc=www, dc=acme, dc=com, c=US"), "*"));

		ap = new AdminPermission("(signer=cn=\\*,o=ACME,c=\\*)", "*");
		
		assertThat(ap).implies(new AdminPermission(newMockBundle(6, "test.bsn",
				"test.location", "cn = Daffy Duck , o = ACME , c = US"), "*"));
		assertThat(ap).implies(new AdminPermission(newMockBundle(7, "test.bsn",
				"test.location", "cn=Road Runner, o=ACME, c=NL"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(8, "test.bsn",
				"test.location", "o=ACME, c=NL"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(9, "test.bsn",
				"test.location", "dc=acme.com, cn=Bugs Bunny, o=ACME, c=US"),
				"*"));

		// test bad signer filter that does not escape '*'
		ap = new AdminPermission("(signer=*, o=ACME, c=US)", "*");
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(1, "test.bsn",
				"test.location", "cn=Bugs Bunny, o=ACME, c=US"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(2, "test.bsn",
				"test.location", "ou = Carrots, cn=Daffy Duck, o=ACME, c=US"),
		"*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(3, "test.bsn",
				"test.location", "dc=www,dc=acme,dc=com,o=ACME,c=US"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(4, "test.bsn",
				"test.location",
		"street = 9C\\, Avenue St. Drézéry, o=ACME, c=FR"), "*"));
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(5, "test.bsn",
				"test.location", "dc=www, dc=acme, dc=com, c=US"), "*"));
	}

	@Test
	public void testInvalidDN() {
		AdminPermission ap = new AdminPermission("(signer=;`´$.,@)", "*");
		assertThat(ap).doesNotImply(new AdminPermission(newMockBundle(1, "test.bsn",
						"test.location", "cn=Bugs Bunny, o=ACME, c=US"), "*"));
	}

	@Test
	public void testActionImplications() {
		AdminPermission classx = new AdminPermission("*", "CLASS");
		AdminPermission execute = new AdminPermission("*", "EXECUTE");
		AdminPermission extensionLifecycle = new AdminPermission("*",
				"EXTENSIONLIFECYCLE");
		AdminPermission lifecycle = new AdminPermission("*", "LIFECYCLE");
		AdminPermission listener = new AdminPermission("*", "LISTENER");
		AdminPermission metadata = new AdminPermission("*", "METADATA");
		AdminPermission resolve = new AdminPermission("*", "RESOLVE");
		AdminPermission resource = new AdminPermission("*", "RESOURCE");
		AdminPermission startlevel = new AdminPermission("*", "STARTLEVEL");
		AdminPermission context = new AdminPermission("*", "CONTEXT");
		AdminPermission weave = new AdminPermission("*", "WEAVE");

		assertThat(classx.getActions()).isEqualTo("class,resolve");
		assertThat(execute.getActions()).isEqualTo("execute,resolve");
		assertThat(extensionLifecycle.getActions()).isEqualTo("extensionLifecycle");
		assertThat(lifecycle.getActions()).isEqualTo("lifecycle");
		assertThat(listener.getActions()).isEqualTo("listener");
		assertThat(metadata.getActions()).isEqualTo("metadata");
		assertThat(resolve.getActions()).isEqualTo("resolve");
		assertThat(resource.getActions()).isEqualTo("resolve,resource");
		assertThat(startlevel.getActions()).isEqualTo("startlevel");
		assertThat(context.getActions()).isEqualTo("context");
		assertThat(weave.getActions()).isEqualTo("weave");

		classx = new AdminPermission(classx.getName(), classx.getActions());
		execute = new AdminPermission(execute.getName(), execute.getActions());
		extensionLifecycle = new AdminPermission(extensionLifecycle.getName(),
				extensionLifecycle.getActions());
		lifecycle = new AdminPermission(lifecycle.getName(),
				lifecycle.getActions());
		listener = new AdminPermission(listener.getName(),
				listener.getActions());
		metadata = new AdminPermission(metadata.getName(),
				metadata.getActions());
		resolve = new AdminPermission(resolve.getName(), resolve.getActions());
		resource = new AdminPermission(resource.getName(),
				resource.getActions());
		startlevel = new AdminPermission(startlevel.getName(),
				startlevel.getActions());
		context = new AdminPermission(context.getName(), context.getActions());
		weave = new AdminPermission(weave.getName(), weave.getActions());

		assertThat(classx).implies(classx);
		assertThat(classx).doesNotImply(execute);
		assertThat(classx).doesNotImply(extensionLifecycle);
		assertThat(classx).doesNotImply(lifecycle);
		assertThat(classx).doesNotImply(listener);
		assertThat(classx).doesNotImply(metadata);
		assertThat(classx).implies(resolve);
		assertThat(classx).doesNotImply(resource);
		assertThat(classx).doesNotImply(startlevel);
		assertThat(classx).doesNotImply(context);
		assertThat(classx).doesNotImply(weave);

		assertThat(execute).doesNotImply(classx);
		assertThat(execute).implies(execute);
		assertThat(execute).doesNotImply(extensionLifecycle);
		assertThat(execute).doesNotImply(lifecycle);
		assertThat(execute).doesNotImply(listener);
		assertThat(execute).doesNotImply(metadata);
		assertThat(execute).implies(resolve);
		assertThat(execute).doesNotImply(resource);
		assertThat(execute).doesNotImply(startlevel);
		assertThat(execute).doesNotImply(context);
		assertThat(execute).doesNotImply(weave);
		
		assertThat(extensionLifecycle).doesNotImply(classx);
		assertThat(extensionLifecycle).doesNotImply(execute);
		assertThat(extensionLifecycle).implies(extensionLifecycle);
		assertThat(extensionLifecycle).doesNotImply(lifecycle);
		assertThat(extensionLifecycle).doesNotImply(listener);
		assertThat(extensionLifecycle).doesNotImply(metadata);
		assertThat(extensionLifecycle).doesNotImply(resolve);
		assertThat(extensionLifecycle).doesNotImply(resource);
		assertThat(extensionLifecycle).doesNotImply(startlevel);
		assertThat(extensionLifecycle).doesNotImply(context);
		assertThat(extensionLifecycle).doesNotImply(weave);
		
		assertThat(lifecycle).doesNotImply(classx);
		assertThat(lifecycle).doesNotImply(execute);
		assertThat(lifecycle).doesNotImply(extensionLifecycle);
		assertThat(lifecycle).implies(lifecycle);
		assertThat(lifecycle).doesNotImply(listener);
		assertThat(lifecycle).doesNotImply(metadata);
		assertThat(lifecycle).doesNotImply(resolve);
		assertThat(lifecycle).doesNotImply(resource);
		assertThat(lifecycle).doesNotImply(startlevel);
		assertThat(lifecycle).doesNotImply(context);
		assertThat(lifecycle).doesNotImply(weave);
		
		assertThat(listener).doesNotImply(classx);
		assertThat(listener).doesNotImply(execute);
		assertThat(listener).doesNotImply(extensionLifecycle);
		assertThat(listener).doesNotImply(lifecycle);
		assertThat(listener).implies(listener);
		assertThat(listener).doesNotImply(metadata);
		assertThat(listener).doesNotImply(resolve);
		assertThat(listener).doesNotImply(resource);
		assertThat(listener).doesNotImply(startlevel);
		assertThat(listener).doesNotImply(context);
		assertThat(listener).doesNotImply(weave);
		
		assertThat(metadata).doesNotImply(classx);
		assertThat(metadata).doesNotImply(execute);
		assertThat(metadata).doesNotImply(extensionLifecycle);
		assertThat(metadata).doesNotImply(lifecycle);
		assertThat(metadata).doesNotImply(listener);
		assertThat(metadata).implies(metadata);
		assertThat(metadata).doesNotImply(resolve);
		assertThat(metadata).doesNotImply(resource);
		assertThat(metadata).doesNotImply(startlevel);
		assertThat(metadata).doesNotImply(context);
		assertThat(metadata).doesNotImply(weave);
		
		assertThat(resolve).doesNotImply(classx);
		assertThat(resolve).doesNotImply(execute);
		assertThat(resolve).doesNotImply(extensionLifecycle);
		assertThat(resolve).doesNotImply(lifecycle);
		assertThat(resolve).doesNotImply(listener);
		assertThat(resolve).doesNotImply(metadata);
		assertThat(resolve).implies(resolve);
		assertThat(resolve).doesNotImply(resource);
		assertThat(resolve).doesNotImply(startlevel);
		assertThat(resolve).doesNotImply(context);
		assertThat(resolve).doesNotImply(weave);
		
		assertThat(resource).doesNotImply(classx);
		assertThat(resource).doesNotImply(execute);
		assertThat(resource).doesNotImply(extensionLifecycle);
		assertThat(resource).doesNotImply(lifecycle);
		assertThat(resource).doesNotImply(listener);
		assertThat(resource).doesNotImply(metadata);
		assertThat(resource).implies(resolve);
		assertThat(resource).implies(resource);
		assertThat(resource).doesNotImply(startlevel);
		assertThat(resource).doesNotImply(context);
		assertThat(resource).doesNotImply(weave);
		
		assertThat(startlevel).doesNotImply(classx);
		assertThat(startlevel).doesNotImply(execute);
		assertThat(startlevel).doesNotImply(extensionLifecycle);
		assertThat(startlevel).doesNotImply(lifecycle);
		assertThat(startlevel).doesNotImply(listener);
		assertThat(startlevel).doesNotImply(metadata);
		assertThat(startlevel).doesNotImply(resolve);
		assertThat(startlevel).doesNotImply(resource);
		assertThat(startlevel).implies(startlevel);
		assertThat(startlevel).doesNotImply(context);
		assertThat(startlevel).doesNotImply(weave);
		
		assertThat(context).doesNotImply(classx);
		assertThat(context).doesNotImply(execute);
		assertThat(context).doesNotImply(extensionLifecycle);
		assertThat(context).doesNotImply(lifecycle);
		assertThat(context).doesNotImply(listener);
		assertThat(context).doesNotImply(metadata);
		assertThat(context).doesNotImply(resolve);
		assertThat(context).doesNotImply(resource);
		assertThat(context).doesNotImply(startlevel);
		assertThat(context).implies(context);
		assertThat(context).doesNotImply(weave);

		assertThat(weave).doesNotImply(classx);
		assertThat(weave).doesNotImply(execute);
		assertThat(weave).doesNotImply(extensionLifecycle);
		assertThat(weave).doesNotImply(lifecycle);
		assertThat(weave).doesNotImply(listener);
		assertThat(weave).doesNotImply(metadata);
		assertThat(weave).doesNotImply(resolve);
		assertThat(weave).doesNotImply(resource);
		assertThat(weave).doesNotImply(startlevel);
		assertThat(weave).doesNotImply(context);
		assertThat(weave).implies(weave);
		
		PermissionCollection pc = context.newPermissionCollection();
		AdminPermission ap = new AdminPermission("(id=2)", "metadata");
		AdminPermission all = new AdminPermission(newMockBundle(2, "test.bsn",
				"test.location", null), "metadata,context");

		assertThat(pc).accepts(context);
		assertThat(pc).accepts(ap);
		assertThat(pc).implies(all);
	}

	private static void invalidAdminPermission(String name, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new AdminPermission(name, actions));
	}
	
	private static void invalidAdminPermission(Bundle bundle, String actions) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("permission created with invalid arguments")
				.isThrownBy(() -> new AdminPermission(bundle, actions));
	}
}
