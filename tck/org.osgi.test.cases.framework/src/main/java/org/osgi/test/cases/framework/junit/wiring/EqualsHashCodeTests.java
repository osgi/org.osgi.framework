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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.test.cases.framework.junit.wiring.EqualsHashCodeTests.CollisionHook.Collision;

public class EqualsHashCodeTests extends WiringTest {
	static final class CollisionHook implements org.osgi.framework.hooks.bundle.CollisionHook {
		static final class Collision {
			private final List<Bundle> collisionCandidates;
			private final int operationType;
			private final Bundle target;
			
			public Collision(int operationType, Bundle target, Collection<Bundle> collisionCandidates) {
				this.operationType = operationType;
				this.target = target;
				this.collisionCandidates = new ArrayList<Bundle>(collisionCandidates);
			}
			
			public List<Bundle> getCollisionCandidates() {
				return collisionCandidates;
			}
			
			public int getOperationType() {
				return operationType;
			}
			
			public Bundle getTarget() {
				return target;
			}
		}
		
		private final Collection<Collision> collisions = new ArrayList<Collision>();
		
		public void filterCollisions(int operationType, Bundle target, Collection<Bundle> collisionCandidates) {
			collisions.add(new Collision(operationType, target, collisionCandidates));
			collisionCandidates.clear();
		}
		
		public Collection<Collision> getCollisions() {
			return collisions;
		}
	}
	
	private CollisionHook collisionHook;
	private ServiceRegistration<org.osgi.framework.hooks.bundle.CollisionHook> registration;

	@Test
	public void testEqualsHashCode() throws Exception {
		Bundle tb1 = install("wiring.tb1.jar.a", "wiring.tb1.jar");
		try {
			Bundle tb2 = install("wiring.tb1.jar.b", "wiring.tb1.jar");
			try {
				assertThat(frameworkWiring.resolveBundles(Arrays.asList(tb1, tb2)))
						.as("Bundles should resolve")
				.isTrue();
				assertThat(collisionHook.getCollisions())
						.as("Wrong number of calls to collision hook").hasSize(1);
				Collision collision = collisionHook.getCollisions().iterator().next();
				assertThat(collision.getTarget())
						.as("Wrong target")
				.isEqualTo(bundleContext.getBundle());
				assertThat(collision.getOperationType())
						.as("Wrong operation type")
				.isEqualTo(org.osgi.framework.hooks.bundle.CollisionHook.INSTALLING);
				assertThat(collision.getCollisionCandidates())
						.as("Wrong number of collision candidates").hasSize(1);
				BundleRevision tb1r = collision.getCollisionCandidates().get(0).adapt(BundleRevision.class);
				assertThat(tb1r)
						.as("Wrong collision candidate")
				.isEqualTo(tb1.adapt(BundleRevision.class));
				BundleRevision tb2r = tb2.adapt(BundleRevision.class);
				assertThat(tb1r.equals(tb2r))
						.as("Revisions should not be equal")
				.isFalse();
				assertThat(tb1r.hashCode() == tb2r.hashCode())
						.as("Hashcodes should not be equal")
				.isFalse();
				assertCapabilitiesNotEqual(tb1r, tb2r);
				assertRequirementsNotEqual(tb1r, tb2r);
				assertWiresNotEqual(tb1r, tb2r);
			}
			finally {
				uninstallSilently(tb2);
			}
		}
		finally {
			uninstallSilently(tb1);
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		String bsnversion = bundleContext.getProperty(Constants.FRAMEWORK_BSNVERSION);
		assertThat(bsnversion == null || bsnversion.equals(Constants.FRAMEWORK_BSNVERSION_MANAGED)).isTrue();
		collisionHook = new CollisionHook();
		registration = bundleContext.registerService(org.osgi.framework.hooks.bundle.CollisionHook.class, collisionHook, null);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (registration != null)
			registration.unregister();
		super.tearDown();
	}
	
	private void assertCapabilitiesNotEqual(BundleRevision tb1, BundleRevision tb2) {
		List<Capability> tb1Capabilities = tb1.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb1Capabilities.size())
				.as("Wrong number of capabilities")
				.isEqualTo(3);
		Capability tb1Capability = tb1Capabilities.get(0);
		assertThat(tb1Capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong capability")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1a");
		List<Capability> tb2Capabilities = tb2.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb1Capabilities.size())
				.as("Wrong number of capabilities")
				.isEqualTo(3);
		Capability tb2Capability = tb2Capabilities.get(0);
		assertThat(tb2Capability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong capability")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1a");
		assertThat(tb1Capability.equals(tb2Capability))
				.as("Capabilities should not be equal")
				.isFalse();
		assertThat(tb1Capability.hashCode() == tb2Capability.hashCode())
				.as("Hashcodes should not be equal")
				.isFalse();
	}
	
	private void assertRequirementsNotEqual(BundleRevision tb1, BundleRevision tb2) {
		List<Requirement> tb1Requirements = tb1.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb1Requirements)
				.as("Wrong number of requirements").hasSize(1);
		Requirement tb1Requirement = tb1Requirements.get(0);
		assertThat(tb1Requirement.getDirectives().get(Constants.FILTER_DIRECTIVE).toString().indexOf(PackageNamespace.PACKAGE_NAMESPACE + "=org.osgi.framework") > 0)
				.as("Wrong requirement")
				.isTrue();
		List<Requirement> tb2Requirements = tb2.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb1Requirements)
				.as("Wrong number of requirements").hasSize(1);
		Requirement tb2Requirement = tb2Requirements.get(0);
		assertThat(tb2Requirement.getDirectives().get(Constants.FILTER_DIRECTIVE).toString().indexOf(PackageNamespace.PACKAGE_NAMESPACE + "=org.osgi.framework") > 0)
				.as("Wrong requirement")
				.isTrue();
		assertThat(tb1Requirement.equals(tb2Requirement))
				.as("Requirements should not be equal")
				.isFalse();
		assertThat(tb1Requirement.hashCode() == tb2Requirement.hashCode())
				.as("Hashcodes should not be equal")
				.isFalse();
	}
	
	private void assertWiresNotEqual(BundleRevision tb1, BundleRevision tb2) {
		List<BundleWire> tb1Wires = tb1.getWiring().getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb1Wires)
				.as("Wrong number of wires").hasSize(1);
		BundleWire tb1Wire = tb1Wires.get(0);
		assertThat(tb1Wire.getRequirement().getDirectives().get(Constants.FILTER_DIRECTIVE).toString().indexOf(PackageNamespace.PACKAGE_NAMESPACE + "=org.osgi.framework") > 0)
				.as("Wrong wire")
				.isTrue();
		List<BundleWire> tb2Wires = tb2.getWiring().getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb2Wires)
				.as("Wrong number of wires").hasSize(1);
		BundleWire tb2Wire = tb2Wires.get(0);
		assertThat(tb2Wire.getRequirement().getDirectives().get(Constants.FILTER_DIRECTIVE).toString().indexOf(PackageNamespace.PACKAGE_NAMESPACE + "=org.osgi.framework") > 0)
				.as("Wrong wire")
				.isTrue();
		assertThat(tb1Wire.equals(tb2Wire))
				.as("Wires should not be equal")
				.isFalse();
		assertThat(tb1Wire.hashCode() == tb2Wire.hashCode())
				.as("Hashcodes should not be equal")
				.isFalse();
	}
	
	private Bundle install(String location, String bundle) throws Exception {
		URL entry = bundleContext.getBundle().getEntry(bundle);
		return bundleContext.installBundle(location, entry.openStream());
	}
}
