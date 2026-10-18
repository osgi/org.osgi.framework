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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

/**
 * This test implements the conclusions of CPEG Bug 1922.
 * <p/>
 * <ul>
 * <li>Attributes declared on {@link Constants#REQUIRE_CAPABILITY} must be 
 *     visible in {@link BundleRequirement#getAttributes()}.</li>
 * <li>Attributes declared on requirements within the osgi.wiring.* namespace
 *     must not be visible in {@link BundleRequirement#getAttributes()}. The
 *     attributes map must be empty.</li>
 * <li>Attributes declared on requirements within the osgi.wiring.* namespace
 *     must be used to generate a {@link Constants#FILTER_DIRECTIVE} visible in
 *     {@link BundleRequirement#getDirectives()}. Any existing {@link
 *     Constants#FILTER_DIRECTIVE} must be overwritten.</li>
 * <li>Directives declared on {@link Constants#REQUIRE_CAPABILITY} must be
 *     visible in {@link BundleRequirement#getDirectives()}.
 * <li>Directives declared within the osgi.wiring.* namespace must be visible
 *     in {@link BundleRequirement#getDirectives()}.</li>
 * </ul>
 * @see https://www.osgi.org/members/bugzilla/show_bug.cgi?id=1922
 */
public class Bug1922Test extends WiringTest {
	private BundleCapability hostCapability;
	private BundleRequirement bundleRequirement;
	private BundleRequirement hostRequirement;
	private BundleRequirement packageRequirement;
	private BundleRequirement requirement1;
	private BundleRequirement requirement2;

	@Test
	public void testRequireCapabilityRequirementAttributes() {
		assertAttribute("foo", "bar", requirement1);
		assertAttribute("bar", "foo", requirement1);
		assertAttribute("foo", "bar", requirement2);
		assertAttribute("bar", "foo", requirement2);
	}

	@Test
	public void testOsgiWiringHostCapabilityAttributes() {
		assertAttribute("foo", "bar", hostCapability);
		assertAttribute("bar", "foo", hostCapability);
	}

	@Test
	public void testOsgiWiringRequirementAttributes() {
		assertEmptyAttributes(bundleRequirement.getAttributes());
		assertEmptyAttributes(hostRequirement.getAttributes());
		assertEmptyAttributes(packageRequirement.getAttributes());
	}

	@Test
	public void testOsgiWiringRequirementFilter() {
		String filter = bundleRequirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
		Map<String,Object> attributes = new HashMap<String,Object>();
		attributes.put(BundleNamespace.BUNDLE_NAMESPACE, "com.acme.launchpad");
		attributes.put("bundle-version", new Version("1.0"));
		attributes.put("foo", "bar");
		attributes.put("x", "x");
		assertFilter(filter, attributes);
		filter = hostRequirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
		attributes.put(HostNamespace.HOST_NAMESPACE,
				"org.osgi.test.cases.framework.wiring.1922");
		assertFilter(filter, attributes);
		filter = packageRequirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
		attributes.put(PackageNamespace.PACKAGE_NAMESPACE,
				"com.acme.rocket.engine");
		attributes.put("version", new Version("1.0"));
		attributes.put("bundle-symbolic-name", "com.acme.rocket");
		assertFilter(filter, attributes);
	}

	@Test
	public void testRequireCapabilityRequirementDirectives() {
		String filter = requirement1.getDirectives().get(Constants.FILTER_DIRECTIVE);
		Map<String,Object> attributes = new HashMap<String,Object>();
		attributes.put("foo", "bar");
		attributes.put("bar", "foo");
		assertFilter(filter, attributes);
		assertDirective("resolution", "optional", requirement1);
		assertDirective("foo", "bar", requirement1);
		assertDirective(Constants.FILTER_DIRECTIVE, null, requirement2);
		assertDirective("resolution", "optional", requirement2);
	}

	@Test
	public void testOsgiWiringRequirementDirectives() {
		assertDirective("visibility", "reexport", bundleRequirement);
		assertDirective("resolution", "optional", bundleRequirement);
		assertDirective("foo", "bar", bundleRequirement);
		assertDirective("foo", "bar", hostRequirement);
		assertDirective("resolution", "optional", packageRequirement);
		assertDirective("foo", "bar", packageRequirement);
	}

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		Bundle tb1 = install("wiring.1922.jar");
		Bundle tb1Frag = install("wiring.1922.frag.jar");
		assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[]{tb1,tb1Frag})))
				.as("Bundles should have resolved")
				.isTrue();
		BundleRevision revision = tb1.adapt(BundleRevision.class);
		List<BundleCapability> capabilities = revision
				.getDeclaredCapabilities(HostNamespace.HOST_NAMESPACE);
		assertThat(capabilities)
				.as("One Fragment-Host capability should exist").hasSize(1);
		hostCapability = capabilities.get(0);
		List<BundleRequirement> requirements = revision
				.getDeclaredRequirements(BundleNamespace.BUNDLE_NAMESPACE);
		assertThat(requirements)
				.as("One Require-Bundle requirement should exist").hasSize(1);
		bundleRequirement = requirements.get(0);
		requirements = revision
				.getDeclaredRequirements(PackageNamespace.PACKAGE_NAMESPACE);
		assertThat(requirements)
				.as("One Import-Package requirement should exist").hasSize(1);
		packageRequirement = requirements.get(0);
		requirements = revision.getDeclaredRequirements("com.acme.countdown");
		assertThat(requirements)
				.as("One com.acme.countdown requirement should exist").hasSize(1);
		requirement1 = requirements.get(0);
		requirements = revision.getDeclaredRequirements("com.acme.lifesupport");
		assertThat(requirements)
				.as("One com.acme.lifesupport requirement should exist").hasSize(1);
		requirement2 = requirements.get(0);
		revision = tb1Frag.adapt(BundleRevision.class);
		requirements = revision
				.getDeclaredRequirements(HostNamespace.HOST_NAMESPACE);
		assertThat(requirements)
				.as("One Fragment-Host requirement should exist").hasSize(1);
		hostRequirement = requirements.get(0);
	}
	
	private void assertAttribute(String name, String value, BundleCapability capability) {
		assertThat(capability.getAttributes().get(name))
				.as("Missing attribute or wrong value")
				.isEqualTo(value);
	}
	
	private void assertAttribute(String name, String value, BundleRequirement requirement) {
		assertThat(requirement.getAttributes().get(name))
				.as("Missing attribute or wrong value")
				.isEqualTo(value);
	}
	
	private void assertDirective(String name, String value, BundleRequirement requirement) {
		// value may be null: the directive must then be absent
		assertThat(requirement.getDirectives().get(name))
				.as("Missing directive or wrong value")
				.isEqualTo(value);
	}
	
	private void assertEmptyAttributes(Map<String, Object> attributes) {
		assertThat(attributes)
				.as("Requirement attributes must be empty for osgi.wiring.* namespace").hasSize(0);
	}
	
	private void assertFilter(String filterStr, Map<String,Object> attributes) {
		try {
			Filter filter = FrameworkUtil.createFilter(filterStr);
			assertThat(filter.matches(attributes))
					.as("The generated filter must match the attributes")
				.isTrue();
		}
		catch (InvalidSyntaxException e) {
			fail("The generated filter string must be valid", e);
		}
	}
}
