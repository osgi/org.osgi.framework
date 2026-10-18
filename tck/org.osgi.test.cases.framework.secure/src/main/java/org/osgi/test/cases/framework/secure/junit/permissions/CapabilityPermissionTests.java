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

package org.osgi.test.cases.framework.secure.junit.permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.security.AllPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

public class CapabilityPermissionTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	private String testName;

	/** The name of the running test method, as TestCase.getName() gave it. */
	private String getName() {
		return testName;
	}
	private static final PermissionInfo[] allPermissions = new PermissionInfo[] {new PermissionInfo(AllPermission.class.getName(), "*", "*")};
	private final List<Bundle>								bundles			= new ArrayList<>();
	private FrameworkWiring frameworkWiring;
	private ConditionalPermissionAdmin condPermAdmin;
	private ServiceReference<ConditionalPermissionAdmin>	condPermAdminRef;
	private String CONDITION_PREFIX;

	public Bundle install(String bundle) {
		Bundle result = bundleInstaller.installBundle(bundle, false);
		if (!bundles.contains(result))
			bundles.add(result);
		return result;
	}

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		testName = testInfo.getTestMethod().get().getName();
		bundles.clear();
		frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
		condPermAdminRef = bundleContext.getServiceReference(ConditionalPermissionAdmin.class);
		assertThat(condPermAdminRef)
				.as("No ConditionalPermisionAdmin service")
				.isNotNull();
		condPermAdmin = bundleContext.getService(condPermAdminRef);
		assertThat(condPermAdmin)
				.as("No ConditionalPermisionAdmin service")
				.isNotNull();
		CONDITION_PREFIX = getName();
		ConditionalPermissionUpdate update = condPermAdmin.newConditionalPermissionUpdate();
		ConditionalPermissionInfo info = condPermAdmin.newConditionalPermissionInfo(CONDITION_PREFIX + ".default", null, allPermissions, ConditionalPermissionInfo.ALLOW);
		List<ConditionalPermissionInfo> infos = update
				.getConditionalPermissionInfos();
		infos.add(info);
		update.commit();
	}

	@AfterEach
	public void tearDown() throws Exception {
		for (Iterator<Bundle> iBundles = bundles.iterator(); iBundles
				.hasNext();)
			try {
				iBundles.next().uninstall();
			} catch (BundleException e) {
				// nothing
			} catch (IllegalStateException e) {
				// happens if the test uninstalls the bundle itself
			}
		refreshBundles(bundles);
		bundles.clear();
		if (condPermAdmin != null) {
			ConditionalPermissionUpdate update = condPermAdmin.newConditionalPermissionUpdate();
			List<ConditionalPermissionInfo> infos = update
					.getConditionalPermissionInfos();
			for (Iterator<ConditionalPermissionInfo> iInfos = infos
					.iterator(); iInfos.hasNext();) {
				ConditionalPermissionInfo info = iInfos.next();
				if (info.getName() != null && info.getName().startsWith(CONDITION_PREFIX))
					iInfos.remove();
			}
			update.commit();
			condPermAdmin = null;
		}
		if (condPermAdminRef != null) {
			bundleContext.ungetService(condPermAdminRef);
			condPermAdminRef = null;
		}
	}

	private void refreshBundles(List<Bundle> bundles) {
		final boolean[] done = new boolean[] {false};
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				synchronized (done) {
					if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
						done[0] = true;
						done.notify();
					}
				}
			}
		};
		frameworkWiring.refreshBundles(bundles, new FrameworkListener[] {listener});
		synchronized (done) {
			if (!done[0])
				try {
					done.wait(5000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					fail("Unexepected interruption.", e);
				}
			if (!done[0])
				fail("Timed out waiting for refresh bundles to finish.");
		}
	}

	@Test
	public void testImpliedCapabilityPermission() {
		// install bundle with osgi.ee requirement
		Bundle requirer = install("resolver.tb2.jar");
		List<Bundle> bundles = Arrays.asList(requirer);
		assertThat(frameworkWiring.resolveBundles(bundles)).isTrue();

		// set no capability permission
		setPermissions(requirer.getLocation(), new PermissionInfo(ServicePermission.class.getName(), "*", ServicePermission.GET));
		refreshBundles(bundles);
		// should still resolve
		assertThat(frameworkWiring.resolveBundles(bundles)).isTrue();

		// set to wrong capability
		setPermissions(requirer.getLocation(), new PermissionInfo(CapabilityPermission.class.getName(), "wrong.namespace", CapabilityPermission.REQUIRE));
		refreshBundles(bundles);
		// should still resolve
		assertThat(frameworkWiring.resolveBundles(bundles)).isTrue();
	}

	@Test
	public void testCapabilityPermission() {
		Bundle provider = install("resolver.tb1.jar");
		Bundle requirer = install("resolver.tb5.jar");
		List<Bundle> testBundles = Arrays.asList(provider, requirer);

		// simple test with all permissions
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		// now grant permission to provide
		setPermissions(provider.getLocation(), new PermissionInfo(CapabilityPermission.class.getName(), "test", CapabilityPermission.PROVIDE));
		refreshBundles(Arrays.asList(new Bundle[]{provider}));
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		// now grant wrong namespace to provide
		setPermissions(provider.getLocation(), new PermissionInfo(CapabilityPermission.class.getName(), "wrong.namespace", CapabilityPermission.PROVIDE));
		refreshBundles(Arrays.asList(new Bundle[]{provider}));
		assertThat(frameworkWiring.resolveBundles(testBundles)).isFalse();
		assertThat(provider)
				.as("Wrong state for provider").isInState(Bundle.RESOLVED);
		assertThat(requirer)
				.as("Wrong state for requirer").isInState(Bundle.INSTALLED);
		// make sure the wiring does not indicate the capability is provided.
		BundleWiring providerWiring = provider.adapt(BundleWiring.class);
		List<BundleCapability> capabilities = providerWiring
				.getCapabilities("test");
		assertThat(capabilities)
				.as("Wrong number of capabilities").hasSize(0);

		// now grant permission to provide all
		setPermissions(provider.getLocation(), new PermissionInfo(CapabilityPermission.class.getName(), "*", CapabilityPermission.PROVIDE));
		refreshBundles(Arrays.asList(new Bundle[]{provider}));
		assertThat(frameworkWiring.resolveBundles(testBundles)).isTrue();

		// grant permission to require
		doRequireTest(provider, requirer, "test", true);

		// now grant wrong permission to require
		doRequireTest(provider, requirer, "wrong.namespace", false);
		assertThat(provider)
				.as("Wrong state for provider").isInState(Bundle.RESOLVED);
		assertThat(requirer)
				.as("Wrong state for requirer").isInState(Bundle.INSTALLED);
		// make sure the capability is provided.
		providerWiring = provider.adapt(BundleWiring.class);
		capabilities = providerWiring.getCapabilities("test");
		assertThat(capabilities)
				.as("Wrong number of capabilities").hasSize(1);

		// grant permission to require all
		doRequireTest(provider, requirer, "*", true);

		// grant permission to require by id of provider
		doRequireTest(provider, requirer, "(id=" + provider.getBundleId() + ")", true);
		// grant permission to require by wrong id of provider
		doRequireTest(provider, requirer, "(id=99999999)", false);

		// grant permission to require by location of provider
		doRequireTest(provider, requirer, "(location=" + provider.getLocation() + ")", true);
		// grant permission to require by wrong location of provider
		doRequireTest(provider, requirer, "(location=wrong.location)", false);

		// grant permission to require by bsn of provider
		doRequireTest(provider, requirer, "(name=" + provider.getSymbolicName() + ")", true);
		// grant permission to require by wrong bsn of provider
		doRequireTest(provider, requirer, "(name=wrong.name)", false);

		// grant permission to require by capability.namespace of provider
		doRequireTest(provider, requirer, "(capability.namespace=test)", true);
		// grant permission to require by wrong capability.namespace of provider
		doRequireTest(provider, requirer, "(capability.namespace=wrong.namespace)", false);

		// grant permission to require by conflict attribute of provider
		doRequireTest(provider, requirer, "(@name=test.conflict)", true);
		// grant permission to require by wrong conflict attribute of provider
		doRequireTest(provider, requirer, "(@name=wrong.conflict.value)", false);

		// grant permission to require by attr of provider
		doRequireTest(provider, requirer, "(version>=1.0)", true);
		// grant permission to require by wrong attr of provider
		doRequireTest(provider, requirer, "(version>=2.0)", false);

		// grant permission to require by attr of provider
		doRequireTest(provider, requirer, "(version.list>=1.1.10)", true);
		// grant permission to require by wrong attr of provider
		doRequireTest(provider, requirer, "(version.list>=2.0)", false);

		// grant permission to require by signer of provider
		doRequireTest(provider, requirer, "(signer=CN=John Smith,O=ACME Inc,OU=ACME Cert Authority,L=Austin,ST=Texas,C=US)", true);
		// grant permission to require by wrong signer of provider
		doRequireTest(provider, requirer, "(signer=CN=Wrong,O=Wrong,OU=Wrong,L=Wrong,ST=Wrong,C=Wrong)", false);
	}

	private void doRequireTest(Bundle provider, Bundle requirer, String filter, boolean resolveSucceed) {
		setPermissions(requirer.getLocation(), new PermissionInfo(CapabilityPermission.class.getName(), filter, CapabilityPermission.REQUIRE));
		refreshBundles(Arrays.asList(new Bundle[]{provider}));
		if (resolveSucceed)
			assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {provider, requirer}))).isTrue();
		else
			assertThat(frameworkWiring.resolveBundles(Arrays.asList(new Bundle[] {provider, requirer}))).isFalse();
	}

	private void setPermissions(String location, PermissionInfo capabilityPermission) {
		String name = CONDITION_PREFIX + '.' + location;
		ConditionalPermissionUpdate update = condPermAdmin.newConditionalPermissionUpdate();
		List<ConditionalPermissionInfo> infos = update
				.getConditionalPermissionInfos();
		for (Iterator<ConditionalPermissionInfo> iInfos = infos
				.iterator(); iInfos.hasNext();) {
			ConditionalPermissionInfo info = iInfos.next();
			if (info.getName() != null && info.getName().startsWith(name))
				iInfos.remove();
		}
		ConditionInfo condInfo = new ConditionInfo(BundleLocationCondition.class.getName(), new String[] {location});
		ConditionalPermissionInfo info1 = condPermAdmin.newConditionalPermissionInfo(name + ".deny", new ConditionInfo[] {condInfo}, allPermissions, ConditionalPermissionInfo.DENY);
		infos.add(0, info1);
		ConditionalPermissionInfo info2 = condPermAdmin.newConditionalPermissionInfo(name + ".allow", new ConditionInfo[] {condInfo}, new PermissionInfo[] {capabilityPermission}, ConditionalPermissionInfo.ALLOW);
		infos.add(0, info2);
		update.commit();
	}

}
