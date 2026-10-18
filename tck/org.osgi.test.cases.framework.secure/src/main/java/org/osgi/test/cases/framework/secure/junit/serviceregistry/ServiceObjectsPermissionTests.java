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

package org.osgi.test.cases.framework.secure.junit.serviceregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.cases.framework.secure.junit.serviceregistry.export.ReferenceProvider;
import org.osgi.test.cases.framework.secure.junit.serviceregistry.export.TestService;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

public class ServiceObjectsPermissionTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

    private BundleContext context;

	@BeforeEach
	public void setUp() {
        context = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
    }

	@Test
	public void testHasServicePermission() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
            ServiceRegistration<TestService> registration = context.registerService(TestService.class, factory, null);
            assertThat(registration).isNotNull();
            try {
				assertThat(factory.services).isEmpty();

                tb1.start();
				assertThat(tb1).isInState(Bundle.ACTIVE);

				assertThat(factory.services).hasSize(1);
				assertThat(factory.services.get(tb1)).hasSize(1);

                tb1.stop();
				assertThat(tb1).isNotInState(Bundle.ACTIVE);

				assertThat(factory.services).isEmpty();

            } finally {
                registration.unregister();
            }
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testNotServicePermission() throws Exception {
        Bundle tb2 = bundleInstaller.installBundle("serviceregistry.tb2.jar", false);
        assertThat(tb2).isNotNull();
        try {
            TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
            final ServiceRegistration<TestService> registration = context.registerService(TestService.class, factory, null);
            assertThat(registration).isNotNull();
            try {
                ServiceRegistration<ReferenceProvider> providerReg = context.registerService(ReferenceProvider.class, new ReferenceProvider() {
                    public ServiceReference<TestService> getReference() {
                        return registration.getReference();
                    }
                }, null);
                assertThat(providerReg).isNotNull();
                try {

					assertThat(factory.services).isEmpty();

                    tb2.start();
					assertThat(tb2).isInState(Bundle.ACTIVE);

					assertThat(factory.services).isEmpty();

                    tb2.stop();
					assertThat(tb2).isNotInState(Bundle.ACTIVE);

                } finally {
                    providerReg.unregister();
                }
				assertThat(factory.services).isEmpty();

            } finally {
                registration.unregister();
            }
        } finally {
            tb2.uninstall();
        }
    }

    static class TestServiceImpl implements TestService {
        private final long id;

        TestServiceImpl(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    static class TestPrototypeServiceFactory implements PrototypeServiceFactory<TestService> {
        Map<Bundle, List<TestService>> services = Collections.synchronizedMap(new HashMap<Bundle, List<TestService>>());

        public TestService getService(Bundle bundle, ServiceRegistration<TestService> registration) {
            List<TestService> bundleServices = services.get(bundle);
            if (bundleServices == null) {
                bundleServices = new ArrayList<TestService>();
            }
            TestService service = new TestServiceImpl(bundle.getBundleId());
            bundleServices.add(service);
            services.put(bundle, bundleServices);
            return service;
        }

        public void ungetService(Bundle bundle, ServiceRegistration<TestService> registration, TestService service) {
            List<TestService> bundleServices = services.get(bundle);
            assertThat(bundleServices).isNotNull();
            assertThat(bundleServices.contains(service)).isTrue();
            assertThat(service.getId()).isEqualTo(bundle.getBundleId());
            bundleServices.remove(service);
            if (bundleServices.isEmpty()) {
                services.remove(bundle);
            }
            else {
                services.put(bundle, bundleServices);
            }
        }
    }
}
