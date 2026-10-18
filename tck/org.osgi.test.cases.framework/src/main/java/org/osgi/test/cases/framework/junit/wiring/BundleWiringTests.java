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
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class BundleWiringTests extends WiringTest {
	@Test
	public void testGetRevision() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");
		Bundle tb14 = install("resolver.tb14.jar");
		List<Bundle> testBundles = Arrays.asList(tb1, tb2, tb3, tb4, tb5, tb14);

		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		List<BundleRevision> testRevisions = getRevisions(testBundles);

		BundleRevision tb1Revision = testRevisions.get(0);
		BundleRevision tb2Revision = testRevisions.get(1);
		BundleRevision tb3Revision = testRevisions.get(2);
		BundleRevision tb4Revision = testRevisions.get(3);
		BundleRevision tb5Revision = testRevisions.get(4);
		BundleRevision tb14Revision = testRevisions.get(5);
		
		List<BundleCapability> tb1AllCapabilities = tb1Revision.getDeclaredCapabilities(null);
		List<BundleCapability> tb1BundleCapabilities = tb1Revision
				.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleCapability> tb1HostCapabilities = tb1Revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		List<BundleCapability> tb1PackageCapabilities = tb1Revision
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		List<BundleCapability> tb1TestCapabilities = tb1Revision.getDeclaredCapabilities("test");
		List<BundleCapability> tb1TestMultipleCapabilities = tb1Revision.getDeclaredCapabilities("test.multiple");
		List<BundleCapability> tb1TestFragmentCapabilities = tb1Revision.getDeclaredCapabilities("test.fragment");
		checkCapabilities(tb1BundleCapabilities, tb1AllCapabilities,
				BundleNamespace.BUNDLE_NAMESPACE, 1, tb1Revision);
		checkCapabilities(tb1HostCapabilities, tb1AllCapabilities,
				HostNamespace.HOST_NAMESPACE, 1, tb1Revision);
		checkCapabilities(tb1PackageCapabilities, tb1AllCapabilities,
				PackageNamespace.PACKAGE_NAMESPACE, 1, tb1Revision);
		checkCapabilities(tb1TestCapabilities, tb1AllCapabilities, "test", 1, tb1Revision);
		checkCapabilities(tb1TestMultipleCapabilities, tb1AllCapabilities, "test.multiple", 2, tb1Revision);
		checkCapabilities(tb1TestFragmentCapabilities, tb1AllCapabilities, "test.fragment", 0, tb1Revision);
		
		List<BundleRequirement> tb1AllRequirements = tb1Revision.getDeclaredRequirements(null);
		List<BundleRequirement> tb1BundleRequirements = tb1Revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleRequirement> tb1HostRequirements = tb1Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		List<BundleRequirement> tb1PackageRequirements = tb1Revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		checkRequirements(tb1BundleRequirements, tb1AllRequirements,
				BundleNamespace.BUNDLE_NAMESPACE, 0, tb1Revision);
		checkRequirements(tb1HostRequirements, tb1AllRequirements,
				HostNamespace.HOST_NAMESPACE, 0, tb1Revision);
		checkRequirements(tb1PackageRequirements, tb1AllRequirements,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb1Revision);
		
		List<BundleCapability> tb2AllCapabilities = tb2Revision.getDeclaredCapabilities(null);
		List<BundleCapability> tb2BundleCapabilities = tb2Revision
				.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleCapability> tb2HostCapabilities = tb2Revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		List<BundleCapability> tb2PackageCapabilities = tb2Revision
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		checkCapabilities(tb2BundleCapabilities, tb2AllCapabilities,
				BundleNamespace.BUNDLE_NAMESPACE, 1, tb2Revision);
		checkCapabilities(tb2HostCapabilities, tb2AllCapabilities,
				HostNamespace.HOST_NAMESPACE, 0, tb2Revision);
		checkCapabilities(tb2PackageCapabilities, tb2AllCapabilities,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb2Revision);
		
		List<BundleRequirement> tb2AllRequirements = tb2Revision.getDeclaredRequirements(null);
		List<BundleRequirement> tb2BundleRequirements = tb2Revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleRequirement> tb2HostRequirements = tb2Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		List<BundleRequirement> tb2PackageRequirements = tb2Revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		checkRequirements(tb2BundleRequirements, tb2AllRequirements,
				BundleNamespace.BUNDLE_NAMESPACE, 0, tb2Revision);
		checkRequirements(tb2HostRequirements, tb2AllRequirements,
				HostNamespace.HOST_NAMESPACE, 0, tb2Revision);
		checkRequirements(tb2PackageRequirements, tb2AllRequirements,
				PackageNamespace.PACKAGE_NAMESPACE, 2, tb2Revision);
		
		List<BundleCapability> tb3AllCapabilities = tb3Revision.getDeclaredCapabilities(null);
		List<BundleCapability> tb3BundleCapabilities = tb3Revision
				.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleCapability> tb3HostCapabilities = tb3Revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		List<BundleCapability> tb3PackageCapabilities = tb3Revision
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		checkCapabilities(tb3BundleCapabilities, tb3AllCapabilities,
				BundleNamespace.BUNDLE_NAMESPACE, 1, tb3Revision);
		checkCapabilities(tb3HostCapabilities, tb3AllCapabilities,
				HostNamespace.HOST_NAMESPACE, 1, tb3Revision);
		checkCapabilities(tb3PackageCapabilities, tb3AllCapabilities,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb3Revision);
		
		List<BundleRequirement> tb3AllRequirements = tb3Revision.getDeclaredRequirements(null);
		List<BundleRequirement> tb3BundleRequirements = tb3Revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleRequirement> tb3HostRequirements = tb3Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		List<BundleRequirement> tb3PackageRequirements = tb3Revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		checkRequirements(tb3BundleRequirements, tb3AllRequirements,
				BundleNamespace.BUNDLE_NAMESPACE, 1, tb3Revision);
		checkRequirements(tb3HostRequirements, tb3AllRequirements,
				HostNamespace.HOST_NAMESPACE, 0, tb3Revision);
		checkRequirements(tb3PackageRequirements, tb3AllRequirements,
				PackageNamespace.PACKAGE_NAMESPACE, 1, tb3Revision);

		List<BundleCapability> tb4AllCapabilities = tb4Revision.getDeclaredCapabilities(null);
		List<BundleCapability> tb4BundleCapabilities = tb4Revision
				.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleCapability> tb4HostCapabilities = tb4Revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		List<BundleCapability> tb4PackageCapabilities = tb4Revision
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		List<BundleCapability> tb4TestFragmentCapabilities = tb4Revision.getDeclaredCapabilities("test.fragment");
		checkCapabilities(tb4BundleCapabilities, tb4AllCapabilities,
				BundleNamespace.BUNDLE_NAMESPACE, 0, tb4Revision);
		checkCapabilities(tb4HostCapabilities, tb4AllCapabilities,
				HostNamespace.HOST_NAMESPACE, 0, tb4Revision);
		checkCapabilities(tb4PackageCapabilities, tb4AllCapabilities,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb4Revision);
		checkCapabilities(tb4TestFragmentCapabilities, tb4AllCapabilities, "test.fragment", 1, tb4Revision);
		
		List<BundleRequirement> tb4AllRequirements = tb4Revision.getDeclaredRequirements(null);
		List<BundleRequirement> tb4BundleRequirements = tb4Revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleRequirement> tb4HostRequirements = tb4Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		List<BundleRequirement> tb4PackageRequirements = tb4Revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		checkRequirements(tb4BundleRequirements, tb4AllRequirements,
				BundleNamespace.BUNDLE_NAMESPACE, 0, tb4Revision);
		checkRequirements(tb4HostRequirements, tb4AllRequirements,
				HostNamespace.HOST_NAMESPACE, 1, tb4Revision);
		checkRequirements(tb4PackageRequirements, tb4AllRequirements,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb4Revision);
		
		List<BundleCapability> tb5AllCapabilities = tb5Revision.getDeclaredCapabilities(null);
		List<BundleCapability> tb5BundleCapabilities = tb5Revision
				.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleCapability> tb5HostCapabilities = tb5Revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		List<BundleCapability> tb5PackageCapabilities = tb5Revision
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		checkCapabilities(tb5BundleCapabilities, tb5AllCapabilities,
				BundleNamespace.BUNDLE_NAMESPACE, 1, tb5Revision);
		checkCapabilities(tb5HostCapabilities, tb5AllCapabilities,
				HostNamespace.HOST_NAMESPACE, 1, tb5Revision);
		checkCapabilities(tb5PackageCapabilities, tb5AllCapabilities,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb5Revision);
		
		List<BundleRequirement> tb5AllRequirements = tb5Revision.getDeclaredRequirements(null);
		List<BundleRequirement> tb5BundleRequirements = tb5Revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleRequirement> tb5HostRequirements = tb5Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		List<BundleRequirement> tb5PackageRequirements = tb5Revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		List<BundleRequirement> tb5TestRequirements = tb5Revision.getDeclaredRequirements("test");
		List<BundleRequirement> tb5TestNoAttrsRequirements = tb5Revision.getDeclaredRequirements("test.no.attrs");
		checkRequirements(tb5BundleRequirements, tb5AllRequirements,
				BundleNamespace.BUNDLE_NAMESPACE, 0, tb5Revision);
		checkRequirements(tb5HostRequirements, tb5AllRequirements,
				HostNamespace.HOST_NAMESPACE, 0, tb5Revision);
		checkRequirements(tb5PackageRequirements, tb5AllRequirements,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb5Revision);
		checkRequirements(tb5TestRequirements, tb5AllRequirements, "test", 10, tb5Revision);
		checkRequirements(tb5TestNoAttrsRequirements, tb5AllRequirements, "test.no.attrs", 1, tb5Revision);
		
		List<BundleCapability> tb14AllCapabilities = tb14Revision.getDeclaredCapabilities(null);
		List<BundleCapability> tb14BundleCapabilities = tb14Revision
				.getDeclaredCapabilities(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleCapability> tb14HostCapabilities = tb14Revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		List<BundleCapability> tb14PackageCapabilities = tb14Revision
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
		checkCapabilities(tb14BundleCapabilities, tb14AllCapabilities,
				BundleNamespace.BUNDLE_NAMESPACE, 1, tb14Revision);
		checkCapabilities(tb14HostCapabilities, tb14AllCapabilities,
				HostNamespace.HOST_NAMESPACE, 1, tb14Revision);
		checkCapabilities(tb14PackageCapabilities, tb14AllCapabilities,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb14Revision);
		
		List<BundleRequirement> tb14AllRequirements = tb14Revision.getDeclaredRequirements(null);
		List<BundleRequirement> tb14BundleRequirements = tb14Revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleRequirement> tb14HostRequirements = tb14Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		List<BundleRequirement> tb14PackageRequirements = tb14Revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		List<BundleRequirement> tb14TestFragmentRequirements = tb14Revision.getDeclaredRequirements("test.fragment");
		checkRequirements(tb14BundleRequirements, tb14AllRequirements,
				BundleNamespace.BUNDLE_NAMESPACE, 0, tb14Revision);
		checkRequirements(tb14HostRequirements, tb14AllRequirements,
				HostNamespace.HOST_NAMESPACE, 0, tb14Revision);
		checkRequirements(tb14PackageRequirements, tb14AllRequirements,
				PackageNamespace.PACKAGE_NAMESPACE, 0, tb14Revision);
		checkRequirements(tb14TestFragmentRequirements, tb14AllRequirements, "test.fragment", 1, tb14Revision);
	}

	void checkCapabilities(List<BundleCapability> toCheck, List<BundleCapability> all, String namespace, int expectedNum, BundleRevision provider) {
		assertThat(toCheck.size())
				.as("Wrong number of capabilities for " + namespace + " from " + provider)
				.isEqualTo(expectedNum);
		for (BundleCapability capability : toCheck) {
			assertThat(all.contains(capability))
					.as("Capability is not in all capabilities")
				.isTrue();
			assertThat(capability.getNamespace())
					.as("Wrong namespace")
				.isEqualTo(namespace);
			assertThat(capability.getRevision())
					.as("Wrong provider")
				.isEqualTo(provider);
			assertResourceEqualsRevision(capability.getResource(), capability.getRevision());
		}
	}
	
	void checkRequirements(List<BundleRequirement> toCheck, List<BundleRequirement> all, String namespace, int expectedNum, BundleRevision requirer) {
		assertThat(toCheck.size())
				.as("Wrong number of requirements for " + namespace + " from " + requirer)
				.isEqualTo(expectedNum);
		for (BundleRequirement requirement : toCheck) {
			assertThat(all.contains(requirement))
					.as("Capability is not in all capabilities")
				.isTrue();
			assertThat(requirement.getNamespace())
					.as("Wrong namespace")
				.isEqualTo(namespace);
			assertThat(requirement.getRevision())
					.as("Wrong requirer")
				.isEqualTo(requirer);
			assertResourceEqualsRevision(requirement.getResource(), requirement.getRevision());
		}
	}
	
	void checkWires(List<BundleWire> toCheck, List<BundleWire> all, String namespace, int expectedNum) {
		assertThat(toCheck.size())
				.as("Wrong number of wires for " + namespace)
				.isEqualTo(expectedNum);
		for (BundleWire wire : toCheck) {
			assertThat(all.contains(wire))
					.as("Capability is not in all capabilities for revision")
				.isTrue();
		}
	}

	private List<BundleRevision> getRevisions(List<Bundle> testBundles) {
		ArrayList<BundleRevision> result = new ArrayList<BundleRevision>(testBundles.size());
		for (Bundle bundle : testBundles) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			result.add(revision);
			assertThat(revision.getSymbolicName())
					.as("Wrong BSN")
				.isEqualTo(bundle.getSymbolicName());
			assertThat(revision.getBundle())
					.as("Wrong bundle")
				.isEqualTo(bundle);
			assertThat(revision.getVersion())
					.as("Wrong version")
				.isEqualTo(bundle.getVersion());
			assertThat(revision.getTypes())
					.as("Wrong type")
				.isEqualTo(bundle.getHeaders("").get(Constants.FRAGMENT_HOST) == null ? 0 : BundleRevision.TYPE_FRAGMENT);
			
			Collection<BundleWire> hostWirings = revision.getWiring()
					.getRequiredWires(HostNamespace.HOST_NAMESPACE);
			assertThat(hostWirings)
					.as("Host wirings must never be null.")
				.isNotNull();
			assertRequiredResourceWiresEqualsRequiredWires(revision.getWiring(), HostNamespace.HOST_NAMESPACE);
			if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				// we assume the fragment resolved to one host
				assertThat(hostWirings)
						.as("Wrong number of host wirings found").hasSize(1);
			} else {
				// regular bundles must have empty host wirings
				assertThat(hostWirings)
						.as("Must have empty wirings for regular bundles").hasSize(0);
			}
			
			assertCapabilitiesEqualsDeclaredCapabilities(revision);
			assertRequirementsEqualsDeclaredRequirements(revision);
		}
		return result;
	}
	
	private void assertCapabilitiesEqualsDeclaredCapabilities(BundleRevision revision) {
		assertThat(revision.getCapabilities(null))
				.as("Capabilities do not equal declared capabilities")
				.isEqualTo(revision.getDeclaredCapabilities(null));
	}
	
	private void assertProvidedResourceWiresEqualsProvidedWires(BundleWiring wiring, String namespace) {
		List<BundleWire> providedWires = wiring.getProvidedWires(namespace);
		List<Wire> providedResourceWires = wiring.getProvidedResourceWires(namespace);
		assertThat(providedResourceWires)
				.as("Provided resource wires not same as provided wires for namespace " + namespace)
				.isEqualTo(providedWires);
	}
	
	private void assertProvidedResourceWiresEqualsProvidedWires(BundleWiring wiring, Collection<String> genericNamespaces) {
		assertProvidedResourceWiresEqualsProvidedWires(wiring, (String)null);
		assertProvidedResourceWiresEqualsProvidedWires(wiring, BundleNamespace.BUNDLE_NAMESPACE);
		assertProvidedResourceWiresEqualsProvidedWires(wiring, HostNamespace.HOST_NAMESPACE);
		assertProvidedResourceWiresEqualsProvidedWires(wiring, PackageNamespace.PACKAGE_NAMESPACE);
		for (String namespace : genericNamespaces)
			assertProvidedResourceWiresEqualsProvidedWires(wiring, namespace);
	}
	
	private void assertProvidedResourceWiresEqualsProvidedWires(BundleWiring[] wirings, Collection<String> genericNamespaces) {
		for (BundleWiring wiring : wirings)
			assertProvidedResourceWiresEqualsProvidedWires(wiring, genericNamespaces);
	}
	
	private void assertRequiredResourceWiresEqualsRequiredWires(BundleWiring wiring, String namespace) {
		List<BundleWire> requiredWires = wiring.getRequiredWires(namespace);
		List<Wire> requiredResourceWires = wiring.getRequiredResourceWires(namespace);
		assertThat(requiredResourceWires)
				.as("Required resource wires not same as required wires for namespace " + namespace)
				.isEqualTo(requiredWires);
	}
	
	private void assertRequiredResourceWiresEqualsRequiredWires(BundleWiring wiring, Collection<String> genericNamespaces) {
		assertRequiredResourceWiresEqualsRequiredWires(wiring, (String)null);
		assertRequiredResourceWiresEqualsRequiredWires(wiring, BundleNamespace.BUNDLE_NAMESPACE);
		assertRequiredResourceWiresEqualsRequiredWires(wiring, HostNamespace.HOST_NAMESPACE);
		assertRequiredResourceWiresEqualsRequiredWires(wiring, PackageNamespace.PACKAGE_NAMESPACE);
		for (String namespace : genericNamespaces)
			assertRequiredResourceWiresEqualsRequiredWires(wiring, namespace);
	}
	
	private void assertRequiredResourceWiresEqualsRequiredWires(BundleWiring[] wirings, Collection<String> genericNamespaces) {
		for (BundleWiring wiring : wirings)
			assertRequiredResourceWiresEqualsRequiredWires(wiring, genericNamespaces);
	}
	
	private void assertRequirementsEqualsDeclaredRequirements(BundleRevision revision) {
		assertThat(revision.getRequirements(null))
				.as("Requirements do not equal declared requirements")
				.isEqualTo(revision.getDeclaredRequirements(null));
	}
	
	private void assertResourceEqualsRevision(BundleWiring wiring) {
		assertResourceEqualsRevision(wiring.getRevision(), wiring.getResource());
	}
	
	private void assertResourceEqualsRevision(Resource resource, BundleRevision revision) {
		assertThat(resource)
				.as("Resource does not equal revision")
				.isEqualTo(revision);
	}
	
	private void assertResourceCapabilitiesEqualsCapabilites(BundleWiring wiring) {
		assertThat(wiring.getResourceCapabilities(null))
				.as("Resource capabilities do not equal capabilities")
				.isEqualTo(wiring.getCapabilities(null));
	}
	
	private void assertResourceRequirementsEqualsRequirements(BundleWiring wiring) {
		assertThat(wiring.getResourceRequirements(null))
				.as("Resource requirements do not equal requirements")
				.isEqualTo(wiring.getRequirements(null));
	}

	@Test
	public void testGetWiring() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");
		Bundle tb14 = install("resolver.tb14.jar");
		List<Bundle> testBundles = Arrays.asList(tb1, tb2, tb3, tb4, tb5, tb14);

		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		BundleWiring tb1Wiring = tb1.adapt(BundleWiring.class);
		BundleWiring tb2Wiring = tb2.adapt(BundleWiring.class);
		BundleWiring tb3Wiring = tb3.adapt(BundleWiring.class);
		BundleWiring tb4Wiring = tb4.adapt(BundleWiring.class);
		BundleWiring tb5Wiring = tb5.adapt(BundleWiring.class);
		BundleWiring tb14Wiring = tb14.adapt(BundleWiring.class);
		BundleWiring[] wirings = new BundleWiring[] {tb1Wiring, tb2Wiring, tb3Wiring, tb4Wiring, tb5Wiring, tb14Wiring};
		
		checkBundleWiring(testBundles.toArray(new Bundle[testBundles.size()]), wirings);

		List<String> genericNamespaces = Arrays.asList("test", "test.multiple", "test.no.attrs", "test.fragment");
		assertProvidedResourceWiresEqualsProvidedWires(tb1Wiring, genericNamespaces);
		
		List<BundleWire> allTb1Capabilities = tb1Wiring.getProvidedWires(null);
		List<BundleWire> osgiBundleTb1Capabilities = tb1Wiring
				.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);
		List<BundleWire> osgiHostTb1Capabilities = tb1Wiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		List<BundleWire> osgiPackageTb1Capabilities = tb1Wiring
				.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
		List<BundleWire> genTestTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(0));
		List<BundleWire> genTestMultipleTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(1));
		List<BundleWire> genTestNoAttrsTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(2));
		List<BundleWire> genTestFragmentTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(3));

		// check for osgi.wiring.host capability from wiring with
		// fragment-attachment:="never"
		List<BundleCapability> osgiHostTb2Capabilities = tb2Wiring
				.getCapabilities(HostNamespace.HOST_NAMESPACE);
		assertThat(osgiHostTb2Capabilities)
				.as("Expecting no osgi.wiring.host capability").hasSize(0);

		checkBundleWires(tb1Wiring, tb2Wiring, tb3Wiring, tb5Wiring, tb14Wiring, tb4, allTb1Capabilities, osgiBundleTb1Capabilities, osgiHostTb1Capabilities, osgiPackageTb1Capabilities, genTestTb1Capabilities, genTestMultipleTb1Capabilities, genTestFragmentTb1Capabilities, genTestNoAttrsTb1Capabilities);

		// test the refresh case
		refreshBundles(Arrays.asList(new Bundle[]{tb1}));
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		checkNotInUseWirings(wirings);

		tb1Wiring = tb1.adapt(BundleWiring.class);
		tb2Wiring = tb2.adapt(BundleWiring.class);
		tb3Wiring = tb3.adapt(BundleWiring.class);
		tb4Wiring = tb4.adapt(BundleWiring.class);
		tb5Wiring = tb5.adapt(BundleWiring.class);
		tb14Wiring = tb14.adapt(BundleWiring.class);
		wirings = new BundleWiring[] {tb1Wiring, tb2Wiring, tb3Wiring, tb4Wiring, tb5Wiring, tb14Wiring};

		checkBundleWiring(testBundles.toArray(new Bundle[testBundles.size()]), wirings);

		// get wired capabilities before update
		osgiBundleTb1Capabilities = tb1Wiring
				.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);
		osgiHostTb1Capabilities = tb1Wiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		osgiPackageTb1Capabilities = tb1Wiring
				.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
		genTestTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(0));
		genTestMultipleTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(1));
		genTestNoAttrsTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(2));
		genTestFragmentTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(3));

		// test the update case
		URL content = bundleContext.getBundle().getEntry("resolver.tb1.v110.jar");
		assertThat(content)
				.as("Cannot find content for update")
				.isNotNull();
		try {
			tb1.update(content.openStream());
		} catch (BundleException e) {
			fail("Unexpected update failure", e);
		} catch (IOException e) {
			fail("Unexpected update failure", e);
		}

		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb1})))
				.as("Could not resolve updated bundle")
				.isTrue();

		// check that the updated wiring has no requirers
		BundleWiring updatedWiring = tb1.adapt(BundleWiring.class);
		checkBundleWiring(new Bundle[] {tb1}, new BundleWiring[] {updatedWiring});
		List<BundleWire> updatedWires = updatedWiring.getProvidedWires(null);
		assertThat(updatedWires)
				.as("Requirers is null")
				.isNotNull();
		// May have zero wires or one fragment wire
		if (updatedWires.size() == 1) {
			assertThat(updatedWires.get(0)
							.getCapability().getNamespace())
					.as("Wrong wire capability namespace")
				.isEqualTo(HostNamespace.HOST_NAMESPACE);
			assertThat(updatedWires.get(0).getRequirement().getRevision())
					.as("Wrong requirer")
				.isEqualTo(tb4Wiring.getRevision());
		} else {
			assertThat(updatedWires)
					.as("Wrong number of requirers").hasSize(0);
		}

		assertThat(tb1Wiring.isInUse())
				.as("Wiring is not in use for: " + tb1)
				.isTrue();
		assertThat(tb1Wiring.isCurrent())
				.as("Wiring is current for: " + tb1)
				.isFalse();

		// Check that old wiring for tb1 is still correct
		assertProvidedResourceWiresEqualsProvidedWires(tb1Wiring, genericNamespaces);
		allTb1Capabilities = tb1Wiring.getProvidedWires(null);
		checkBundleWires(tb1Wiring, tb2Wiring, tb3Wiring, tb5Wiring, tb14Wiring, tb4, allTb1Capabilities, osgiBundleTb1Capabilities, osgiHostTb1Capabilities, osgiPackageTb1Capabilities, genTestTb1Capabilities, genTestMultipleTb1Capabilities, genTestFragmentTb1Capabilities, genTestNoAttrsTb1Capabilities);

		// test the refresh case
		refreshBundles(Arrays.asList(new Bundle[]{tb1}));
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		checkNotInUseWirings(wirings);
		checkNotInUseWirings(new BundleWiring[] {updatedWiring});

		tb1Wiring = tb1.adapt(BundleWiring.class);
		tb2Wiring = tb2.adapt(BundleWiring.class);
		tb3Wiring = tb3.adapt(BundleWiring.class);
		tb4Wiring = tb4.adapt(BundleWiring.class);
		tb5Wiring = tb5.adapt(BundleWiring.class);
		tb14Wiring = tb14.adapt(BundleWiring.class);
		wirings = new BundleWiring[] {tb1Wiring, tb2Wiring, tb3Wiring, tb4Wiring, tb5Wiring, tb14Wiring};

		checkBundleWiring(testBundles.toArray(new Bundle[testBundles.size()]), wirings);

		// test uninstall case
		try {
			tb1.uninstall();
		} catch (BundleException e) {
			fail("Unexpecte error on uninstall", e);
		}

		assertThat(tb1.adapt(BundleWiring.class))
				.as("Bundle wiring is not null for bundle: " + tb1)
				.isNull();
		// note that we do not reget tb1Wiring because it must be null on uninstall from the check above
		tb2Wiring = tb2.adapt(BundleWiring.class);
		tb3Wiring = tb3.adapt(BundleWiring.class);
		tb4Wiring = tb4.adapt(BundleWiring.class);
		tb5Wiring = tb5.adapt(BundleWiring.class);
		tb14Wiring = tb14.adapt(BundleWiring.class);
		wirings = new BundleWiring[] {tb1Wiring, tb2Wiring, tb3Wiring, tb4Wiring, tb5Wiring, tb14Wiring};

		assertThat(tb1Wiring.isInUse())
				.as("Wiring is not in use for: " + tb1)
				.isTrue();
		assertThat(tb1Wiring.isCurrent())
				.as("Wring is current for: " + tb1)
				.isFalse();

		allTb1Capabilities = tb1Wiring.getProvidedWires(null);
		osgiBundleTb1Capabilities = tb1Wiring
				.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);
		osgiHostTb1Capabilities = tb1Wiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		osgiPackageTb1Capabilities = tb1Wiring
				.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
		genTestTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(0));
		genTestMultipleTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(1));
		genTestNoAttrsTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(2));
		genTestFragmentTb1Capabilities = tb1Wiring.getProvidedWires(genericNamespaces.get(3));
		
		checkBundleWires(tb1Wiring, tb2Wiring, tb3Wiring, tb5Wiring, tb14Wiring, tb4, allTb1Capabilities, osgiBundleTb1Capabilities, osgiHostTb1Capabilities, osgiPackageTb1Capabilities, genTestTb1Capabilities, genTestMultipleTb1Capabilities, genTestFragmentTb1Capabilities, genTestNoAttrsTb1Capabilities);

		// test the refresh case
		refreshBundles(null);
		assertThat(frameworkWiring.resolveBundles(testBundles)).isFalse();

		checkNotInUseWirings(wirings);

		// Wirings must be null now since the bundles are not resolved
		assertThat(tb1.adapt(BundleWiring.class))
				.as("Bundle wiring is not null for bundle: " + tb1)
				.isNull();
		assertThat(tb2.adapt(BundleWiring.class))
				.as("Bundle wiring is not null for bundle: " + tb2)
				.isNull();
		assertThat(tb3.adapt(BundleWiring.class))
				.as("Bundle wiring is not null for bundle: " + tb3)
				.isNull();
		assertThat(tb4.adapt(BundleWiring.class))
				.as("Bundle wiring is not null for bundle: " + tb4)
				.isNull();
		assertThat(tb5.adapt(BundleWiring.class))
				.as("Bundle wiring is not null for bundle: " + tb5)
				.isNull();
	}

	private void checkBundleWiring(Bundle[] bundles, BundleWiring[] wirings) {
		assertThat(wirings.length)
				.as("Lists are not the same size")
				.isEqualTo(bundles.length);
		assertProvidedResourceWiresEqualsProvidedWires(wirings,
				Collections.<String> emptyList());
		assertRequiredResourceWiresEqualsRequiredWires(wirings,
				Collections.<String> emptyList());
		for (int i = 0; i < wirings.length; i++) {
			BundleWiring wiring = wirings[i];
			Bundle bundle = bundles[i];
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			assertThat(revision)
					.as("BundleRevision is null for: " + bundle)
				.isNotNull();
			assertThat(revision.getSymbolicName())
					.as("Wrong BSN")
				.isEqualTo(bundle.getSymbolicName());
			assertThat(revision.getVersion())
					.as("Wrong version")
				.isEqualTo(bundle.getVersion());

			assertThat(wiring)
					.as("BundleWiring is null for bundle: " + bundle)
				.isNotNull();
			assertThat(wiring.getBundle())
					.as("Wrong bundle for wiring: " + bundle)
				.isEqualTo(bundle);

			BundleRevision wiringRevision = wiring.getRevision();
			assertThat(wiringRevision)
					.as("Wiring revision is null for bundle: " + bundle)
				.isNotNull();
			assertThat(wiringRevision.getSymbolicName())
					.as("Wrong BSN")
				.isEqualTo(bundle.getSymbolicName());
			assertThat(wiringRevision.getVersion())
					.as("Wrong version")
				.isEqualTo(bundle.getVersion());

			assertThat(wiring.isCurrent())
					.as("Wiring is not current for: " + bundle)
				.isTrue();
			assertThat(wiring.isInUse())
					.as("Wiring is not in use for: " + bundle)
				.isTrue();
			
			assertResourceEqualsRevision(wiring);
			assertResourceCapabilitiesEqualsCapabilites(wiring);
			assertResourceRequirementsEqualsRequirements(wiring);
		}
	}

	private void checkNotInUseWirings(BundleWiring[] wirings) {
		for (int i = 0; i < wirings.length; i++) {
			BundleWiring wiring = wirings[i];
			if (wiring == null)
				continue; // fragment case
			assertThat(wiring.isCurrent())
					.as("Wiring is current for: " + wiring.getBundle())
				.isFalse();
			assertThat(wiring.isInUse())
					.as("Wiring is in use for: " + wiring.getBundle())
				.isFalse();
			assertThat(wiring.getCapabilities(null))
					.as("Wiring capabilities must be null: " + wiring.getBundle())
				.isNull();
			assertThat(wiring.getRequirements(null))
					.as("Wiring requirements must be null: " + wiring.getBundle())
				.isNull();
			assertThat(wiring.getClassLoader())
					.as("Wiring class loader must be null: " + wiring.getBundle())
				.isNull();
			assertThat(wiring.findEntries("/", "*", 0))
					.as("Wiring findEntries must be null: " + wiring.getBundle())
				.isNull();
			assertThat(wiring.listResources("/", "*", 0))
					.as("Wiring listResources must be null: " + wiring.getBundle())
				.isNull();
			assertThat(wiring.getProvidedWires(null))
					.as("Wiring providedWires must be null: " + wiring.getBundle())
				.isNull();
			assertThat(wiring.getRequiredWires(null))
					.as("Wiring requiredWires must be null: " + wiring.getBundle())
				.isNull();
		}
	}
	
	private void checkCapabilitiesTb1v110(BundleWiring tb1Wiring, Bundle tb4) {
		assertThat(tb1Wiring.getCapabilities(null))
				.as("Wrong number of capabilities").hasSize(9);
		checkCapabilities(
				tb1Wiring.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE),
				tb1Wiring.getCapabilities(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				1, 
				tb1Wiring.getRevision());
		checkCapabilities(
				tb1Wiring.getCapabilities(HostNamespace.HOST_NAMESPACE),
				tb1Wiring.getCapabilities(null), 
 HostNamespace.HOST_NAMESPACE,
				1, 
				tb1Wiring.getRevision());
		checkCapabilities(
				tb1Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE),
				tb1Wiring.getCapabilities(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				1, 
				tb1Wiring.getRevision());
		checkCapabilities(
				tb1Wiring.getCapabilities("test"), 
				tb1Wiring.getCapabilities(null), 
				"test", 
				1, 
				tb1Wiring.getRevision());
		checkCapabilities(
				tb1Wiring.getCapabilities("test.multiple"), 
				tb1Wiring.getCapabilities(null), 
				"test.multiple", 
				2, 
				tb1Wiring.getRevision());
		checkCapabilities(
				tb1Wiring.getCapabilities("test.fragment"), 
				tb1Wiring.getCapabilities(null), 
				"test.fragment", 
				1, 
				tb4.adapt(BundleRevision.class));
		checkCapabilities(
				tb1Wiring.getCapabilities("test.no.attrs"), 
				tb1Wiring.getCapabilities(null), 
				"test.no.attrs", 
				1, 
				tb1Wiring.getRevision());
	}
	
	private void checkCapabilitiesTb2(BundleWiring tb2Wiring) {
		assertThat(tb2Wiring.getCapabilities(null))
				.as("Wrong number of capabilities").hasSize(2);
		checkCapabilities(
				tb2Wiring.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE),
				tb2Wiring.getCapabilities(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				1, 
				tb2Wiring.getRevision());
		checkCapabilities(
				tb2Wiring.getCapabilities(HostNamespace.HOST_NAMESPACE),
				tb2Wiring.getCapabilities(null), 
 HostNamespace.HOST_NAMESPACE,
				0, 
				tb2Wiring.getRevision());
		checkCapabilities(
				tb2Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE),
				tb2Wiring.getCapabilities(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				0, 
				tb2Wiring.getRevision());
	}
	
	private void checkCapabilitiesTb3(BundleWiring tb3Wiring) {
		assertThat(tb3Wiring.getCapabilities(null))
				.as("Wrong number of capabilities").hasSize(3);
		checkCapabilities(
				tb3Wiring.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE),
				tb3Wiring.getCapabilities(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				1, 
				tb3Wiring.getRevision());
		checkCapabilities(
				tb3Wiring.getCapabilities(HostNamespace.HOST_NAMESPACE),
				tb3Wiring.getCapabilities(null), 
 HostNamespace.HOST_NAMESPACE,
				1, 
				tb3Wiring.getRevision());
		checkCapabilities(
				tb3Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getCapabilities(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				0, 
				tb3Wiring.getRevision());
	}
	
	private void checkCapabilitiesTb4(BundleWiring tb4Wiring) {
		assertThat(tb4Wiring.getCapabilities(null))
				.as("Wrong number of capabilities").hasSize(1);
	}
	
	private void checkCapabilitiesTb5(BundleWiring tb5Wiring) {
		assertThat(tb5Wiring.getCapabilities(null))
				.as("Wrong number of capabilities").hasSize(3);
		checkCapabilities(
				tb5Wiring.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE),
				tb5Wiring.getCapabilities(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				1, 
				tb5Wiring.getRevision());
		checkCapabilities(
				tb5Wiring.getCapabilities(HostNamespace.HOST_NAMESPACE),
				tb5Wiring.getCapabilities(null), 
 HostNamespace.HOST_NAMESPACE,
				1, 
				tb5Wiring.getRevision());
		checkCapabilities(
				tb5Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE),
				tb5Wiring.getCapabilities(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				0, 
				tb5Wiring.getRevision());
	}
	
	private void checkCapabilitiesTb14(BundleWiring tb14Wiring) {
		assertThat(tb14Wiring.getCapabilities(null))
				.as("Wrong number of capabilities").hasSize(3);
		checkCapabilities(
				tb14Wiring.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE),
				tb14Wiring.getCapabilities(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				1, 
				tb14Wiring.getRevision());
		checkCapabilities(
				tb14Wiring.getCapabilities(HostNamespace.HOST_NAMESPACE),
				tb14Wiring.getCapabilities(null), 
 HostNamespace.HOST_NAMESPACE,
				1, 
				tb14Wiring.getRevision());
		checkCapabilities(
				tb14Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE),
				tb14Wiring.getCapabilities(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				0, 
				tb14Wiring.getRevision());
	}
	
	private void checkRequirementsTb1v110(BundleWiring tb1Wiring) {
		assertThat(tb1Wiring.getRequirements(null))
				.as("Wrong number of requirements").hasSize(0);
	}
	
	private void checkRequirementsTb2(BundleWiring tb2Wiring) {
		assertThat(tb2Wiring.getRequirements(null))
				.as("Wrong number of requirements").hasSize(2);
		checkRequirements(
				tb2Wiring.getRequirements(PackageNamespace.PACKAGE_NAMESPACE),
				tb2Wiring.getRequirements(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				2, 
				tb2Wiring.getRevision());
	}
	
	private void checkRequirementsTb3(BundleWiring tb3Wiring) {
		assertThat(tb3Wiring.getRequirements(null))
				.as("Wrong number of requirements").hasSize(2);
		checkRequirements(
				tb3Wiring.getRequirements(BundleNamespace.BUNDLE_NAMESPACE),
				tb3Wiring.getRequirements(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				1, 
				tb3Wiring.getRevision());
		checkRequirements(
				tb3Wiring.getRequirements(HostNamespace.HOST_NAMESPACE),
				tb3Wiring.getRequirements(null), 
 HostNamespace.HOST_NAMESPACE,
				0, 
				tb3Wiring.getRevision());
		checkRequirements(
				tb3Wiring.getRequirements(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getRequirements(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				1, 
				tb3Wiring.getRevision());
	}
	
	private void checkRequirementsTb4(BundleWiring tb4Wiring) {
		assertThat(tb4Wiring.getRequirements(null))
				.as("Wrong number of requirements").hasSize(1);
		checkRequirements(
				tb4Wiring.getRequirements(BundleNamespace.BUNDLE_NAMESPACE),
				tb4Wiring.getRequirements(null), 
				BundleNamespace.BUNDLE_NAMESPACE,
				0, 
				tb4Wiring.getRevision());
		checkRequirements(
				tb4Wiring.getRequirements(HostNamespace.HOST_NAMESPACE),
				tb4Wiring.getRequirements(null), 
 HostNamespace.HOST_NAMESPACE,
				1, 
				tb4Wiring.getRevision());
		checkRequirements(
				tb4Wiring.getRequirements(PackageNamespace.PACKAGE_NAMESPACE),
				tb4Wiring.getRequirements(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				0, 
				tb4Wiring.getRevision());
	}
	
	private void checkRequirementsTb5(BundleWiring tb5Wiring) {
		assertThat(tb5Wiring.getRequirements(null))
				.as("Wrong number of requirements").hasSize(11);
		checkRequirements(
				tb5Wiring.getRequirements("test"), 
				tb5Wiring.getRequirements(null), 
				"test", 
				10, 
				tb5Wiring.getRevision());
		checkRequirements(
				tb5Wiring.getRequirements("test.no.attrs"), 
				tb5Wiring.getRequirements(null), 
				"test.no.attrs", 
				1, 
				tb5Wiring.getRevision());
	}
	
	private void checkRequirementsTb14(BundleWiring tb14Wiring) {
		assertThat(tb14Wiring.getRequirements(null))
				.as("Wrong number of requirements").hasSize(1);
		checkRequirements(
				tb14Wiring.getRequirements("test.fragment"), 
				tb14Wiring.getRequirements(null), 
				"test.fragment", 
				1, 
				tb14Wiring.getRevision());
	}

	private void checkBundleWires(
			BundleWiring tb1Wiring, BundleWiring tb2Wiring, BundleWiring tb3Wiring, BundleWiring tb5Wiring, BundleWiring tb14Wiring, 
			Bundle tb4, 
			List<BundleWire> allTb1ProvidedWires, 
			List<BundleWire> osgiBundleTb1ProvidedWires, 
			List<BundleWire> osgiHostTb1ProvidedWires, 
			List<BundleWire> osgiPackageTb1ProvidedWires, 
			List<BundleWire> genTestTb1ProvidedWires, 
			List<BundleWire> genTestMultipleTb1ProvidedWires, 
			List<BundleWire> genTestFragmentTb1ProvidedWires, 
			List<BundleWire> genTestNoAttrsTb1ProvidedWires) {
		assertThat(allTb1ProvidedWires)
				.as("Wrong number of wires").hasSize(15);
		checkWires(osgiBundleTb1ProvidedWires, allTb1ProvidedWires,
				BundleNamespace.BUNDLE_NAMESPACE, 1);
		checkWires(osgiHostTb1ProvidedWires, allTb1ProvidedWires,
				HostNamespace.HOST_NAMESPACE, 1);
		checkWires(osgiPackageTb1ProvidedWires, allTb1ProvidedWires,
				PackageNamespace.PACKAGE_NAMESPACE, 1);
		checkWires(genTestTb1ProvidedWires, allTb1ProvidedWires, "test", 10);
		checkWires(genTestMultipleTb1ProvidedWires, allTb1ProvidedWires, "test.multiple", 0);
		checkWires(genTestFragmentTb1ProvidedWires, allTb1ProvidedWires, "test.fragment", 1);
		checkWires(genTestNoAttrsTb1ProvidedWires, allTb1ProvidedWires, "test.no.attrs", 1);
		
		checkCapabilitiesTb1v110(tb1Wiring, tb4);
		checkCapabilitiesTb2(tb2Wiring);
		checkCapabilitiesTb3(tb3Wiring);
		checkCapabilitiesTb4(tb4.adapt(BundleWiring.class));
		checkCapabilitiesTb5(tb5Wiring);
		checkCapabilitiesTb14(tb14Wiring);
		checkRequirementsTb1v110(tb1Wiring);
		checkRequirementsTb2(tb2Wiring);
		checkRequirementsTb3(tb3Wiring);
		checkRequirementsTb4(tb4.adapt(BundleWiring.class));
		checkRequirementsTb5(tb5Wiring);
		checkRequirementsTb14(tb14Wiring);
		
		checkBundleWire(
				osgiBundleTb1ProvidedWires.get(0), 
				tb1Wiring, 
				tb3Wiring, 
				tb1Wiring.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE)
						.get(0),
				tb3Wiring.getRequirements(BundleNamespace.BUNDLE_NAMESPACE)
						.get(0));
		checkBundleWire(
				osgiHostTb1ProvidedWires.get(0), 
				tb1Wiring, 
				tb4.adapt(BundleWiring.class), 
				tb1Wiring.getCapabilities(HostNamespace.HOST_NAMESPACE).get(0),
				tb4.adapt(BundleWiring.class)
						.getRequirements(HostNamespace.HOST_NAMESPACE).get(0));
		checkBundleWire(
				osgiPackageTb1ProvidedWires.get(0), 
				tb1Wiring, 
				tb2Wiring, 
				tb1Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)
						.get(0),
				tb2Wiring.getRequirements(PackageNamespace.PACKAGE_NAMESPACE)
						.get(1));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(0),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(0));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(1),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(1));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(2),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(2));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(3),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(3));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(4),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(4));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(5),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(5));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(6),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(6));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(7),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(7));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(8),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(8));
		checkBundleWire(
				tb5Wiring.getRequiredWires("test").get(9),
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test").get(0),
				tb5Wiring.getRequirements("test").get(9));
		checkBundleWire(
				genTestNoAttrsTb1ProvidedWires.get(0), 
				tb1Wiring, 
				tb5Wiring, 
				tb1Wiring.getCapabilities("test.no.attrs").get(0),
				tb5Wiring.getRequirements("test.no.attrs").get(0));
		checkBundleWire(
				genTestFragmentTb1ProvidedWires.get(0), 
				tb1Wiring, 
				tb14Wiring, 
				tb1Wiring.getCapabilities("test.fragment").get(0),
				tb14Wiring.getRequirements("test.fragment").get(0));
		
		List<BundleWire> fragments = tb1Wiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertThat(fragments)
				.as("Wrong number of fragments").hasSize(1);
		assertThat(fragments.get(0).getRequirerWiring().getBundle())
				.as("Wrong fragment")
				.isEqualTo(tb4);
	}

	private void checkBundleWire(
			BundleWire wire, 
			BundleWiring provider, 
			BundleWiring requirer, 
			BundleCapability capability,
			BundleRequirement requirement) {
		assertThat(wire.getProvider())
				.as("Wrong provider")
				.isEqualTo(provider.getResource());
		assertThat(wire.getRequirer())
				.as("Wrong requirer")
				.isEqualTo(requirer.getResource());
		assertThat(wire.getProviderWiring())
				.as("Wrong provider wiring")
				.isEqualTo(provider);
		assertThat(wire.getRequirerWiring())
				.as("Wrong requirer wiring")
				.isEqualTo(requirer);
		assertThat(wire.getCapability())
				.as("Wrong capability")
				.isEqualTo(capability);
		assertThat(wire.getRequirement())
				.as("Wrong requirement")
				.isEqualTo(requirement);
		assertThat(wire.getRequirement().matches(wire.getCapability()))
				.as("Requirement does not match capability")
				.isTrue();
		String filterDirective = wire.getRequirement().getDirectives().get(Constants.FILTER_DIRECTIVE);
		if (wire.getRequirement().getNamespace().startsWith("osgi.wiring.")) {
			assertThat(wire.getRequirement().getAttributes())
					.as("An osgi.wiring.* requirement has non-empty attribute map.").isEmpty();
			assertThat(filterDirective)
					.as("Null filter directive is not allowed for osgi.wiring.* name spaces.")
				.isNotNull();
			try {
				Filter filter = FrameworkUtil.createFilter(filterDirective);
				assertThat(filter.matches(wire.getCapability().getAttributes()))
						.as("Filter directive does not match capability attributes: " + filterDirective)
				.isTrue();
			} catch (InvalidSyntaxException e) {
				fail("Failed to create filter: " + filterDirective, e);
			}
		}
	}

	@Test
	public void testGetRevisions() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");
		List<Bundle> testBundles = Arrays.asList(new Bundle[]{tb1, tb2, tb3, tb4, tb5});

		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		BundleRevisions tb1Revisions = tb1.adapt(BundleRevisions.class);
		BundleRevisions tb2Revisions = tb2.adapt(BundleRevisions.class);
		BundleRevisions tb3Revisions = tb3.adapt(BundleRevisions.class);
		BundleRevisions tb4Revisions = tb4.adapt(BundleRevisions.class);
		BundleRevisions tb5Revisions = tb5.adapt(BundleRevisions.class);
		BundleRevisions[] revisions = new BundleRevisions[] {tb1Revisions,
				tb2Revisions, tb3Revisions, tb4Revisions, tb5Revisions};

		checkWirings(testBundles.toArray(new Bundle[testBundles.size()]), revisions, 1, true);

		// test the refresh case
		refreshBundles(Arrays.asList(new Bundle[]{tb1}));
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		// do not reget the BundleRevisions must survive refresh operations
		checkWirings(testBundles.toArray(new Bundle[testBundles.size()]), revisions, 1, true);

		// test the update case
		Bundle tb8 = install("resolver.tb8.jar");
		BundleRevision tb1Revision1 = tb1.adapt(BundleRevision.class);
		URL content = bundleContext.getBundle().getEntry("resolver.tb1.v120.jar");
		assertThat(content)
				.as("Cannot find content for update")
				.isNotNull();
		try {
			tb1.update(content.openStream());
		} catch (BundleException e) {
			fail("Unexpected update failure", e);
		} catch (IOException e) {
			fail("Unexpected update failure", e);
		}
		BundleRevision tb1Revision2 = tb1.adapt(BundleRevision.class);
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb1, tb8})))
				.as("Could not resolve updated bundle")
				.isTrue();
		checkWirings(new Bundle[] {tb1}, new BundleRevisions[] {tb1Revisions},
				2,
				true);
		checkRevisions(tb1Revisions, new BundleRevision[] {tb1Revision2,
				tb1Revision1});

		Bundle tb9 = install("resolver.tb9.jar");
		content = bundleContext.getBundle().getEntry("resolver.tb1.v130.jar");
		assertThat(content)
				.as("Cannot find content for update")
				.isNotNull();
		try {
			tb1.update(content.openStream());
		} catch (BundleException e) {
			fail("Unexpected update failure", e);
		} catch (IOException e) {
			fail("Unexpected update failure", e);
		}
		BundleRevision tb1Revision3 = tb1.adapt(BundleRevision.class);
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb1, tb9})))
				.as("Could not resolve updated bundle")
				.isTrue();
		checkWirings(new Bundle[] {tb1}, new BundleRevisions[] {tb1Revisions},
				3,
				true);
		checkRevisions(tb1Revisions, new BundleRevision[] {tb1Revision3,
				tb1Revision2, tb1Revision1});
		
		// test the refresh case
		refreshBundles(Arrays.asList(new Bundle[]{tb1}));
		assertThat(frameworkWiring.resolveBundles(testBundles))
				.as("Could not resolve test bundles")
				.isTrue();

		// do not reget the BundleRevisions must survive refresh operations
		checkWirings(testBundles.toArray(new Bundle[testBundles.size()]), revisions, 1, true);

		// test uninstall case
		try {
			tb1.uninstall();
		} catch (BundleException e) {
			fail("Unexpected error on uninstall", e);
		}

		// regetting tb1 revisions to test that we can still get it after uninstall
		// this revision will only have 1 revision and it is not current
		tb1Revisions = tb1.adapt(BundleRevisions.class);
		checkWirings(new Bundle[] {tb1}, new BundleRevisions[] {tb1Revisions},
				1,
				false);
		// all other wirings are current and will only have one wiring each
		BundleRevisions[] otherRevisions = new BundleRevisions[] {tb2Revisions,
				tb3Revisions, tb4Revisions, tb5Revisions};
		Bundle[] otherBundes = new Bundle[] {tb2, tb3, tb4, tb5};
		checkWirings(otherBundes, otherRevisions, 1, true);
	}

	private void checkRevisions(BundleRevisions revisions,
			BundleRevision[] bundleRevisions) {
		List<BundleRevision> revisionList = revisions.getRevisions();
		assertThat(revisionList)
				.as("Wrong number of revisions").hasSize(bundleRevisions.length);
		int i = 0;
		for (Iterator<BundleRevision> iRevisions = revisionList.iterator(); iRevisions.hasNext(); i++)
			assertThat(iRevisions.next())
					.as("Wrong revision found")
				.isEqualTo(bundleRevisions[i]);
	}

	private void checkWirings(Bundle[] bundles,
			BundleRevisions[] bundlesRevisions,
			int expectedNumRevisions, boolean hasCurrent) {
		assertThat(bundlesRevisions.length)
				.as("Lists are not the same size")
				.isEqualTo(bundles.length);
		for (int i = 0; i < bundlesRevisions.length; i++) {
			Bundle bundle = bundles[i];
			BundleRevision current = bundle.adapt(BundleRevision.class);
			if (hasCurrent) {
				assertThat(current)
						.as("BundleRevision is null for: " + bundle)
				.isNotNull();
				assertThat(current.getSymbolicName())
						.as("Wrong BSN")
				.isEqualTo(bundle.getSymbolicName());
				assertThat(current.getVersion())
						.as("Wrong version")
				.isEqualTo(bundle.getVersion());
			} else {
				assertThat(current)
						.as("BundleRevision must be null for: " + bundle)
				.isNull();
			}
			BundleRevisions bundleRevisions = bundlesRevisions[i];
			assertThat(bundleRevisions)
					.as("BundleRevisions is null for bundle: " + bundle)
				.isNotNull();
			assertThat(bundleRevisions.getBundle())
					.as("Wrong bundle for revisions")
				.isEqualTo(bundle);
			List<BundleRevision> revisions = bundleRevisions.getRevisions();
			if (hasCurrent)
				assertThat(revisions.get(0))
						.as("Wrong current revision for bundle")
				.isEqualTo(current);
			assertThat(revisions)
					.as("Wrong number of in use revisions").hasSize(expectedNumRevisions);

			int index = 0;
			for (Iterator<BundleRevision> iter = revisions.iterator(); iter.hasNext(); index++) {
				BundleRevision revision = iter.next();
				BundleWiring wiring = revision.getWiring();
				assertThat(wiring)
						.as("bundle wiring is null")
				.isNotNull();
				Collection<BundleWire> hostWires = wiring
						.getProvidedWires(HostNamespace.HOST_NAMESPACE);
				Collection<BundleWire> fragmentWires = wiring
						.getRequiredWires(HostNamespace.HOST_NAMESPACE);
				assertThat(hostWires)
						.as("Host wires is null")
				.isNotNull();
				assertThat(fragmentWires)
						.as("Fragment wires is null")
				.isNotNull();
				if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
					assertThat(hostWires.size())
							.as("Wrong number of host wires")
				.isEqualTo(0);
					assertThat(fragmentWires.size())
							.as("Wrong number of fragment wires")
				.isEqualTo(1);
					BundleWire fragmentWire = fragmentWires.iterator().next();
					assertThat(fragmentWire
									.getProviderWiring()
									.getProvidedWires(
											HostNamespace.HOST_NAMESPACE)
									.contains(fragmentWire))
							.as("Fragment wire not found")
				.isTrue();
					continue;
				}
				assertThat(fragmentWires.size())
						.as("Wrong number of fragment wires")
				.isEqualTo(0);
				if (index == 0 && hasCurrent)
					assertThat(wiring.isCurrent())
							.as("Wiring is not current for: " + bundle)
				.isTrue();
				else
					assertThat(wiring.isCurrent())
							.as("Wiring is current for: " + bundle)
				.isFalse();
				assertThat(wiring.isInUse())
						.as("Wiring is not in use for: " + bundle)
				.isTrue();
			}
		}
	}

	// Note that this test assumes the tests are run on a JavaSE 1.5 vm or higher
	@Test
	public void testOSGiEE() {
		// First bundle tests that the framework sets reasonable defaults for JavaSE 1.5
		Bundle tb10v100 = install("resolver.tb10.v100.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb10v100}))).isTrue();
		// Second bundle requires an osgi.ee that should not be able to resolve
		Bundle tb10v110 = install("resolver.tb10.v110.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb10v110}))).isFalse();
		// Third bundle requires an osgi.ee that is specified by the system.capabilities.extra property
		Bundle tb10v120 = install("resolver.tb10.v120.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb10v120}))).isTrue();
		// forth bundle requires JavaSE [1.3, 1.4)
		Bundle tb10v130 = install("resolver.tb10.v130.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb10v130}))).isFalse();
		// fifth bundle requires JavaSE [1.3, 10000)
		Bundle tb10v140 = install("resolver.tb10.v140.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb10v140}))).isTrue();

		// Test that the ees come from the system bundle
		BundleWiring tb10v100Wiring = tb10v100.adapt(BundleWiring.class);
		assertThat(tb10v100Wiring)
				.as("Wiring is null for: " + tb10v100)
				.isNotNull();
		List<BundleWire> v100RequiredEEs = tb10v100Wiring.getRequiredWires("osgi.ee");
		assertThat(v100RequiredEEs)
				.as("Wrong number of required osgi.ees").hasSize(7);
		Bundle systemBundle = bundleContext.getBundle(0);
		assertThat(systemBundle)
				.as("SystemBundle is null")
				.isNotNull();
		for (Iterator<BundleWire> ees = v100RequiredEEs.iterator(); ees.hasNext();) {
			assertThat(ees.next().getProviderWiring().getBundle())
					.as("Wrong provider for osgi.ee")
				.isEqualTo(systemBundle);
		}

		BundleWiring tb10v120Wiring = tb10v120.adapt(BundleWiring.class);
		assertThat(tb10v120Wiring)
				.as("Wiring is null for: " + tb10v120)
				.isNotNull();
		List<BundleWire> v120RequiredEEs = tb10v120Wiring.getRequiredWires("osgi.ee");
		assertThat(v120RequiredEEs)
				.as("Wrong number of required osgi.ees").hasSize(1);
		assertThat(systemBundle)
				.as("SystemBundle is null")
				.isNotNull();
		for (Iterator<BundleWire> ees = v120RequiredEEs.iterator(); ees.hasNext();) {
			assertThat(ees.next().getProviderWiring().getBundle())
					.as("Wrong provider for osgi.ee")
				.isEqualTo(systemBundle);
		}
	}

	@Test
	public void testRequiredExecutionEnvironment() {
		// install bundles that use BREE header
		Bundle tb1 = install("wiring.ee.tb1.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb1}))).isTrue();
		Bundle tb2 = install("wiring.ee.tb2.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb2}))).isTrue();
		Bundle tb3 = install("wiring.ee.tb3.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb3}))).isTrue();
		Bundle tb4 = install("wiring.ee.tb4.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb4}))).isTrue();
		Bundle tb5 = install("wiring.ee.tb5.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb5}))).isTrue();
		Bundle tb6 = install("wiring.ee.tb6.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb6}))).isTrue();

		checkOSGiEEWiring(tb1, "OSGi/Minimum");
		checkOSGiEEWiring(tb2, "JavaSE");
		checkOSGiEEWiring(tb3, "AA/BB");
		checkOSGiEEWiring(tb4, "EE/FF-YY");
		checkOSGiEEWiring(tb5, "GG-XX/HH");
		checkOSGiEEWiring(tb6, "II-1.0/JJ-2.0");
	}

	private void checkOSGiEEWiring(Bundle tb, String eeName) {
		BundleWiring wiring = tb.adapt(BundleWiring.class);
		assertThat(wiring)
				.as("Null wiring for resolved bundle: " + tb)
				.isNotNull();

		List<BundleWire> eeWires = wiring.getRequiredWires(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		assertThat(eeWires)
				.as("Null ee wires: " + tb)
				.isNotNull();
		assertThat(eeWires)
				.as("Wrong number of ee wires: " + tb).hasSize(1);

		BundleWire eeWire = eeWires.iterator().next();
		BundleCapability eeCapability = eeWire.getCapability();
		assertThat(eeCapability.getNamespace())
				.as("Wrong namespace")
				.isEqualTo(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
		assertThat(eeName)
				.as("Wrong ee name")
				.isEqualTo(eeCapability.getAttributes().get(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE));
	}

	@Test
	public void testOptionalRequireCapability() {
		Bundle tb11 = install("resolver.tb11.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb11}))).isTrue();

		BundleWiring tb11Wiring = tb11.adapt(BundleWiring.class);
		assertThat(tb11Wiring)
				.as("Wiring is null")
				.isNotNull();
		List<BundleRequirement> requirements = tb11Wiring.getRequirements(null);
		assertThat(requirements)
				.as("Requirements is null")
				.isNotNull();
		assertThat(requirements)
				.as("Wrong number of requirements").hasSize(0);
		List<BundleWire> wires = tb11Wiring.getRequiredWires(null);
		assertThat(wires)
				.as("Wires is null")
				.isNotNull();
		assertThat(wires)
				.as("Wrong number of wires").hasSize(0);

		Bundle tb12 = install("resolver.tb12.jar");
		refreshBundles(Arrays.asList(new Bundle[] {tb11}));

		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {tb11, tb12}))).isTrue();
		tb11Wiring = tb11.adapt(BundleWiring.class);
		assertThat(tb11Wiring)
				.as("Wiring is null")
				.isNotNull();
		requirements = tb11Wiring.getRequirements(null);
		assertThat(requirements)
				.as("Requirements is null")
				.isNotNull();
		assertThat(requirements)
				.as("Wrong number of requirements").hasSize(1);
		wires = tb11Wiring.getRequiredWires(null);
		assertThat(wires)
				.as("Wires is null")
				.isNotNull();
		assertThat(wires)
				.as("Wrong number of wires").hasSize(1);
		BundleWire wire = wires.get(0);
		assertThat(wire.getProviderWiring().getBundle())
				.as("Wrong provider")
				.isEqualTo(tb12);
		assertThat(wire.getRequirerWiring().getBundle())
				.as("Wrong requirer")
				.isEqualTo(tb11);
	}

	// Note that this test is done in GetEntryResourceTest
	//	public void testFindEntries() {
	//		fail("Need to write a findEntries test.");
	//	}

	@Test
	public void testListResources() {
		install("wiring.base.jar");
		// force base to resolve first
		assertThat(frameworkWiring.resolveBundles(bundles))
				.as("Could not resolve test bundles")
				.isTrue();

		Bundle exporter = install("wiring.exporter.jar");
		Bundle importer = install("wiring.importer.jar");
		Bundle requirer = install("wiring.requirer.jar");
		install("wiring.reexport.jar");

		assertThat(frameworkWiring.resolveBundles(bundles))
				.as("Could not resolve test bundles")
				.isTrue();

		BundleWiring exporterWiring = exporter.adapt(BundleWiring.class);
		BundleWiring importerWiring = importer.adapt(BundleWiring.class);
		BundleWiring requirerWiring = requirer.adapt(BundleWiring.class);

		// test that empty lists are returned when no resources are found
		Collection<String> empty = exporterWiring.listResources("", "*.notfound",
				BundleWiring.LISTRESOURCES_RECURSE);
		assertThat(empty)
				.as("Should return empty list")
				.isNotNull();
		assertThat(empty)
				.as("Should have 0 resources").hasSize(0);
		empty = importerWiring.listResources("", "*.notfound", BundleWiring.LISTRESOURCES_RECURSE);
		assertThat(empty)
				.as("Should return empty list")
				.isNotNull();
		assertThat(empty)
				.as("Should have 0 resources").hasSize(0);
		empty = requirerWiring.listResources("", "*.notfound", BundleWiring.LISTRESOURCES_RECURSE);
		assertThat(empty)
				.as("Should return empty list")
				.isNotNull();
		assertThat(empty)
				.as("Should have 0 resources").hasSize(0);

		// test exporter resources
		Collection<String> rootResources = exporterWiring.listResources("/root",
				"*.txt", 0);
		assertThat(rootResources)
				.as("Wrong number of resources").hasSize(1);
		assertThat(rootResources
				.iterator().next())
				.as("Wrong resource")
				.isEqualTo("root/root.export.txt");
		checkResources(exporterWiring.getClassLoader(), rootResources);

		// note that root.B package has been substituted
		List<String> expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt", 
				   "root/A/b/b.export.txt", 
				  "root/A/A.export.txt",
				  "root/A/A.reexport.txt",
				   "root/B/a/a.export.txt",
				   "root/B/b/b.export.txt",
				  "root/B/B.base.txt", // this has been substituted
				  "root/C/C.reexport.txt",
				"root/root.export.txt"});
		rootResources = exporterWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(exporterWiring.getClassLoader(), rootResources);

		// test local resources of exporter; note that root.B resources are not available
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt", 
				   "root/A/b/b.export.txt", 
				  "root/A/A.export.txt", 
				   "root/B/a/a.export.txt",
				   "root/B/b/b.export.txt",
				"root/root.export.txt"});
		rootResources = exporterWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(exporterWiring.getClassLoader(), rootResources);

		// test importer resources
		rootResources = importerWiring.listResources("/root", "*.txt", 0);
		assertThat(rootResources)
				.as("Wrong number of resources").hasSize(1);
		assertThat(rootResources
				.iterator().next())
				.as("Wrong resource")
				.isEqualTo("root/root.local.txt");
		checkResources(importerWiring.getClassLoader(), rootResources);

		// note that root.B package has been substituted
		rootResources = importerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.local.txt", 
				   "root/A/b/b.local.txt", 
				  "root/A/A.local.txt", 
				   "root/B/a/a.export.txt",
				   "root/B/b/b.export.txt",
				  "root/B/B.base.txt", // this has been substituted
				"root/root.local.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(importerWiring.getClassLoader(), rootResources);

		// test local resources, anything shadowed by an import must not be included
		rootResources = importerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.local.txt", 
				   "root/A/b/b.local.txt", 
				  "root/A/A.local.txt", 
				"root/root.local.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(importerWiring.getClassLoader(), rootResources);

		// test the require bundle case
		rootResources = requirerWiring.listResources("/root", "*.txt", 0);
		expected = Arrays.asList(new String[] {
				"root/root.export.txt",
				"root/root.local.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(requirerWiring.getClassLoader(), rootResources);

		// test require case; no shadowing of local resources; still have root.B substituted
		rootResources = requirerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt", 
				   "root/A/a/a.local.txt", 
				   "root/A/b/b.export.txt", 
				   "root/A/b/b.local.txt", 
				  "root/A/A.export.txt",
				  "root/A/A.reexport.txt",
				  "root/A/A.local.txt",
				   "root/B/a/a.export.txt",
				   "root/B/a/a.local.txt",
				   "root/B/b/b.export.txt",
				   "root/B/b/b.local.txt",
				  "root/B/B.base.txt", // this has been substituted
				  "root/B/B.local.txt",
				  "root/C/C.reexport.txt",
				  "root/root.export.txt",
				"root/root.local.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(requirerWiring.getClassLoader(), rootResources);

		// test require local resources; not there is no shadowing so we get all local resources
		rootResources = requirerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.local.txt", 
				   "root/A/b/b.local.txt", 
				  "root/A/A.local.txt",
				   "root/B/a/a.local.txt",
				   "root/B/b/b.local.txt",
				  "root/B/B.local.txt",
				"root/root.local.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(requirerWiring.getClassLoader(), rootResources);

		// install fragments to test
		install("wiring.exporter.frag.jar");
		install("wiring.importer.frag.jar");
		install("wiring.requirer.frag.jar");

		refreshBundles(bundles);
		assertThat(frameworkWiring.resolveBundles(bundles))
				.as("failed to resolve test fragments")
				.isTrue();

		// test that old wirings return null
		rootResources = exporterWiring.listResources("/root", "*.txt", 0);
		assertThat(rootResources)
				.as("Old wiring still accesses resources")
				.isNull();
		rootResources = importerWiring.listResources("/root", "*.txt", 0);
		assertThat(rootResources)
				.as("Old wiring still accesses resources")
				.isNull();
		rootResources = requirerWiring.listResources("/root", "*.txt", 0);
		assertThat(rootResources)
				.as("Old wiring still accesses resources")
				.isNull();

		// get the latest wiring
		exporterWiring = exporter.adapt(BundleWiring.class);
		importerWiring = importer.adapt(BundleWiring.class);
		requirerWiring = requirer.adapt(BundleWiring.class);

		// test exporter resources
		expected = Arrays.asList(new String[] {
				"root/root.export.txt",
				"root/root.frag.txt"});
		rootResources = exporterWiring.listResources("/root", "*.txt", 0);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(exporterWiring.getClassLoader(), rootResources);

		// note that root.B package has been substituted
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.export.txt",
				   "root/A/b/b.frag.txt", 
				  "root/A/A.export.txt", 
				  "root/A/A.frag.txt",
				  "root/A/A.reexport.txt",
				   "root/B/a/a.export.txt",
				   "root/B/a/a.frag.txt",
				   "root/B/b/b.export.txt",
				   "root/B/b/b.frag.txt",
				  "root/B/B.base.txt", // this has been substituted
				  "root/C/C.reexport.txt",
			    "root/root.export.txt",
				"root/root.frag.txt"});
		rootResources = exporterWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(exporterWiring.getClassLoader(), rootResources);

		// test local resources of exporter; note that root.B resources are not available
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.export.txt",
				   "root/A/b/b.frag.txt", 
				  "root/A/A.export.txt", 
				  "root/A/A.frag.txt",
				   "root/B/a/a.export.txt",
				   "root/B/a/a.frag.txt",
				   "root/B/b/b.export.txt",
				   "root/B/b/b.frag.txt",
				"root/root.export.txt",
				"root/root.frag.txt"});
		rootResources = exporterWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(exporterWiring.getClassLoader(), rootResources);

		// test importer resources
		expected = Arrays.asList(new String[] {
				"root/root.local.txt",
				"root/root.frag.txt"});
		rootResources = importerWiring.listResources("/root", "*.txt", 0);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(importerWiring.getClassLoader(), rootResources);

		// note that root.B package has been substituted
		rootResources = importerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.local.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.local.txt",
				   "root/A/b/b.frag.txt",
				  "root/A/A.local.txt",
				  "root/A/A.frag.txt", 
				   "root/B/a/a.export.txt", 
				   "root/B/a/a.frag.txt",
				   "root/B/b/b.export.txt",
				   "root/B/b/b.frag.txt",
				  "root/B/B.base.txt", // this has been substituted
				"root/root.local.txt",
				"root/root.frag.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(importerWiring.getClassLoader(), rootResources);

		// test local resources, anything shadowed by an import must not be included
		rootResources = importerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.local.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.local.txt",
				   "root/A/b/b.frag.txt",
				  "root/A/A.local.txt",
				  "root/A/A.frag.txt", 
				"root/root.local.txt",
				"root/root.frag.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(importerWiring.getClassLoader(), rootResources);

		// test the require bundle case
		rootResources = requirerWiring.listResources("/root", "*.txt", 0);
		expected = Arrays.asList(new String[] {
				"root/root.export.txt",
				"root/root.local.txt",
				"root/root.frag.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(requirerWiring.getClassLoader(), rootResources);

		// test require case; no shadowing of local resources; still have root.B substituted
		rootResources = requirerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt", 
				   "root/A/a/a.local.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.export.txt", 
				   "root/A/b/b.local.txt",
				   "root/A/b/b.frag.txt",
				  "root/A/A.export.txt",
				  "root/A/A.reexport.txt",
				  "root/A/A.local.txt",
				  "root/A/A.frag.txt",
				   "root/B/a/a.export.txt",
				   "root/B/a/a.local.txt",
				   "root/B/a/a.frag.txt",
				   "root/B/b/b.export.txt",
				   "root/B/b/b.local.txt",
				   "root/B/b/b.frag.txt",
				  "root/B/B.base.txt", // this has been substituted
				  "root/B/B.local.txt",
				  "root/B/B.frag.txt",
				  "root/C/C.reexport.txt",
				"root/root.export.txt",
				"root/root.local.txt",
				"root/root.frag.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(requirerWiring.getClassLoader(), rootResources);

		// test require local resources; not there is no shadowing so we get all local resources
		rootResources = requirerWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.local.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.local.txt",
				   "root/A/b/b.frag.txt",
				  "root/A/A.local.txt",
				  "root/A/A.frag.txt",
				   "root/B/a/a.local.txt",
				   "root/B/a/a.frag.txt",
				   "root/B/b/b.local.txt",
				   "root/B/b/b.frag.txt",
				  "root/B/B.local.txt",
				  "root/B/B.frag.txt",
				"root/root.local.txt",
				"root/root.frag.txt"
		});
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(requirerWiring.getClassLoader(), rootResources);

		// test update case
		URL updateContent = bundleContext.getBundle().getEntry("wiring.exporter.v2.jar");
		assertThat(updateContent)
				.as("Could not find update content")
				.isNotNull();
		try {
			exporter.update(updateContent.openStream());
		} catch (Exception e) {
			fail("Failed to update bundle", e);
		}
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {exporter})))
				.as("Failed to resolve bundle")
				.isTrue();
		BundleWiring oldExporterWiring = exporterWiring;
		BundleWiring newExporterWiring = exporter.adapt(BundleWiring.class);

		// Do a sanity check to make sure the old wiring still works
		// note that root.B package has been substituted
		// note that the fragment should still be providing content to the old wiring
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt",
				   "root/A/a/a.frag.txt",
				   "root/A/b/b.export.txt",
				   "root/A/b/b.frag.txt", 
				  "root/A/A.export.txt", 
				  "root/A/A.frag.txt",
				  "root/A/A.reexport.txt",
				   "root/B/a/a.export.txt",
				   "root/B/a/a.frag.txt",
				   "root/B/b/b.export.txt",
				   "root/B/b/b.frag.txt",
				  "root/B/B.base.txt", // this has been substituted
				  "root/C/C.reexport.txt",
			    "root/root.export.txt",
				"root/root.frag.txt"});
		rootResources = oldExporterWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(oldExporterWiring.getClassLoader(), rootResources);

		// check the new wiring; no fragment attached
		// note that root.B package has been substituted
		expected = Arrays.asList(new String[] {
				   "root/A/a/a.export.txt", 
				   "root/A/b/b.export.txt", 
				  "root/A/A.export.txt",
				  "root/A/A.reexport.txt",
				   "root/B/a/a.export.txt",
				   "root/B/b/b.export.txt",
				  "root/B/B.base.txt", // this has been substituted
				  "root/C/C.reexport.txt",
				"root/root.export.txt"});
		rootResources = newExporterWiring.listResources("/root", "*.txt", BundleWiring.LISTRESOURCES_RECURSE);
		assertResourcesEquals("Wrong resources", expected, rootResources);
		checkResources(newExporterWiring.getClassLoader(), rootResources);
	}

	@Test
	public void testBSNMatchingAttributes() {
		Bundle tb13a = install("resolver.tb13a.jar");
		Bundle tb13b = install("resolver.tb13b.jar");
		Bundle tb13Client1 = install("resolver.tb13.client1.jar");
		Bundle tb13Client2 = install("resolver.tb13.client2.jar");
		Bundle tb13Client3 = install("resolver.tb13.client3.jar");
		Bundle tb13Client4 = install("resolver.tb13.client4.jar");
		Bundle tb13Frag1 = install("resolver.tb13.frag1.jar");
		Bundle tb13Frag2 = install("resolver.tb13.frag2.jar");
		Bundle tb13Frag3 = install("resolver.tb13.frag3.jar");
		Bundle tb13Frag4 = install("resolver.tb13.frag4.jar");

		assertThat(frameworkWiring.resolveBundles(bundles)).isFalse();

		assertThat(tb13a)
				.as("Unexpected state for: " + tb13a.getSymbolicName()).isInState(Bundle.RESOLVED);
		assertThat(tb13b)
				.as("Unexpected state for: " + tb13b.getSymbolicName()).isInState(Bundle.RESOLVED);
		assertThat(tb13Client1)
				.as("Unexpected state for: " + tb13Client1.getSymbolicName()).isInState(Bundle.INSTALLED);
		assertThat(tb13Client2)
				.as("Unexpected state for: " + tb13Client2.getSymbolicName()).isInState(Bundle.RESOLVED);
		assertThat(tb13Client3)
				.as("Unexpected state for: " + tb13Client3.getSymbolicName()).isInState(Bundle.INSTALLED);
		assertThat(tb13Client4)
				.as("Unexpected state for: " + tb13Client4.getSymbolicName()).isInState(Bundle.RESOLVED);
		assertThat(tb13Frag1)
				.as("Unexpected state for: " + tb13Frag1.getSymbolicName()).isInState(Bundle.INSTALLED);
		assertThat(tb13Frag2)
				.as("Unexpected state for: " + tb13Frag2.getSymbolicName()).isInState(Bundle.RESOLVED);
		assertThat(tb13Frag3)
				.as("Unexpected state for: " + tb13Frag3.getSymbolicName()).isInState(Bundle.INSTALLED);
		assertThat(tb13Frag4)
				.as("Unexpected state for: " + tb13Frag4.getSymbolicName()).isInState(Bundle.RESOLVED);

		BundleWiring tb13Client1Wiring = tb13Client1.adapt(BundleWiring.class);
		assertThat(tb13Client1Wiring)
				.as("Expected null Wiring: " + tb13Client1.getSymbolicName())
				.isNull();
		BundleWiring tb13Client3Wiring = tb13Client3.adapt(BundleWiring.class);
		assertThat(tb13Client3Wiring)
				.as("Expected null Wiring: " + tb13Client3.getSymbolicName())
				.isNull();

		BundleWiring tb13aWiring = tb13a.adapt(BundleWiring.class);
		assertThat(tb13aWiring)
				.as("Expected non-null wiring: " + tb13a.getSymbolicName())
				.isNotNull();
		BundleWiring tb13bWiring = tb13b.adapt(BundleWiring.class);
		assertThat(tb13bWiring)
				.as("Expected non-null wiring: " + tb13b.getSymbolicName())
				.isNotNull();
		BundleWiring tb13Client2Wiring = tb13Client2.adapt(BundleWiring.class);
		assertThat(tb13Client2Wiring)
				.as("Expected non-null wiring: " + tb13Client2.getSymbolicName())
				.isNotNull();
		BundleWiring tb13Client4Wiring = tb13Client4.adapt(BundleWiring.class);
		assertThat(tb13Client4Wiring)
				.as("Expected non-null wiring: " + tb13Client4.getSymbolicName())
				.isNotNull();
		
		List<BundleRequirement> client2Requirements = tb13Client2Wiring
				.getRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		assertThat(client2Requirements)
				.as("Unexpected number of requirements").hasSize(1);
		assertThat(client2Requirements.get(0).getRevision().getBundle())
				.as("Wrong provider")
				.isEqualTo(tb13Client2);
		
		List<BundleWire> client2RequiredWires = tb13Client2Wiring
				.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
		assertThat(client2RequiredWires)
				.as("Unexpected number of wires").hasSize(1);
		assertThat(client2RequiredWires.get(0).getProviderWiring().getBundle())
				.as("Wrong provider")
				.isEqualTo(tb13a);

		List<BundleWire> client4RequiredWires = tb13Client4Wiring
				.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
		assertThat(client4RequiredWires)
				.as("Unexpected number of wires").hasSize(1);
		assertThat(client4RequiredWires.get(0).getProviderWiring().getBundle())
				.as("Wrong provider")
				.isEqualTo(tb13b);
		
		List<BundleWire> tb13aProvidedWires = tb13aWiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertThat(tb13aProvidedWires)
				.as("Unexpected number of wires").hasSize(1);
		assertThat(tb13aProvidedWires.get(0).getRequirerWiring().getBundle())
				.as("Wrong fragment attached")
				.isEqualTo(tb13Frag2);
		
		BundleWiring tb13Frag2Wiring = tb13Frag2.adapt(BundleWiring.class);
		assertThat(tb13Frag2Wiring)
				.as("Fragments must be adaptable to BundleWiring")
				.isNotNull();
		List<BundleWire> tb13Frag2RequiredWires = tb13Frag2Wiring
				.getRequiredWires(HostNamespace.HOST_NAMESPACE);
		assertThat(tb13Frag2RequiredWires)
				.as("Unexpected number of wires").hasSize(1);
		assertThat(tb13Frag2RequiredWires.get(0).getProviderWiring().getBundle())
				.as("Wrong host attached")
				.isEqualTo(tb13a);
		
		List<BundleWire> tb13bProvidedWires = tb13bWiring
				.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		assertThat(tb13bProvidedWires)
				.as("Unexpected number of wires").hasSize(1);
		assertThat(tb13bProvidedWires.get(0).getRequirerWiring().getBundle())
				.as("Wrong fragment attached")
				.isEqualTo(tb13Frag4);
		
		BundleWiring tb13Frag4Wiring = tb13Frag4.adapt(BundleWiring.class);
		assertThat(tb13Frag2Wiring)
				.as("Fragments must be adaptable to BundleWiring")
				.isNotNull();
		List<BundleWire> tb13Frag4RequiredWires = tb13Frag4Wiring
				.getRequiredWires(HostNamespace.HOST_NAMESPACE);
		assertThat(tb13Frag4RequiredWires)
				.as("Unexpected number of wires").hasSize(1);
		assertThat(tb13Frag4RequiredWires.get(0).getProviderWiring().getBundle())
				.as("Wrong host attached")
				.isEqualTo(tb13b);
	}

	private void assertResourcesEquals(String message, Collection<?> expected, Collection<?> actual) {
		if (expected.size() != actual.size())
			fail(message + ": Collections are not the same size: " + expected + ":  " + actual);
		assertThat(actual.containsAll(expected))
				.as(message + ": Colections do not contain the same content: " + expected + ":  " + actual)
				.isTrue();
	}

	private void checkResources(ClassLoader cl, Collection<String> resources) {
		for(Iterator<String> iResources = resources.iterator(); iResources.hasNext();) {
			String path = iResources.next();
			URL resource = cl.getResource(path);
			assertThat(resource)
					.as("Could not find resource: " + path)
				.isNotNull();
		}
	}

	/**
	 * Ensures an implementation delivers a bundle wiring's provided wires in
	 * the proper order. The ordering rules are as follows.
	 * 
	 * (1) For a given name space, the list contains the wires in the order the
	 * capabilities were specified in the manifests of the bundle revision and
	 * the attached fragments of this bundle wiring.
	 * 
	 * (2) There is no ordering defined between wires in different namespaces.
	 * 
	 * (3) There is no ordering defined between multiple wires for the same
	 * capability, but the wires must be contiguous, and the group must be
	 * ordered as in (1).
	 */
	@Test
	public void testProvidedWiresOrdering() {
		Bundle tb1 = install("wiring.tb1.jar");
		Bundle tb2 = install("wiring.tb2.jar");
		Bundle tb3 = install("wiring.tb3.jar");
		Bundle tb4 = install("wiring.tb4.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb1,tb2,tb3,tb4})))
				.as("Bundles should have resolved")
				.isTrue();
		BundleWiring tb1Wiring = tb1.adapt(BundleWiring.class);
		BundleWiring tb2Wiring = tb2.adapt(BundleWiring.class);
		BundleWiring tb3Wiring = tb3.adapt(BundleWiring.class);
		BundleWiring tb4Wiring = tb4.adapt(BundleWiring.class);
		List<BundleWire> tb1Wires = tb1Wiring
				.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(tb1Wires.size())
				.as("Wrong number of wires")
				.isEqualTo(6);
		assertThat(tb1Wires.get(0).getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong order")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1a");
		assertThat(tb1Wires.get(0).getRequirerWiring().equals(tb2Wiring) || tb1Wires.get(0).getRequirerWiring().equals(tb3Wiring))
				.as("Wrong requirer")
				.isTrue();
		assertThat(tb1Wires.get(1).getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong order")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1a");
		assertThat(tb1Wires.get(1).getRequirerWiring().equals(tb2Wiring) || tb1Wires.get(1).getRequirerWiring().equals(tb3Wiring))
				.as("Wrong requirer")
				.isTrue();
		assertThat(tb1Wires.get(2).getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong order")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1b");
		assertThat(tb1Wires.get(2).getRequirerWiring().equals(tb2Wiring) || tb1Wires.get(2).getRequirerWiring().equals(tb4Wiring))
				.as("Wrong requirer")
				.isTrue();
		assertThat(tb1Wires.get(3).getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong order")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1b");
		assertThat(tb1Wires.get(3).getRequirerWiring().equals(tb2Wiring) || tb1Wires.get(3).getRequirerWiring().equals(tb4Wiring))
				.as("Wrong requirer")
				.isTrue();
		assertThat(tb1Wires.get(4).getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong order")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1c");
		assertThat(tb1Wires.get(4).getRequirerWiring().equals(tb3Wiring) || tb1Wires.get(4).getRequirerWiring().equals(tb4Wiring))
				.as("Wrong requirer")
				.isTrue();
		assertThat(tb1Wires.get(5).getCapability().getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong order")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1c");
		assertThat(tb1Wires.get(5).getRequirerWiring().equals(tb3Wiring) || tb1Wires.get(5).getRequirerWiring().equals(tb4Wiring))
				.as("Wrong requirer")
				.isTrue();
	}

	/**
	 * Basic test for support of the DynamicImport-Package requirement.
	 */
	@Test
	public void testDynamicImportPackage() throws Exception {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("wiring.tb1.jar");
		Bundle tb3 = install("wiring.tb5.jar");
		
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb1,tb2,tb3})))
				.as("The bundles should have resolved")
				.isTrue();

		BundleRevision tb3Revision = tb3.adapt(BundleRevision.class);
		
		BundleWiring tb1Wiring = tb1.adapt(BundleWiring.class);
		BundleWiring tb2Wiring = tb2.adapt(BundleWiring.class);
		BundleWiring tb3Wiring = tb3.adapt(BundleWiring.class);
		
		checkRequirements(
				tb3Revision
						.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Revision.getDeclaredRequirements(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				1, 
				tb3Revision);
		checkRequirements(
				tb3Wiring.getRequirements(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getRequirements(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				1, 
				tb3Wiring.getRevision());
		checkWires(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getRequiredWires(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				0);
		
		tb3.loadClass("org.osgi.test.cases.framework.resolver.tb1.Test");
		checkWires(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getRequiredWires(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				1);
		checkBundleWire(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)
						.get(0),
				tb1Wiring,
				tb3Wiring,
 tb1Wiring
						.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)
						.get(0),
				tb3Wiring.getRequirements(PackageNamespace.PACKAGE_NAMESPACE)
						.get(0));
		
		tb3.loadClass("org.osgi.test.cases.framework.wiring.tb1a.PlaceHolder");
		checkWires(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getRequiredWires(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				2);
		BundleCapability bc = tb2Wiring.getCapabilities(
				PackageNamespace.PACKAGE_NAMESPACE).get(0);
		assertThat(bc.getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong attribute")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1a");
		checkBundleWire(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)
						.get(1),
				tb2Wiring,
				tb3Wiring,
				bc,
 tb3Wiring
						.getRequirements(PackageNamespace.PACKAGE_NAMESPACE)
						.get(0));
		
		tb3.loadClass("org.osgi.test.cases.framework.wiring.tb1b.PlaceHolder");
		checkWires(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE),
				tb3Wiring.getRequiredWires(null), 
				PackageNamespace.PACKAGE_NAMESPACE,
				3);
		bc = tb2Wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE).get(
				1);
		assertThat(bc.getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))
				.as("Wrong attribute")
				.isEqualTo("org.osgi.test.cases.framework.wiring.tb1b");
		checkBundleWire(
				tb3Wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)
						.get(2),
				tb2Wiring,
				tb3Wiring,
				bc,
 tb3Wiring
						.getRequirements(PackageNamespace.PACKAGE_NAMESPACE)
						.get(0));
		
		
		
	}
}
