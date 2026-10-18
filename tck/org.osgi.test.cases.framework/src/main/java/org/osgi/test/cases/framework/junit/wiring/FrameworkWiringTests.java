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
import static org.osgi.test.cases.framework.junit.tck.TckProperties.getScaling;
import static org.osgi.test.cases.framework.junit.tck.TckProperties.getTimeout;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.test.common.wiring.FrameworkWirings;

public class FrameworkWiringTests extends WiringTest {
	@AfterEach
	public void tearDown() throws Exception {
		for (Iterator<Bundle> iBundles = bundles.iterator(); iBundles.hasNext();)
			try {
				iBundles.next().uninstall();
			} catch (BundleException e) {
				// nothing
			} catch (IllegalStateException e) {
				// happens if the test uninstalls the bundle itself
			}
		FrameworkWirings.refreshBundles(bundleContext, bundles);
		bundles.clear();
	}

	@Test
	public void testRefreshListeners() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");

		Collection<Bundle> testBundles = Arrays.asList(new Bundle[]{tb1, tb2, tb3, tb4, tb5});

		final boolean[] called = new boolean[] {false};
		final AssertionFailedError error[] = new AssertionFailedError[1];
		frameworkWiring.refreshBundles(testBundles, new FrameworkListener[] { 
				new FrameworkListener() {
					public void frameworkEvent(FrameworkEvent event) {
						synchronized (called) {
							try {
								called[0] = true;
								called.notify();
								assertThat(event.getType())
										.as("Unexpected event type")
				.isEqualTo(FrameworkEvent.PACKAGES_REFRESHED);
							} catch (AssertionFailedError e) {
								if (error[0] == null)
									error[0] = e;
							}
						}
					}
				}
			}
		);
		final long endTime = System.currentTimeMillis() + getTimeout()
				* getScaling();
		synchronized (called) {
			while (!called[0])
				try {
					called.wait(endTime - System.currentTimeMillis());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					fail("Unexepected interruption.", e);
				}
			if (!called[0] && (System.currentTimeMillis() > endTime)) {
				fail("Failed to call specified listener");
			}
			if (error[0] != null)
				throw error[0];
		}
	}

	@Test
	public void testResolveBundles() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");

		Collection<Bundle> testBundles = Arrays.asList(new Bundle[]{tb1, tb2, tb3, tb4, tb5});
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		assertThat(tb1)
				.as("Wrong state for bundle: " + tb1).isInState(Bundle.RESOLVED);
		assertThat(tb2)
				.as("Wrong state for bundle: " + tb2).isInState(Bundle.RESOLVED);
		assertThat(tb3)
				.as("Wrong state for bundle: " + tb3).isInState(Bundle.RESOLVED);
		assertThat(tb4)
				.as("Wrong state for bundle: " + tb4).isInState(Bundle.RESOLVED);
		assertThat(tb5)
				.as("Wrong state for bundle: " + tb5).isInState(Bundle.RESOLVED);
	}

	@Test
	public void testDependencyClosure() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");

		Collection<Bundle> testBundles = Arrays.asList(new Bundle[]{tb1, tb2, tb3, tb4, tb5});
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		Collection<Bundle> closure = frameworkWiring.getDependencyClosure(Arrays.asList(new Bundle[]{tb1}));
		assertThat(closure.size())
				.as("Wrong number in closure")
				.isEqualTo(5);
		assertThat(closure.containsAll(testBundles))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();
		assertThat(testBundles.containsAll(closure))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();

		closure = frameworkWiring.getDependencyClosure(Arrays.asList(new Bundle[]{tb2}));
		assertThat(closure.size())
				.as("Wrong number in closure")
				.isEqualTo(1);
		assertThat(closure.contains(tb2))
				.as("Wrong bundle in closure")
				.isTrue();

		// Test fragment, this pulls in the host which pulls in everything else
		closure = frameworkWiring.getDependencyClosure(Arrays.asList(new Bundle[]{tb4}));
		assertThat(closure.size())
				.as("Wrong number in closure")
				.isEqualTo(5);
		assertThat(closure.containsAll(testBundles))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();
		assertThat(testBundles.containsAll(closure))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();
	}

	@Test
	public void testRemovalPending() {
		Bundle tb1 = install("resolver.tb1.v110.jar");
		Bundle tb2 = install("resolver.tb2.jar");
		Bundle tb3 = install("resolver.tb3.jar");
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb5 = install("resolver.tb5.jar");

		Collection<Bundle> testBundles = Arrays.asList(new Bundle[]{tb1, tb2, tb3, tb4, tb5});
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		// TODO should investigate why other tests are not cleaning up their uninstalled bundles
		// for now doing a refresh of all removal pending to ensure a clean state
		FrameworkWirings.refreshBundles(bundleContext);
		// Need to make sure there are no removal pendings already present.
		Collection<Bundle> removals = frameworkWiring.getRemovalPendingBundles();
		assertThat(removals)
				.as("Removal pendings are left over: " + removals.toString()).hasSize(0);
		try {
			tb1.uninstall();
		} catch (BundleException e) {
			fail("Failed to uninstall bundle", e);
		}

		removals = frameworkWiring.getRemovalPendingBundles();
		assertThat(removals.size())
				.as("Wrong number of removals")
				.isEqualTo(1);
		assertThat(removals.contains(tb1))
				.as("Wrong bundle in removals")
				.isTrue();

		Collection<Bundle> closure = frameworkWiring.getDependencyClosure(removals);
		assertThat(closure.size())
				.as("Wrong number in closure")
				.isEqualTo(5);
		assertThat(closure.containsAll(testBundles))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();
		assertThat(testBundles.containsAll(closure))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();

		try {
			tb4.uninstall();
		} catch (BundleException e) {
			fail("Failed to uninstall bundle", e);
		}

		removals = frameworkWiring.getRemovalPendingBundles();
		assertThat(removals.size())
				.as("Wrong number of removals")
				.isEqualTo(2);
		assertThat(removals.contains(tb1))
				.as("Wrong bundle in removals")
				.isTrue();
		assertThat(removals.contains(tb4))
				.as("Wrong bundle in removals")
				.isTrue();

		closure = frameworkWiring.getDependencyClosure(removals);
		assertThat(closure.size())
				.as("Wrong number in closure")
				.isEqualTo(5);
		assertThat(closure.containsAll(testBundles))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();
		assertThat(testBundles.containsAll(closure))
				.as("Wrong bundles in closure: " + closure)
				.isTrue();
	}

	@Test
	public void testFindResources() {
		Bundle tb4 = install("resolver.tb4.jar");
		Bundle tb1 = install("resolver.tb1.v110.jar");

		BundleRevision systemRevision = bundleContext.getBundle(
				Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleRevision.class);

		// Get the system bundle identity, not that the alias system.bundle
		// cannot
		// be used here. The real symbolic name of the system bundle revision
		// must be used
		Collection<BundleCapability> systemIdentities = findProviders(
				IdentityNamespace.IDENTITY_NAMESPACE,
				systemRevision.getSymbolicName());
		assertThat(systemIdentities)
				.as("Wrong number of system identity capabilities.").hasSize(1);
		BundleCapability systemIdentity = systemIdentities.iterator().next();
		assertThat(systemIdentity.getRevision())
				.as("Wrong provider.")
				.isEqualTo(systemRevision);

		// Use a BundleRequirement to look up capabilities.
		BundleRevision tb1Revision = tb1.adapt(BundleRevision.class);
		BundleRevision tb4Revision = tb4.adapt(BundleRevision.class);
		List<BundleRequirement> hostReqs = tb4Revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		assertThat(hostReqs)
				.as("Wrong number of host reqs.").hasSize(1);
		Collection<BundleCapability> hostCaps = frameworkWiring
				.findProviders(hostReqs.get(0));
		assertThat(hostCaps)
				.as("Wrong number of host capabilities.").hasSize(1);
		assertThat(hostCaps.iterator().next()
				.getRevision())
				.as("Wrong provider.")
				.isEqualTo(tb1Revision);

		// Make sure non effective capabilities are returned
		Collection<BundleCapability> nonEffective = findProviders(
				"test.effective", null);
		assertThat(nonEffective)
				.as("Wrong number of capabilities.").hasSize(1);
		assertThat(nonEffective.iterator()
				.next().getRevision())
				.as("Wrong provider.")
				.isEqualTo(tb1Revision);

		// Search for a package
		Collection<BundleCapability> packageCaps = findProviders(
				PackageNamespace.PACKAGE_NAMESPACE,
				"org.osgi.test.cases.framework.resolver.tb1");
		assertThat(packageCaps)
				.as("Wrong number of capabilities.").hasSize(1);
		assertThat(packageCaps.iterator()
				.next().getRevision())
				.as("Wrong provider.")
				.isEqualTo(tb1Revision);

		// Search for a capability from a fragment
		Collection<BundleCapability> fragmentCaps = findProviders(
				"test.fragment", null);
		assertThat(fragmentCaps)
				.as("Wrong number of capabilities.").hasSize(1);
		assertThat(fragmentCaps.iterator()
				.next().getRevision())
				.as("Wrong provider.")
				.isEqualTo(tb4Revision);

		// Search for capabilities that have multiple matches
		Collection<BundleCapability> multipleCaps = findProviders(
				"test.multiple", null);
		assertThat(multipleCaps)
				.as("Wrong number of capabilities.").hasSize(2);
		for (BundleCapability multipleCap : multipleCaps) {
			assertThat(multipleCap.getRevision())
					.as("Wrong provider.")
				.isEqualTo(tb1Revision);
		}

		// Use wild card matching to search for identity
		Collection<BundleCapability> multipleIdentities = findProviders(
				IdentityNamespace.IDENTITY_NAMESPACE,
				"org.osgi.test.cases.framework.resolver.*");
		assertThat(multipleIdentities)
				.as("Wrong number of capabilities.").hasSize(2);
		boolean foundTb1 = false;
		boolean foundTb4 = false;
		for (BundleCapability identity : multipleIdentities) {
			foundTb1 |= tb1Revision.equals(identity.getRevision());
			foundTb4 |= tb4Revision.equals(identity.getRevision());
		}
		assertThat(foundTb1)
				.as("Did not find tb1 identity.")
				.isTrue();
		assertThat(foundTb4)
				.as("Did not find tb4 identity.")
				.isTrue();
	}

	private Collection<BundleCapability> findProviders(String namespace,
			String namespaceValue) {
		String filter = namespaceValue == null ? null : "(" + namespace + "="
				+ namespaceValue + ")";
		return frameworkWiring.findProviders(new TestRequirement(namespace,
				filter));
	}
	static class TestRequirement implements Requirement {
		private final String namespace;
		private final Map<String, String> directives;
		private final Map<String,Object>	attributes	= Collections
				.emptyMap();

		public TestRequirement(String namespace, String filter) {
			this.namespace = namespace;
			if (filter != null) {
				this.directives = Collections.singletonMap(
						Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
			} else {
				this.directives = Collections.emptyMap();
			}
		}

		public String getNamespace() {
			return this.namespace;
		}

		public Map<String, String> getDirectives() {
			return directives;
		}

		public Map<String, Object> getAttributes() {
			return attributes;
		}

		public Resource getResource() {
			// returning null because this is a synthetic requirement
			return null;
		}

	}
}
