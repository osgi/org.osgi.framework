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

package org.osgi.test.cases.framework.junit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

public class ServiceScopesTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

    private BundleContext context;
    private Bundle        self;

	@BeforeEach
	public void setUp() {
        context = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
        self = context.getBundle();
    }

	@Test
	public void testSingletonScope() throws Exception {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("test", "yes");
        properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_BUNDLE);
        TestService service = new TestServiceImpl(0);
        ServiceRegistration<TestService> registration = context.registerService(TestService.class, service, properties);
        assertThat(registration).isNotNull();
        try {
            ServiceReference<TestService> reference = registration.getReference();
            assertThat(reference).isNotNull();
            assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_SINGLETON);
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
        } finally {
            registration.unregister();
        }
    }

	@Test
	public void testSingletonScopeWithTwoConsumers() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_BUNDLE);
            TestService service = new TestServiceImpl(0);
            ServiceRegistration<TestService> registration = tb1Context.registerService(TestService.class, service, properties);
            assertThat(registration).isNotNull();
            ServiceReference<TestService> reference1 = context.getServiceReference(TestService.class);
            assertThat(reference1).isNotNull();
            ServiceReference<TestService> reference2 = tb1Context.getServiceReference(TestService.class);
            assertThat(reference2).isNotNull();

            assertThat(reference1.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_SINGLETON);
            assertThat(reference2.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_SINGLETON);

            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(tb1Context.getService(reference2))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(tb1Context.getService(reference2))
            		.as("service object not the same")
				.isSameAs(service);

            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();

            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
            registration.unregister();
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testBundleScope() throws Exception {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("test", "yes");
        properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
        TestServiceFactory factory = new TestServiceFactory();
        ServiceRegistration<TestService> registration = context.registerService(TestService.class, factory, properties);
        assertThat(registration).isNotNull();
        try {
            ServiceReference<TestService> reference = registration.getReference();
            assertThat(reference).isNotNull();
            assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_BUNDLE);

			assertThat(factory.services).isEmpty();
            TestService service = context.getService(reference);
            assertThat(service).isNotNull();
            assertThat(service.getId()).isEqualTo(self.getBundleId());
			assertThat(factory.services).hasSize(1);
            assertThat(service)
            		.as("service object not the same")
				.isSameAs(factory.services.get(self));
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
			assertThat(factory.services).hasSize(1);
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).isEmpty();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
        } finally {
            registration.unregister();
        }
    }

	@Test
	public void testBundleScopeWithTwoConsumers() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();

            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
            TestServiceFactory factory = new TestServiceFactory();
            ServiceRegistration<TestService> registration = tb1Context.registerService(TestService.class, factory, properties);
            assertThat(registration).isNotNull();
            ServiceReference<TestService> reference1 = context.getServiceReference(TestService.class);
            assertThat(reference1).isNotNull();
            ServiceReference<TestService> reference2 = tb1Context.getServiceReference(TestService.class);
            assertThat(reference2).isNotNull();

            assertThat(reference1.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_BUNDLE);
            assertThat(reference2.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_BUNDLE);

			assertThat(factory.services).isEmpty();

            TestService service1 = context.getService(reference1);
            assertThat(service1).isNotNull();
            assertThat(service1)
            		.as("service object not the same")
				.isSameAs(factory.services.get(self));
            assertThat(service1.getId()).isEqualTo(self.getBundleId());
			assertThat(factory.services).hasSize(1);

            TestService service2 = tb1Context.getService(reference2);
            assertThat(service2).isNotNull();
            assertThat(service2)
            		.as("service object not the same")
				.isSameAs(factory.services.get(tb1));
            assertThat(service2.getId()).isEqualTo(tb1.getBundleId());
			assertThat(factory.services).hasSize(2);

            assertThat(service2).isNotSameAs(service1);

            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
            assertThat(tb1Context.getService(reference2))
            		.as("service object not the same")
				.isSameAs(service2);
			assertThat(factory.services).hasSize(2);

            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(2);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(2);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);

            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).isEmpty();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).isEmpty();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).isEmpty();
            registration.unregister();
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testPrototypeScope() throws Exception {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("test", "yes");
        properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
        TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
        ServiceRegistration<TestService> registration = context.registerService(TestService.class, factory, properties);
        assertThat(registration).isNotNull();
        try {
            ServiceReference<TestService> reference = registration.getReference();
            assertThat(reference).isNotNull();
            assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);

			assertThat(factory.services).isEmpty();
            TestService service = context.getService(reference);
            assertThat(service).isNotNull();
            assertThat(service.getId()).isEqualTo(self.getBundleId());
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
			assertThat(factory.services.get(self)).contains(service);
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).isEmpty();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
            assertThat(context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
        } finally {
            registration.unregister();
        }
    }

	@Test
	public void testMultipleGetsOfServiceObjects() throws Exception {
		ServiceRegistration<TestService> registration = context.registerService(TestService.class, new TestServiceImpl(42), null);
		assertThat(registration).isNotNull();
		try {
			ServiceReference<TestService> reference = registration.getReference();
			assertThat(reference).isNotNull();
			assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
					.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_SINGLETON);

			final ServiceObjects<TestService> so1 = context.getServiceObjects(reference);
			assertThat(so1).isNotNull();
			TestService service = so1.getService();
			assertThat(service).isNotNull();

			final ServiceObjects<TestService> so2 = context.getServiceObjects(reference);
			assertThat(so2).isNotNull();
			so2.ungetService(service);
		} finally {
			registration.unregister();
		}
	}

	@Test
	public void testPrototypeScopeWithTwoConsumers() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();

            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
            TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
            ServiceRegistration<TestService> registration = tb1Context.registerService(TestService.class, factory, properties);
            assertThat(registration).isNotNull();
            ServiceReference<TestService> reference1 = context.getServiceReference(TestService.class);
            assertThat(reference1).isNotNull();
            ServiceReference<TestService> reference2 = tb1Context.getServiceReference(TestService.class);
            assertThat(reference2).isNotNull();

            assertThat(reference1.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);
            assertThat(reference2.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);

			assertThat(factory.services).isEmpty();

            TestService service1 = context.getService(reference1);
            assertThat(service1).isNotNull();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
			assertThat(factory.services.get(self)).contains(service1);
            assertThat(service1.getId()).isEqualTo(self.getBundleId());

            TestService service2 = tb1Context.getService(reference2);
            assertThat(service2).isNotNull();
            assertThat(factory.services.size()).isEqualTo(2);
            assertThat(factory.services.get(tb1).size()).isEqualTo(1);
            assertThat(factory.services.get(tb1).contains(service2)).isTrue();
            assertThat(service2.getId()).isEqualTo(tb1.getBundleId());

            assertThat(service2).isNotSameAs(service1);

            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(tb1Context.getService(reference2))
            		.as("service object not the same")
				.isSameAs(service2);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(tb1)).hasSize(1);

            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
            assertThat(factory.services.get(self)).isNull();
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);
            assertThat(factory.services.get(self)).isNull();
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);
            assertThat(factory.services.get(self)).isNull();

            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(tb1)).hasSize(1);
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).isEmpty();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).isEmpty();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).isEmpty();
            registration.unregister();
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testPrototypeConsumerWithSingletonScope() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();

            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_BUNDLE);
            TestService service = new TestServiceImpl(0);
            ServiceRegistration<TestService> registration = tb1Context.registerService(TestService.class, service, properties);
            assertThat(registration).isNotNull();
            ServiceReference<TestService> reference = registration.getReference();
            assertThat(reference).isNotNull();
            assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_SINGLETON);

            ServiceObjects<TestService> objects = tb1Context.getServiceObjects(reference);
            assertThat(objects).isNotNull();
            assertThat(objects.getServiceReference()).isEqualTo(reference);

            assertThat(tb1Context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(tb1Context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(tb1Context.getService(reference))
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(objects.getService())
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(objects.getService())
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(objects.getService())
            		.as("service object not the same")
				.isSameAs(service);
            assertThat(tb1Context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
            assertThat(tb1Context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
            assertThat(tb1Context.ungetService(reference))
            		.as("service use count wrong")
				.isTrue();
            objects.ungetService(service);
            objects.ungetService(service);
            objects.ungetService(service);
            assertThat(tb1Context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
            assertThat(tb1Context.ungetService(reference))
            		.as("service use count wrong")
				.isFalse();
            registration.unregister();
            assertThat(tb1Context.getServiceObjects(reference)).isNull();
            tb1.stop();
            assertThatExceptionOfType(IllegalStateException.class).as("expected exception")
            		.isThrownBy(() -> tb1Context.getServiceObjects(reference));
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testPrototypeConsumerWithBundleScope() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();

            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
            TestServiceFactory factory = new TestServiceFactory();
            ServiceRegistration<TestService> registration = tb1Context.registerService(TestService.class, factory, properties);
            assertThat(registration).isNotNull();
            ServiceReference<TestService> reference1 = context.getServiceReference(TestService.class);
            ServiceReference<TestService> reference2 = tb1Context.getServiceReference(TestService.class);
            assertThat(reference1).isNotNull();
            assertThat(reference2).isNotNull();

            assertThat(reference1.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_BUNDLE);
            assertThat(reference2.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_BUNDLE);

            ServiceObjects<TestService> objects1 = context.getServiceObjects(reference1);
            assertThat(objects1).isNotNull();
            assertThat(objects1.getServiceReference()).isEqualTo(reference1);
            ServiceObjects<TestService> objects2 = tb1Context.getServiceObjects(reference2);
            assertThat(objects2).isNotNull();
            assertThat(objects2.getServiceReference()).isEqualTo(reference2);

			assertThat(factory.services).isEmpty();

            TestService service1 = objects1.getService();
            assertThat(service1).isNotNull();
            assertThat(service1)
            		.as("service object not the same")
				.isSameAs(factory.services.get(self));
            assertThat(service1.getId()).isEqualTo(self.getBundleId());
			assertThat(factory.services).hasSize(1);

            TestService service2 = objects2.getService();
            assertThat(service2).isNotNull();
            assertThat(service2)
            		.as("service object not the same")
				.isSameAs(factory.services.get(tb1));
            assertThat(service2.getId()).isEqualTo(tb1.getBundleId());
			assertThat(factory.services).hasSize(2);

            assertThat(service2).isNotSameAs(service1);

            assertThat(objects1.getService())
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
            assertThat(tb1Context.getService(reference2))
            		.as("service object not the same")
				.isSameAs(service2);
			assertThat(factory.services).hasSize(2);

            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects1.ungetService(service2));
            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects2.ungetService(service1));

            objects1.ungetService(service1);
			assertThat(factory.services).hasSize(2);
            objects1.ungetService(service1);
			assertThat(factory.services).hasSize(2);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);

            objects2.ungetService(service2);
			assertThat(factory.services).hasSize(1);
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).isEmpty();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).isEmpty();
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).isEmpty();
            registration.unregister();
            assertThat(tb1Context.getServiceObjects(reference1)).isNull();
            tb1.stop();
            assertThatExceptionOfType(IllegalStateException.class).as("expected exception")
            		.isThrownBy(() -> tb1Context.getServiceObjects(reference1));
            assertThat(objects1.getServiceReference()).isEqualTo(reference1);
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testPrototypeConsumerWithPrototypeScope() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();

            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
            TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
            ServiceRegistration<TestService> registration = tb1Context.registerService(TestService.class, factory, properties);
            assertThat(registration).isNotNull();
            ServiceReference<TestService> reference1 = context.getServiceReference(TestService.class);
            assertThat(reference1).isNotNull();
            ServiceReference<TestService> reference2 = tb1Context.getServiceReference(TestService.class);
            assertThat(reference2).isNotNull();

            assertThat(reference1.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);
            assertThat(reference2.getProperty(Constants.SERVICE_SCOPE))
            		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);

            ServiceObjects<TestService> objects1 = context.getServiceObjects(reference1);
            assertThat(objects1).isNotNull();
            assertThat(objects1.getServiceReference()).isEqualTo(reference1);
            ServiceObjects<TestService> objects2 = tb1Context.getServiceObjects(reference2);
            assertThat(objects2).isNotNull();
            assertThat(objects2.getServiceReference()).isEqualTo(reference2);

			assertThat(factory.services).isEmpty();

            TestService service1 = context.getService(reference1);
            assertThat(service1).isNotNull();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(self)).hasSize(1);
			assertThat(factory.services.get(self)).contains(service1);
            assertThat(service1.getId()).isEqualTo(self.getBundleId());

            TestService service2 = tb1Context.getService(reference2);
            assertThat(service2).isNotNull();
            assertThat(factory.services.size()).isEqualTo(2);
            assertThat(factory.services.get(tb1).size()).isEqualTo(1);
            assertThat(factory.services.get(tb1).contains(service2)).isTrue();
            assertThat(service2.getId()).isEqualTo(tb1.getBundleId());

            assertThat(service2).isNotSameAs(service1);

            assertThat(context.getService(reference1))
            		.as("service object not the same")
				.isSameAs(service1);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(1);
            TestService service11 = objects1.getService();
            assertThat(service11)
            		.as("service object the same")
				.isNotSameAs(service1);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(2);
            TestService service12 = objects1.getService();
            assertThat(service12)
            		.as("service object the same")
				.isNotSameAs(service1);
            assertThat(service12)
            		.as("service object the same")
				.isNotSameAs(service11);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(3);

            assertThat(tb1Context.getService(reference2))
            		.as("service object not the same")
				.isSameAs(service2);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(tb1)).hasSize(1);
            TestService service21 = objects2.getService();
            assertThat(service21)
            		.as("service object the same")
				.isNotSameAs(service2);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(tb1)).hasSize(2);
            TestService service22 = objects2.getService();
            assertThat(service22)
            		.as("service object the same")
				.isNotSameAs(service2);
            assertThat(service22)
            		.as("service object the same")
				.isNotSameAs(service21);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(tb1)).hasSize(3);

            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects1.ungetService(service1));
            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects2.ungetService(service2));

            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(3);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(2);

            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(2);
            assertThat(context.ungetService(reference1))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(2);

            objects1.ungetService(service11);
			assertThat(factory.services).hasSize(2);
			assertThat(factory.services.get(self)).hasSize(1);
            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects1.ungetService(service11));
            objects1.ungetService(service12);
			assertThat(factory.services).hasSize(1);
            assertThat(factory.services.get(self)).isNull();
            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects1.ungetService(service12));

            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(tb1)).hasSize(3);
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isTrue();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(tb1)).hasSize(2);

            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(tb1)).hasSize(2);
            assertThat(tb1Context.ungetService(reference2))
            		.as("service use count wrong")
				.isFalse();
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(tb1)).hasSize(2);

            objects2.ungetService(service21);
			assertThat(factory.services).hasSize(1);
			assertThat(factory.services.get(tb1)).hasSize(1);
            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects2.ungetService(service21));
            objects2.ungetService(service22);
			assertThat(factory.services).isEmpty();
            assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
            		.isThrownBy(() -> objects2.ungetService(service22));

            registration.unregister();
            assertThat(tb1Context.getServiceObjects(reference1)).isNull();
            tb1.stop();
            assertThatExceptionOfType(IllegalStateException.class).as("expected exception")
            		.isThrownBy(() -> tb1Context.getServiceObjects(reference1));

        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testPrototypeConsumerWithPrototypeScopeOneService()
			throws Exception {
		Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
		assertThat(tb1).isNotNull();
		try {
			tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
			BundleContext tb1Context = tb1.getBundleContext();
			assertThat(tb1Context).isNotNull();

			Hashtable<String,String> properties = new Hashtable<String,String>();
			properties.put("test", "yes");
			TestPrototypeServiceFactoryOneObject factory = new TestPrototypeServiceFactoryOneObject();
			ServiceRegistration<TestService> registration = tb1Context
					.registerService(TestService.class, factory, properties);
			assertThat(registration).isNotNull();
			ServiceReference<TestService> reference1 = context
					.getServiceReference(TestService.class);
			assertThat(reference1).isNotNull();
			ServiceReference<TestService> reference2 = tb1Context
					.getServiceReference(TestService.class);
			assertThat(reference2).isNotNull();

			ServiceObjects<TestService> objects1 = context
					.getServiceObjects(reference1);
			assertThat(objects1).isNotNull();
			assertThat(objects1.getServiceReference()).isEqualTo(reference1);
			ServiceObjects<TestService> objects2 = tb1Context
					.getServiceObjects(reference2);
			assertThat(objects2).isNotNull();
			assertThat(objects2.getServiceReference()).isEqualTo(reference2);

			assertThat(factory.services).isEmpty();

			TestService service1 = objects1.getService();
			assertThat(service1).isNotNull();
			assertThat(factory.services).hasSize(1);
			assertThat(service1).isEqualTo(factory.services.get(self));
			assertThat(service1.getId()).isEqualTo(self.getBundleId());

			TestService service2 = objects2.getService();
			assertThat(service2).isNotNull();
			assertThat(factory.services).hasSize(2);
			assertThat(service2).isEqualTo(factory.services.get(tb1));
			assertThat(service2.getId()).isEqualTo(tb1.getBundleId());

			assertThat(service2).isNotSameAs(service1);
			assertThat(service1.getCount().get()).isOne();
			assertThat(service2.getCount().get()).isOne();

			TestService service11 = objects1.getService();
			assertThat(service11)
					.as("service object not same")
				.isSameAs(service1);
			assertThat(service1.getCount().get()).isEqualTo(2);
			TestService service12 = objects1.getService();
			assertThat(service12)
					.as("service object not same")
				.isSameAs(service1);
			assertThat(service1.getCount().get()).isEqualTo(3);
			assertThat(service12)
					.as("service object not same")
				.isSameAs(service11);

			TestService service21 = objects2.getService();
			assertThat(service21)
					.as("service object not same")
				.isSameAs(service2);
			assertThat(service2.getCount().get()).isEqualTo(2);
			TestService service22 = objects2.getService();
			assertThat(service22)
					.as("service object not same")
				.isSameAs(service2);
			assertThat(service2.getCount().get()).isEqualTo(3);
			assertThat(service22)
					.as("service object not same")
				.isSameAs(service21);

			objects1.ungetService(service1);
			assertThat(service1).isEqualTo(factory.services.get(self));
			assertThat(service1.getCount().get()).isEqualTo(3);
			objects1.ungetService(service1);
			assertThat(service1).isEqualTo(factory.services.get(self));
			assertThat(service1.getCount().get()).isEqualTo(3);
			objects1.ungetService(service1);
			assertThat(factory.services.get(self)).isNull();
			assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
					.isThrownBy(() -> objects1.ungetService(service1));

			objects2.ungetService(service2);
			assertThat(service2).isEqualTo(factory.services.get(tb1));
			assertThat(service2.getCount().get()).isEqualTo(3);
			objects2.ungetService(service2);
			assertThat(service2).isEqualTo(factory.services.get(tb1));
			assertThat(service2.getCount().get()).isEqualTo(3);
			objects2.ungetService(service2);
			assertThat(factory.services.get(tb1)).isNull();
			assertThatExceptionOfType(IllegalArgumentException.class).as("expected exception")
					.isThrownBy(() -> objects2.ungetService(service2));

			registration.unregister();
			assertThat(tb1Context.getServiceObjects(reference1)).isNull();
			tb1.stop();
			assertThatExceptionOfType(IllegalStateException.class).as("expected exception")
					.isThrownBy(() -> tb1Context.getServiceObjects(reference1));

		} finally {
			tb1.uninstall();
		}
	}

	@Test
	public void testPrototypeScopeCleanup() throws Exception {
        Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
        assertThat(tb1).isNotNull();
        try {
            tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
            BundleContext tb1Context = tb1.getBundleContext();
            assertThat(tb1Context).isNotNull();

            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put("test", "yes");
            properties.put(Constants.SERVICE_SCOPE, Constants.SCOPE_SINGLETON);
            TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
            ServiceRegistration<TestService> registration = context.registerService(TestService.class, factory, properties);
            assertThat(registration).isNotNull();
            try {
                ServiceReference<TestService> reference = tb1Context.getServiceReference(TestService.class);
                assertThat(reference).isNotNull();

                assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
                		.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);

                ServiceObjects<TestService> objects = tb1Context.getServiceObjects(reference);
                assertThat(objects).isNotNull();
                assertThat(objects.getServiceReference()).isEqualTo(reference);

				assertThat(factory.services).isEmpty();

                TestService service1 = tb1Context.getService(reference);
                assertThat(service1).isNotNull();
				assertThat(factory.services).hasSize(1);
				assertThat(factory.services.get(tb1)).hasSize(1);
				assertThat(factory.services.get(tb1)).contains(service1);
                assertThat(service1.getId()).isEqualTo(tb1.getBundleId());

                TestService service2 = objects.getService();
                assertThat(service2)
                		.as("service object the same")
				.isNotSameAs(service1);
                assertThat(factory.services.size()).isEqualTo(1);
                assertThat(factory.services.get(tb1).size()).isEqualTo(2);
                assertThat(factory.services.get(tb1).contains(service2)).isTrue();

                TestService service3 = objects.getService();
                assertThat(service3)
                		.as("service object the same")
				.isNotSameAs(service2);
				assertThat(factory.services).hasSize(1);
				assertThat(factory.services.get(tb1)).hasSize(3);
				assertThat(factory.services.get(tb1)).contains(service3);

                tb1.stop();
				assertThat(factory.services).isEmpty();
                assertThat(objects.getServiceReference()).isEqualTo(reference);
            } finally {
                registration.unregister();
            }
        } finally {
            tb1.uninstall();
        }
    }

	@Test
	public void testPrototypeScopeCleanupOnUnregister() throws Exception {
		Bundle tb1 = bundleInstaller.installBundle("serviceregistry.tb1.jar", false);
		assertThat(tb1).isNotNull();
		try {
			tb1.start();
			assertThat(tb1).isInState(Bundle.ACTIVE);
			BundleContext tb1Context = tb1.getBundleContext();
			assertThat(tb1Context).isNotNull();

			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put("test", "yes");
			TestPrototypeServiceFactory factory = new TestPrototypeServiceFactory();
			ServiceRegistration<TestService> registration = context.registerService(TestService.class, factory, properties);
			assertThat(registration).isNotNull();
			try {
				ServiceReference<TestService> reference = tb1Context.getServiceReference(TestService.class);
				assertThat(reference).isNotNull();

				assertThat(reference.getProperty(Constants.SERVICE_SCOPE))
						.as("service.scope not set correctly")
				.isEqualTo(Constants.SCOPE_PROTOTYPE);

				ServiceObjects<TestService> objects = tb1Context.getServiceObjects(reference);
				assertThat(objects).isNotNull();
				assertThat(objects.getServiceReference()).isEqualTo(reference);

				assertThat(factory.services).isEmpty();

				TestService service1 = tb1Context.getService(reference);
				assertThat(service1).isNotNull();

				TestService service2 = objects.getService();
				assertThat(service2)
						.as("service object the same")
				.isNotSameAs(service1);

				TestService service3 = objects.getService();
				assertThat(service3)
						.as("service object the same")
				.isNotSameAs(service2);

				assertThat(factory.services).hasSize(1);
				assertThat(factory.services.get(tb1)).hasSize(3);
			} finally {
				registration.unregister();
			}
			// test clean up on unregister now;
			assertThat(factory.services).isEmpty();
		} finally {
			tb1.uninstall();
		}
	}

    public interface TestService {
        public long getId();

		public AtomicInteger getCount();
    }

    static class TestServiceImpl implements TestService {
        private final long id;
		private final AtomicInteger	count;

        TestServiceImpl(long id) {
            this.id = id;
			count = new AtomicInteger();
        }

		@Override
		public long getId() {
            return id;
        }

		@Override
		public AtomicInteger getCount() {
			return count;
		}
    }

    static class TestServiceFactory implements ServiceFactory<TestService> {
        Map<Bundle, TestService> services = Collections.synchronizedMap(new HashMap<Bundle, TestService>());

		@Override
		public TestService getService(Bundle bundle,
				ServiceRegistration<TestService> registration) {
            TestService service = new TestServiceImpl(bundle.getBundleId());
            assertThat(services.get(bundle)).isNull();
            services.put(bundle, service);
            return service;
        }

		@Override
		public void ungetService(Bundle bundle,
				ServiceRegistration<TestService> registration,
				TestService service) {
            assertThat(service).isEqualTo(services.get(bundle));
            assertThat(service.getId()).isEqualTo(bundle.getBundleId());
            services.remove(bundle);
        }
    }

    static class TestPrototypeServiceFactory implements PrototypeServiceFactory<TestService> {
        Map<Bundle, List<TestService>> services = Collections.synchronizedMap(new HashMap<Bundle, List<TestService>>());

		@Override
		public TestService getService(Bundle bundle,
				ServiceRegistration<TestService> registration) {
            List<TestService> bundleServices = services.get(bundle);
            if (bundleServices == null) {
                bundleServices = new ArrayList<TestService>();
            }
            TestService service = new TestServiceImpl(bundle.getBundleId());
            bundleServices.add(service);
            services.put(bundle, bundleServices);
            return service;
        }

		@Override
		public void ungetService(Bundle bundle,
				ServiceRegistration<TestService> registration,
				TestService service) {
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

	static class TestPrototypeServiceFactoryOneObject
			implements PrototypeServiceFactory<TestService> {
		Map<Bundle,TestService> services = Collections
				.synchronizedMap(new HashMap<Bundle,TestService>());

		@Override
		public TestService getService(Bundle bundle,
				ServiceRegistration<TestService> registration) {
			TestService service = services.get(bundle);
			if (service == null) {
				service = new TestServiceImpl(bundle.getBundleId());
				services.put(bundle, service);
			}
			service.getCount().getAndIncrement();
			return service;
		}

		@Override
		public void ungetService(Bundle bundle,
				ServiceRegistration<TestService> registration,
				TestService service) {
			TestService bundleService = services.get(bundle);
			assertThat(bundleService).isNotNull();
			assertThat(service).isSameAs(bundleService);
			assertThat(service.getId()).isEqualTo(bundle.getBundleId());
			services.remove(bundle);
		}
	}
}
