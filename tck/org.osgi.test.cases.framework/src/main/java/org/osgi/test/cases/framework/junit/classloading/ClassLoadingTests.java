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
package org.osgi.test.cases.framework.junit.classloading;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.annotation.InjectInstalledBundle;
import org.osgi.test.common.install.BundleInstaller;
import org.osgi.test.common.wiring.FrameworkWirings;

/**
 * This class contains tests related with the framework class loading policies.
 *
 * @author left
 * @author $Id$
 */
public class ClassLoadingTests {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	@BeforeEach
	public void refreshBeforeTest() {
		// injected bundles are uninstalled after @AfterEach: refresh before the next test
		FrameworkWirings.refreshBundles(bundleContext);
	}

	@AfterEach
	public void tearDown() {
		FrameworkWirings.refreshBundles(bundleContext);
	}

	// Service Registry --------------------------

	/**
	 * BundleContext.getServiceReference returns the highest ranked service that
	 * BundleContext.getServiceReferences returns.
	 *
	 * This test case will check the behavior of method getServiceReference()
	 * when more than one service is installed.
	 *
	 * @osgi.spec BundleContext.getServiceReference(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleContextGetReference001(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb1a.jar", start = true) Bundle tb1a,
			@InjectInstalledBundle(value = "classloading.tb1b.jar", start = true) Bundle tb1b,
			@InjectInstalledBundle(value = "classloading.tb4a.jar", start = false) Bundle tb4a
			) throws Exception {
		try {
			System.out.println("Checking the service reference returned by BundleContext.getServiceReference()");
			tb4a.start();
			tb4a.stop();
		}
		catch (BundleException ex) {
			if (ex.getCause() != null) {
				fail(ex.getCause().getMessage());
			}
			else {
				fail(ex.getMessage());
			}
		}
	}

	/**
	 * If getServiceReferences returns no service references, then
	 * getServiceReference returns null.
	 *
	 * This test case will check the behavior of method getServiceReference()
	 * without any installed services.
	 *
	 * @osgi.spec BundleContext.getServiceReference(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleContextGetReference002(
			@InjectInstalledBundle(value = "classloading.tb4b.jar", start = false) Bundle tb4b
			) throws Exception {
		try {
			System.out.println("Checking the behavior of BundleContext.getServiceReference() when BundleContext.getServiceReferences() returns no references");
			tb4b.start();
			tb4b.stop();
		}
		catch (BundleException ex) {
			if (ex.getCause() != null) {
				fail(ex.getCause().getMessage());
			}
			else {
				fail(ex.getMessage());
			}
		}
	}

	/**
	 * Some bundles may wish to get all service references available in the
	 * framework regardless of what version of a package they are wired to. A
	 * new method is added (BundleContext.getAllServiceReferences(String,
	 * String)) to allow a bundle to get all ServiceReference objects.
	 * getAllServiceReferences returns all the ServiceReference objects without
	 * performing any class tests.
	 *
	 * The test bundle will importing the version 3.0.0 of the interface
	 * SomeService and check the behavior of method getServiceReferences() when
	 * two services using the interface version 1.0.0 are installed. The method
	 * getAllServiceReferences() must returns all services independent of the
	 * interface wired to the test bundle.
	 *
	 * @osgi.spec BundleContext.getAllServiceReferences(String, String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleContextGetAllServiceReferences001(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb1a.jar", start = true) Bundle tb1a,
			@InjectInstalledBundle(value = "classloading.tb1b.jar", start = true) Bundle tb1b,
			@InjectInstalledBundle(value = "classloading.tb3.jar", start = true) Bundle tb3,
			@InjectInstalledBundle(value = "classloading.tb4c.jar", start = false) Bundle tb4c
			) throws Exception {
		try {
			System.out.println("Checking if the method getAllServiceReferences returns the number of services as expected");
			tb4c.start();
			tb4c.stop();
		}
		catch (BundleException ex) {
			if (ex.getCause() != null) {
				fail(ex.getCause().getMessage());
			}
			else {
				fail(ex.getMessage());
			}
		}
	}

	/**
	 * This test case will check the behavior of method isAssignableTo() when
	 * the wired package of the getter bundle (the bundle requesting the
	 * service) is the same of the registrant bundle.
	 *
	 * @osgi.spec ServiceReference.isAssignableTo(Bundle, String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleContextIsAssignableTo001(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb1a.jar", start = true) Bundle tb1a,
			@InjectInstalledBundle(value = "classloading.tb4d.jar", start = false) Bundle tb4d
			) throws Exception {
		try {
			System.out.println("Checking if the method isAssignableTo returns true when the package source of the getter bundle is the same of the registrant bundle");
			tb4d.start();
			tb4d.stop();
		}
		catch (BundleException ex) {
			if (ex.getCause() != null) {
				fail(ex.getCause().getMessage());
			}
			else {
				fail(ex.getMessage());
			}
		}
	}

	/**
	 * This test case will check the behavior of method isAssignableTo() when
	 * the wired package of the getter bundle (the bundle requesting the
	 * service) is not the same of the registrant bundle.
	 *
	 * @osgi.spec ServiceReference.isAssignableTo(Bundle, String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleContextIsAssignableTo002(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb1a.jar", start = true) Bundle tb1a,
			@InjectInstalledBundle(value = "classloading.tb3.jar", start = true) Bundle tb3,
			@InjectInstalledBundle(value = "classloading.tb4e.jar", start = false) Bundle tb4e
			) throws Exception {
		try {
			System.out.println("Checking if the method isAssignableTo returns false when the package source of the getter bundle is not the same of the registrant bundle");
			tb4e.start();
			tb4e.stop();
		}
		catch (BundleException ex) {
			if (ex.getCause() != null) {
				fail(ex.getCause().getMessage());
			}
			else {
				fail(ex.getMessage());
			}
		}
	}

	private static final String	SOME_SERVICE_CLASS	= "org.osgi.test.cases.framework.classloading.exports.service.SomeService";
	private static final String	SOME_SERVICE_IMPL	= "org.osgi.test.cases.framework.classloading.exports.service.SimpleSomeServiceImpl";

	private enum IMPL_BUNDLE {
		VERSION1, VERSION2, REGISTRANT, USER
	}

	/**
	 * User bundle has no package source; should see other's service
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBundleContextIsAssignableTo003() throws Exception {
		BiFunction<Bundle,Object,ServiceReference< ? >> register = (b, s) -> {
			return b.getBundleContext()
					.registerService(SOME_SERVICE_CLASS, s, null)
					.getReference();
		};
		doTestBundleContextIsAssibnableTo( //
				"classloading.tb18c.jar", //
				"classloading.tb18d.jar", //
				IMPL_BUNDLE.VERSION1, //
				true, //
				register);
	}

	/**
	 * Tests two bundles have private packages; should not see eachother's
	 * service
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBundleContextIsAssignableTo004() throws Exception {
		BiFunction<Bundle,Object,ServiceReference< ? >> register = (b, s) -> {
			return b.getBundleContext()
					.registerService(SOME_SERVICE_CLASS, s, null)
					.getReference();
		};
		doTestBundleContextIsAssibnableTo( //
				"classloading.tb18a.jar", //
				"classloading.tb18b.jar", //
				IMPL_BUNDLE.REGISTRANT, //
				false, //
				register);
	}

	/**
	 * Test user has package source, registrant does not, a factory from another
	 * bundle is used; should see the service
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBundleContextIsAssignableTo005() throws Exception {
		BiFunction<Bundle,Object,ServiceReference< ? >> register = (b, s) -> {
			return b.getBundleContext()
					.registerService(SOME_SERVICE_CLASS,
							new ServiceFactory<Object>() {

								@Override
								public Object getService(Bundle bundle,
										ServiceRegistration<Object> registration) {
									return s;
								}

								@Override
								public void ungetService(Bundle bundle,
										ServiceRegistration<Object> registration,
										Object service) {
								}

							}, null)
					.getReference();
		};
		doTestBundleContextIsAssibnableTo( //
				"classloading.tb18d.jar", //
				"classloading.tb18c.jar", //
				IMPL_BUNDLE.VERSION1, //
				true, //
				register);
	}

	/**
	 * Test user has package source, registrant does not, register an object
	 * that has a different package source; should not see service
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBundleContextIsAssignableTo006() throws Exception {
		BiFunction<Bundle,Object,ServiceReference< ? >> register = (b, s) -> {
			return b.getBundleContext()
					.registerService(SOME_SERVICE_CLASS, s, null)
					.getReference();
		};
		doTestBundleContextIsAssibnableTo( //
				"classloading.tb18d.jar", //
				"classloading.tb18c.jar", //
				IMPL_BUNDLE.VERSION2, //
				false, //
				register);
	}

	/**
	 * Test user has package source, registrant does not, register an object
	 * that has same package source as user; should see service
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBundleContextIsAssignableTo007() throws Exception {
		BiFunction<Bundle,Object,ServiceReference< ? >> register = (b, s) -> {
			return b.getBundleContext()
					.registerService(SOME_SERVICE_CLASS, s, null)
					.getReference();
		};
		doTestBundleContextIsAssibnableTo( //
				"classloading.tb18d.jar", //
				"classloading.tb18c.jar", //
				IMPL_BUNDLE.VERSION1, //
				true, //
				register);
	}

	private void doTestBundleContextIsAssibnableTo(String registrantLoc,
			String userLoc, IMPL_BUNDLE implBundle, boolean expected,
			BiFunction<Bundle,Object,ServiceReference< ? >> register)
			throws Exception {
		List<Bundle> bundles = new ArrayList<>();

		try {
			Bundle exporter1;
			Bundle exporter2;
			Bundle registrant;
			Bundle user;
			bundles.add(exporter1 = bundleInstaller.installBundle("classloading.tb1.jar", true));
			bundles.add(exporter2 = bundleInstaller.installBundle("classloading.tb2.jar", true));
			bundles.add(registrant = bundleInstaller.installBundle(registrantLoc, true));
			bundles.add(user = bundleInstaller.installBundle(userLoc, true));
			bundles.forEach(b -> {
				try {
					b.start();
				} catch (BundleException e) {
					sneakyThrow(e);
				}
			});
			Class< ? > serviceImpl;
			switch (implBundle) {
				case VERSION1 :
					serviceImpl = exporter1.loadClass(SOME_SERVICE_IMPL);
					break;
				case VERSION2 :
					serviceImpl = exporter2.loadClass(SOME_SERVICE_IMPL);
					break;
				case REGISTRANT :
					serviceImpl = registrant.loadClass(SOME_SERVICE_IMPL);
					break;
				case USER :
					serviceImpl = user.loadClass(SOME_SERVICE_IMPL);
				default :
					throw new RuntimeException("unknown");
			}
			ServiceReference< ? > ref = register.apply(registrant,
					serviceImpl.getConstructor().newInstance());
			assertThat(ref.isAssignableTo(user,
							"org.osgi.test.cases.framework.classloading.exports.service.SomeService"))
					.as("Wrong result from isAssignableTo.")
					.isEqualTo(expected);

		} finally {
			bundles.forEach(b -> {
				try {
					b.uninstall();
				} catch (BundleException e) {
					sneakyThrow(e);
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}

	/**
	 * The service registry must deal with multiple versions of a particular
	 * interface being published. It should record the module which registered
	 * each interface and use this information to match against a module later
	 * attempting to find the interface. A find should be satisfied with the
	 * version of the service interface imported by the module attempting to
	 * find the interface.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testServiceRegistryWithMultipleServices001(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb2.jar", start = true) Bundle tb2
			) throws Exception {
		Bundle tb1a;
		Bundle tb2a;
		Bundle tb4f;
		Bundle tb4g;

		try {
			System.out.println("Checking if the correct service is imported");
			tb4f = bundleInstaller.installBundle("classloading.tb4f.jar", false);
			tb1a = bundleInstaller.installBundle("classloading.tb1a.jar", true);
			tb1a.start();
			tb4f.start();
			tb4f.stop();
			tb1a.stop();
			tb1a.uninstall();
			tb4f.uninstall();

			System.out.println("Checking if the correct service is imported");
			tb4g = bundleInstaller.installBundle("classloading.tb4g.jar", false);
			tb2a = bundleInstaller.installBundle("classloading.tb2a.jar", true);
			tb2a.start();
			tb4g.start();
			tb4g.stop();
			tb2a.stop();
			tb2a.uninstall();
			tb4g.uninstall();
		}
		catch (BundleException ex) {
			if (ex.getCause() != null) {
				fail(ex.getCause().getMessage());
			}
			else {
				fail(ex.getMessage());
			}
		}
	}

	// Service Events ----------------------------

	/**
	 * Service events must only be delivered to event listeners which can
	 * validly cast the event.
	 *
	 * Tests the case which the event cannot be delivered.
	 *
	 * @osgi.spec ServiceListener.serviceChanged(ServiceEvent)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testServiceListener001() throws Exception {
		Bundle tb1;
		Bundle tb1a;
		Bundle tb2;
		Bundle tb5;
		Bundle tb5a;
		Method method;
		Object service;
		ServiceReference< ? > serviceReference;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		tb1.start();
		tb1a = bundleInstaller.installBundle("classloading.tb1a.jar", true);
		tb2 = bundleInstaller.installBundle("classloading.tb2.jar", true);
		tb2.start();
		tb5 = bundleInstaller.installBundle("classloading.tb5.jar", true);
		tb5.start();

		tb5a = bundleInstaller.installBundle("classloading.tb5a.jar", false);
		try {
			tb5a.start();

			tb1a.start();
			tb1a.stop();

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				// Ignore this exception
			}

			// Check if the some event is delivered
			serviceReference = bundleContext
					.getServiceReference(
							"org.osgi.test.cases.framework.classloading.exports.listener.ServiceListenerTester");
			service = bundleContext.getService(serviceReference);
			method = service.getClass().getMethod("getServiceEventDelivered",
					(Class[]) null);
			assertThat(method.invoke(service, (Object[]) null))
					.as("Checking if an event is delivered for a bundle which imports a non-assignable service interface")
					.isNull();

			bundleContext.ungetService(serviceReference);

			tb5a.stop();
		}
		finally {
			tb5a.uninstall();

			tb5.start();
			tb5.uninstall();
			tb1a.uninstall();
			tb2.stop();
			tb2.uninstall();
			tb1.stop();
			tb1.uninstall();
		}
	}

	/**
	 * Service events must only be delivered to event listeners which can
	 * validly cast the event.
	 *
	 * Tests the case which the event must be delivered.
	 *
	 * @osgi.spec ServiceListener.serviceChanged(ServiceEvent)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testServiceListener002() throws Exception {
		Bundle tb1;
		Bundle tb1a;
		Bundle tb5;
		Bundle tb5b;
		Method method;
		Object service;
		ServiceReference< ? > serviceReference;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		tb1.start();
		tb1a = bundleInstaller.installBundle("classloading.tb1a.jar", true);
		tb5 = bundleInstaller.installBundle("classloading.tb5.jar", true);
		tb5.start();

		tb5b = bundleInstaller.installBundle("classloading.tb5b.jar", false);
		try {
			tb5b.start();

			tb1a.start();
			tb1a.stop();

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				// Ignore this exception
			}

			// Check if the some event is delivered
			serviceReference = bundleContext
					.getServiceReference(
							"org.osgi.test.cases.framework.classloading.exports.listener.ServiceListenerTester");
			service = bundleContext.getService(serviceReference);
			method = service.getClass().getMethod("getServiceEventDelivered",
					(Class[]) null);
			assertThat(method.invoke(service, (Object[]) null))
					.as("Checking if an event is delivered for a bundle which imports an assignable service interface")
					.isNotNull();

			bundleContext.ungetService(serviceReference);

			tb5b.stop();
		}
		finally {
			tb5b.uninstall();

			tb5.start();
			tb5.uninstall();
			tb1a.uninstall();
			tb1.stop();
			tb1.uninstall();
		}
	}

	/**
	 * Some bundles may wish to listen to all service events regardless of what
	 * version of a package they are wired to. A new type of ServiceListener is
	 * added (AllServiceListener) to allow a bundle to listen to all service
	 * events. When an AllServiceLisetener is used the framework does not do
	 * class checks on the ServiceReference for the ServiceEvent before
	 * delivering it to the AllServiceListener.
	 *
	 * @osgi.spec AllServiceListener.serviceChanged(ServiceEvent)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testAllServiceListener001() throws Exception {
		Bundle tb1;
		Bundle tb1a;
		Bundle tb2;
		Bundle tb2a;
		Bundle tb5;
		Bundle tb5c;
		Method method;
		Object service;
		ServiceReference< ? > serviceReference;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		tb1.start();
		tb1a = bundleInstaller.installBundle("classloading.tb1a.jar", true);
		tb2 = bundleInstaller.installBundle("classloading.tb2.jar", true);
		tb2.start();
		tb2a = bundleInstaller.installBundle("classloading.tb2a.jar", true);
		tb5 = bundleInstaller.installBundle("classloading.tb5.jar", true);
		tb5.start();

		tb5c = bundleInstaller.installBundle("classloading.tb5c.jar", false);
		try {
			tb5c.start();

			// This bundle install a service which is assignable to the imported
			// interface
			tb1a.start();
			tb1a.stop();

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				// Ignore this exception
			}

			// Check if the some event is delivered
			serviceReference = bundleContext
					.getServiceReference(
							"org.osgi.test.cases.framework.classloading.exports.listener.AllServiceListenerTester");
			service = bundleContext.getService(serviceReference);
			method = service.getClass().getMethod("getServiceEventDelivered",
					(Class[]) null);
			assertThat(method.invoke(service, (Object[]) null))
					.as("Checking if an event is delivered for a bundle which imports a assignable service interface")
					.isNotNull();

			bundleContext.ungetService(serviceReference);

			// This bundle install a service which is not assignable to the
			// imported interface
			tb2a.start();
			tb2a.stop();

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				// Ignore this exception
			}

			// Check if the some event is delivered
			serviceReference = bundleContext
					.getServiceReference(
							"org.osgi.test.cases.framework.classloading.exports.listener.AllServiceListenerTester");
			service = bundleContext.getService(serviceReference);
			method = service.getClass().getMethod("getServiceEventDelivered",
					(Class[]) null);
			assertThat(method.invoke(service, (Object[]) null))
					.as("Checking if an event is delivered for a bundle which imports a non-assignable service interface")
					.isNotNull();

			bundleContext.ungetService(serviceReference);

			tb5c.stop();
		}
		finally {
			tb5c.uninstall();

			tb5.uninstall();
			tb2a.uninstall();
			tb2.uninstall();
			tb1a.uninstall();
			tb1.uninstall();
		}
	}

	/**
	 * Some bundles may wish to listen to all service events regardless of what
	 * version of a package they are wired to. A new type of ServiceListener is
	 * added (AllServiceListener) to allow a bundle to listen to all service
	 * events. When an AllServiceLisetener is used the framework does not do
	 * class checks on the ServiceReference for the ServiceEvent before
	 * delivering it to the AllServiceListener.
	 *
	 * This test uses an AllServiceListener which is also an
	 * UnfilteredServiceListener. The listener is added with a filter that can
	 * never match (!(objectClass=*)).
	 *
	 * @osgi.spec AllServiceListener.serviceChanged(ServiceEvent)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testUnfilteredAllServiceListener001() throws Exception {
		Bundle tb1;
		Bundle tb1a;
		Bundle tb2;
		Bundle tb2a;
		Bundle tb5;
		Bundle tb5d;
		Method method;
		Object service;
		ServiceReference< ? > serviceReference;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		tb1.start();
		tb1a = bundleInstaller.installBundle("classloading.tb1a.jar", true);
		tb2 = bundleInstaller.installBundle("classloading.tb2.jar", true);
		tb2.start();
		tb2a = bundleInstaller.installBundle("classloading.tb2a.jar", true);
		tb5 = bundleInstaller.installBundle("classloading.tb5.jar", true);
		tb5.start();

		tb5d = bundleInstaller.installBundle("classloading.tb5d.jar", true);
		try {
			tb5d.start();

			// This bundle install a service which is assignable to the imported
			// interface
			tb1a.start();
			tb1a.stop();

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				// Ignore this exception
			}

			// Check if the some event is delivered
			serviceReference = bundleContext
					.getServiceReference(
							"org.osgi.test.cases.framework.classloading.exports.listener.AllServiceListenerTester");
			service = bundleContext.getService(serviceReference);
			method = service.getClass().getMethod("getServiceEventDelivered",
					(Class[]) null);
			assertThat(method.invoke(service, (Object[]) null))
					.as("Checking if an event is delivered for a bundle which imports a assignable service interface")
					.isNotNull();

			bundleContext.ungetService(serviceReference);

			// This bundle install a service which is not assignable to the
			// imported interface
			tb2a.start();
			tb2a.stop();

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
				// Ignore this exception
			}

			// Check if the some event is delivered
			serviceReference = bundleContext
					.getServiceReference(
							"org.osgi.test.cases.framework.classloading.exports.listener.AllServiceListenerTester");
			service = bundleContext.getService(serviceReference);
			method = service.getClass().getMethod("getServiceEventDelivered",
					(Class[]) null);
			assertThat(method.invoke(service, (Object[]) null))
					.as("Checking if an event is delivered for a bundle which imports a non-assignable service interface")
					.isNotNull();

			bundleContext.ungetService(serviceReference);

			tb5d.stop();
		}
		finally {
			tb5d.uninstall();

			tb5.uninstall();
			tb2a.uninstall();
			tb2.uninstall();
			tb1a.uninstall();
			tb1.uninstall();
		}
	}


	// Permission Checking -----------------------

	/**
	 * Since Java SE 9 added support for the Java Platform Module System to Java
	 * SE, The set of java.* provided by the Java platform is no longer constant
	 * for a specific version of the Java Platform. It depends on the set of
	 * modules that are enabled for the runtime instance. The OSGi framework
	 * specification is updated to support requirements on java.* packages.
	 * Explicitly, this means, a bundle can now declare imports for java.*
	 * packages. But, exporting java.* packages is not allowed by a bundle and
	 * doing so will result in an error.
	 * 
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testJavaPackageExplicityExportImport001() {
		Bundle tb7a;
		Bundle tb7b;

		try {
			System.out.println("Installing a bundle which declares imports for java.*");
			tb7a = bundleInstaller.installBundle("classloading.tb7a.jar", false);
			tb7a.uninstall();
		}
		catch (BundleException ex) {
			fail("Should not have thrown the exception: " + ex.toString());
		}

		try {
			System.out.println("Installing a bundle which declares exports for java.*");
			tb7b = bundleInstaller.installBundle("classloading.tb7b.jar", false);
			tb7b.uninstall();
			fail("A bundle which declares exports for java.* packages should fail to install");
		}
		catch (BundleException ex) {
			assertThat(ex.getType())
					.as("It should throw a bundle exception of type manifest error")
					.isEqualTo(BundleException.MANIFEST_ERROR);
		}
	}

	/**
	 * All other packages accessible via the system class path must be hidden
	 * from executing bundles.
	 *
	 * @ Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testHiddenPackages001() throws Exception {
		Bundle tb7c;
		String bootPackages;

		tb7c = bundleInstaller.installBundle("classloading.tb7c.jar", false);
		try {
			try {
				ClassLoader.getSystemClassLoader().loadClass(
						"javax.xml.parsers.SAXParser");
			}
			catch (ClassNotFoundException ex) {
				fail("A class required by this test cannot be loaded");
			}

			bootPackages = bundleContext.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
			if (bootPackages == null) {
				bootPackages = "";
			}
			if (bootPackages != null) {
				StringTokenizer packages = new StringTokenizer(bootPackages, ",");
				while(packages.hasMoreTokens()) {
					String pkg = packages.nextToken().trim();
					if (pkg.equals("*") || pkg.indexOf("org.*") != -1 || pkg.indexOf("org.omg.*") != -1 || pkg.indexOf("org.omg.CORBA") != -1) {
						fail("This test does not run with the org.omg.CORBA package available in '" + Constants.FRAMEWORK_BOOTDELEGATION + "'");
					}
				}
			}

			System.out.println("Installing a bundle which uses a package accessible using the system class loader");
			try {
				tb7c.start();
				tb7c.stop();
				fail("All packages (except java.*) loaded by system class loader must be hidden from executing bundles");
			} catch (BundleException be) {
				// expected
			}
		}
		finally {
			tb7c.uninstall();
		}
	}

	// Class Loading Search Order ----------------

	/**
	 * When an R4 bundle's class loader attempts to load a class or find a
	 * resource, the search must be performed in a specified order.
	 *
	 * If the class or resource is in a java.* package, the request is delegated
	 * to the parent class loader; otherwise the search continues with the next
	 * step. If the request is delegated to the parent class loader and the
	 * class or resource is not found, then the search terminates and the
	 * request fails.
	 *
	 * If the class or resource is a package included in the boot delegation
	 * list, the request is delegated to the parent classloader.
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder001() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb8c;
		Bundle tb8d;
		Class< ? > clazz;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", true);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", true);
		tb8c = bundleInstaller.installBundle("classloading.tb8c.jar", true);

		try {
			tb8d = bundleInstaller.installBundle("classloading.tb8d.jar", true);

			// The loading class must be loaded by the parent class loader
			clazz = tb8d.loadClass("java.lang.Math");
			assertThat(clazz
					.getMethods().length != 0)
					.as("Checking the number of class methods")
					.isTrue();

			// Loading a class which does not exist when loaded by the parent
			// class loader
			try {
				clazz = tb8d.loadClass("java.lang.SomeClass");
				fail("The classes belongs to package java.lang must be loaded only with the parent class loader");
			}
			catch (ClassNotFoundException ex) {
				// Expected exception
			}

			tb8d.uninstall();
		}
		catch (BundleException ex) {
			if (ex.getCause() == null) {
				fail(ex.getMessage());
			}
			else {
				fail(ex.getCause().getMessage());
			}
		}
		finally {

			tb8c.uninstall();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * If the requested class or resource is in a package that is imported using
	 * Import-Package or that is dynamically imported, then the request is
	 * delegated to the exporting bundle's class loader ; otherwise the search
	 * continues with the next step. If the request is delegated to an exporting
	 * class loader and the class or resource is not found, then the search
	 * terminates and the request fails.
	 *
	 * If the requested class or resource is in a package imported by this
	 * module, the request is delegated to the exporting module's class loader.
	 * If the class or resource is not found, then the search terminates and the
	 * request has failed. The search terminates in this case since imported
	 * classes are not treated as split.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder002() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb8c;
		Bundle tb8d;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", true);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", true);
		tb8c = bundleInstaller.installBundle("classloading.tb8c.jar", true);

		try {
			tb8d = bundleInstaller.installBundle("classloading.tb8d.jar", true);

			// This resource must be loaded using the importing bundle
			url = tb8d.getResource("resources/tb8a-resource.txt");

			// Read the resource content to check the loading bundle
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the bundle name")
						.isEqualTo("tb8a");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			// This resource cannot be found when loaded with the class
			// loader of the exporting bundle
			url = tb8d.getResource("resources/tb8b-resource.txt");
			assertThat(url)
					.as("Checking if the resource is incorrectly found")
					.isNull();

			tb8d.uninstall();
		}
		catch (BundleException ex) {
			if (ex.getCause() == null) {
				fail(ex.getMessage());
			}
			else {
				fail(ex.getCause().getMessage());
			}
		}
		finally {
			tb8c.uninstall();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * If the requested class or resource is in a package that is imported from
	 * one or more other bundles using Require-Bundle, the request is delegated
	 * to the class loaders of the other bundles, in the order in which they are
	 * specified in this bundle's manifest. If the class or resource is not
	 * found, then the search continues with the next step.
	 *
	 * The bundle's own internal class path is searched. If the class or
	 * resource is not found, then the search continues with the next step
	 * unless the requested class or resource is in a package that is imported
	 * from one or more other bundles using Require-Bundle, in which case the
	 * search terminates and the request fails.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder003() throws Exception {
		Bundle tb8b;
		Bundle tb8c;
		Bundle tb8e;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", true);
		tb8c = bundleInstaller.installBundle("classloading.tb8c.jar", true);

		tb8e = bundleInstaller.installBundle("classloading.tb8e.jar", true);
		try {
			// This resource must be loaded using the required bundle class
			// loader
			url = tb8e.getResource("resources/tb8b-resource.txt");

			// Read the resource content to check the loading bundle
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the bundle name")
						.isEqualTo("tb8b");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			// This resource must be loaded using the bundle class loader
			url = tb8e.getResource("resources/tb8e-resource.txt");

			// Read the resource content to check the loading bundle
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the bundle name")
						.isEqualTo("tb8e");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			// This resource cannot be found
			url = tb8c.getResource("resources/nonexistent-resource.txt");
			assertThat(url)
					.as("Checking if the resource is incorrectly found")
					.isNull();
		}
		finally {
			tb8e.uninstall();
			tb8c.uninstall();
			tb8b.uninstall();
		}
	}

	/**
	 * Teste if the bundle's own internal class path is searched.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder004() throws Exception {
		Bundle tb8b;
		Bundle tb8c;
		Bundle tb8e;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", true);
		tb8c = bundleInstaller.installBundle("classloading.tb8c.jar", true);

		tb8e = bundleInstaller.installBundle("classloading.tb8e.jar", true);
		try {
			// This resource must be loaded using the bundle class loader
			url = tb8e.getResource("resources/tb8e-resource.txt");

			// Read the resource content to check the loading bundle
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the bundle name")
						.isEqualTo("tb8e");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			// This resource cannot be found
			url = tb8c.getResource("resources/nonexistent-resource.txt");
			assertThat(url)
					.as("Checking if the resource is incorrectly found")
					.isNull();
		}
		finally {
			tb8e.uninstall();
			tb8c.uninstall();
			tb8b.uninstall();
		}
	}

	/**
	 * If the requested class or resource is in a package that is imported using
	 * DynamicImport-Package, then a dynamic import of the package is now
	 * attempted, otherwise the search terminates and the request fails. If the
	 * dynamic import of the package is established, the request is delegated to
	 * the exporting bundle's class loader. If the request is delegated to an
	 * exporting class loader and the class or resource is not found, then the
	 * search terminates and the request fails.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder005() throws Exception {
		Bundle tb8c;
		Bundle tb8f;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8c = bundleInstaller.installBundle("classloading.tb8c.jar", true);

		tb8f = bundleInstaller.installBundle("classloading.tb8f.jar", true);
		try {
			// This resource must be loaded using the dynamic importing bundle
			url = tb8f.getResource("resources/tb8c-resource.txt");

			// Read the resource content to check the loading bundle
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the bundle name")
						.isEqualTo("tb8c");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			// This resource cannot be found
			url = tb8f.getResource("resources/nonexistent-resource.txt");
			assertThat(url)
					.as("Checking if the resource is incorrectly found")
					.isNull();
		}
		finally {
			tb8f.uninstall();
			tb8c.uninstall();
		}
	}

	// RFC 0079 - 5.1.5.2 Class Loading Search Order for R3 Compatibility -----

	/**
	 * Since previous versions of the specification were incomplete and/or
	 * contradictory regarding the visibility of packages provided via the
	 * system class path, it is possible that legacy bundles exist that violate
	 * the rules defined above. To smooth this transition with legacy bundles, a
	 * framework must modify its behavior slightly for R3 or lower bundles. For
	 * such legacy bundles, the following class/resource search order is
	 * acceptable:
	 *
	 * Delegate the request to the parent class loader and if the class or
	 * resource is found, return this result. If the class or resource is in a
	 * java.* package and it was not found by the parent class loader, then the
	 * request fails.
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder006() throws Exception {
		Bundle tb9a;
		Bundle tb9b;
		Class< ? > clazz;

		tb9a = bundleInstaller.installBundle("classloading.tb9a.jar", true);

		tb9b = bundleInstaller.installBundle("classloading.tb9b.jar", true);
		try {
			// The loading class must be loaded by the parent class loader
			clazz = tb9b.loadClass("java.lang.Math");
			assertThat(clazz
					.getMethods().length != 0)
					.as("Checking the number of class methods")
					.isTrue();

			// Loading a class which does not exist when loaded by the parent
			// class loader
			try {
				clazz = tb9b.loadClass("java.lang.SomeClass");
				fail("The classes belongs to package java.lang must be loaded only with the parent class loader");
			}
			catch (ClassNotFoundException ex) {
				// Expected exception
			}
		}
		finally {
			tb9b.uninstall();
			tb9a.uninstall();
		}
	}

	/**
	 * If the requested class or resource is in a package imported, either
	 * statically or dynamically, by this bundle, then the request is delegated
	 * to the exporting bundle's class loader ; otherwise the search continues
	 * with the next step. If the request is delegated to an exporting bundle's
	 * class loader and the class or resource is not found, then the search
	 * terminates and the request fails since imported packages are not treated
	 * as split.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder007() throws Exception {
		Bundle tb9a;
		Bundle tb9c;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb9a = bundleInstaller.installBundle("classloading.tb9a.jar", true);

		tb9c = bundleInstaller.installBundle("classloading.tb9c.jar", true);
		try {
			// This resource must be loaded using the importing bundle class
			// loader
			url = tb9c.getResource("resources/tb9a-resource.txt");

			// Read the resource content to check the loading bundle
			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the bundle name")
						.isEqualTo("tb9a");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			// This resource cannot be found
			url = tb9c.getResource("resources/nonexistent-resource.txt");
			assertThat(url)
					.as("Checking if the resource is incorrectly found")
					.isNull();
		}
		finally {
			tb9c.uninstall();
			tb9a.uninstall();
		}
	}

	/**
	 * The bundle's own internal class path is searched. If the class or
	 * resource is not found, then the search terminates and the request fails.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testClassLoadingSearchOrder008() throws Exception {
		Bundle tb9d;
		URL url;

		tb9d = bundleInstaller.installBundle("classloading.tb9d.jar", true);
		try {
			// This resource must be loaded using the bundle class path
			url = tb9d.getResource("resources/tb9d-resource.txt");
			assertThat(url)
					.as("Checking if the resource is correctly found")
					.isNotNull();

			// This resource cannot be found
			url = tb9d.getResource("resources/nonexistent-resource.txt");
			assertThat(url)
					.as("Checking if the resource is incorrectly found")
					.isNull();
		}
		finally {
			tb9d.uninstall();
		}
	}

	// Bundles and Modules -----------------------------------

	/**
	 * Bundles are related to modules as follows: a given bundle archive defines
	 * a module; installing a bundle creates a module; updating a bundle may
	 * create another module based on the updated bundle archive and so there
	 * may be a one-many relationship between the bundle object and the modules
	 * created for the bundle. However, a bundle archive will not create more
	 * than one module unless it is modified.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.update(InputStream)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleModule001() throws Exception {
		long bundleId;
		Bundle tb1;
		InputStream inputStream;
		URL url;

		// Install the bundle
		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		bundleId = tb1.getBundleId();

		try {
			// Update the bundle with the same archive
			url = bundleContext.getBundle().getEntry("classloading.tb1.jar");
			inputStream = url.openStream();
			tb1.update(inputStream);
			inputStream.close();

			// Bundle id must not change after update
			assertThat(bundleId == tb1.getBundleId())
					.as("Bundle id must not change after update")
					.isTrue();
		}
		finally {
			tb1.uninstall();
		}

		tb1 = bundleInstaller.installBundle("classloading.tb10a.jar", true);

		try {
			// Bundle id must not change after install the new bundle
			assertThat(bundleId != tb1.getBundleId())
					.as("Bundle id must not change after install the new bundle with the same symbolic name and different version")
					.isTrue();
		}
		finally {
			tb1.uninstall();
		}
	}

	// Bundle Symbolic Name ---------------------------------

	/**
	 * Version 2 bundle manifests must specify the bundle symbolic name.
	 *
	 * @osgi.spec BundleContext.start(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleSymbolicName001() throws Exception {
		Bundle tb11a;

		try {
			tb11a = bundleInstaller.installBundle("classloading.tb11a.jar", true);
			tb11a.uninstall();
			fail("A version 2 bundle manifests must specify the bundle symbolic name");
		}
		catch (BundleException ex) {
			// Ignore this exception
		}
	}

	// Singleton Bundles -----------------------------------

	/**
	 * At most one version of a particular singleton bundle may be resolved in
	 * the framework. When multiple versions of a singleton bundle with the same
	 * symbolic name are installed, the framework implementation must select at
	 * most one of the singleton bundles to resolve.
	 *
	 * Only bundles specified as singleton bundles are treated as such. In
	 * particular, singleton bundles do not affect the resolution of non-
	 * singleton bundles with the same symbolic name.
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testSingletonBundle001(
			@InjectInstalledBundle(value = "classloading.tb11b.jar", start = false) Bundle tb11b,
			@InjectInstalledBundle(value = "classloading.tb11c.jar", start = false) Bundle tb11c,
			@InjectInstalledBundle(value = "classloading.tb11d.jar", start = false) Bundle tb11d,
			@InjectInstalledBundle(value = "classloading.tb11e.jar", start = false) Bundle tb11e
			) throws Exception {
		bundleContext.getBundle(0).adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(tb11b, tb11c, tb11d, tb11e));
		if ((tb11b.getState() & Bundle.RESOLVED) != 0)
			tb11b.start();
		if ((tb11c.getState() & Bundle.RESOLVED) != 0)
			tb11c.start();
		if ((tb11d.getState() & Bundle.RESOLVED) != 0)
			tb11d.start();
		if ((tb11e.getState() & Bundle.RESOLVED) != 0)
			tb11e.start();

		assertThat((tb11b.getState() == Bundle.ACTIVE && tb11c.getState() == Bundle.INSTALLED) ||
				(tb11c.getState() == Bundle.ACTIVE && tb11b.getState() == Bundle.INSTALLED))
				.as("Only one singleton bundle may be resolved at the same time")
				.isTrue();
		assertThat(tb11d.getState() == Bundle.ACTIVE
						&& tb11e.getState() == Bundle.ACTIVE)
				.as("More than one non-singleton bundle may be resolved at the same time")
				.isTrue();
	}

	// Installing Bundles ---------------------------------

	/**
	 * If an attempt is made to install a bundle which has the same bundle
	 * symbolic name and bundle version as an already installed bundle, then a
	 * BundleException must be thrown and the install must fail
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testBundleInstall001(
			@InjectInstalledBundle(value = "classloading.tb12a.jar", start = false) Bundle tb12a
			) throws Exception {
		Bundle tb12b;

		try {
			System.out.println("Installing a bundle with the same symbolic name and version of other installed bundle");
			tb12b = bundleInstaller.installBundle("classloading.tb12b.jar", false);
			tb12b.uninstall();
			fail("A bundle with the same symbolic name and version of other cannot be installed");
		}
		catch (BundleException ex) {
			// Ignore this exception
		}
	}

	// Exporting Packages -----------------------------------

	/**
	 * If multiple packages need to be exported with identical parameters, the
	 * syntax permits a list of packages, separated by semicolons, to be
	 * specified before the parameters. The same package may be exported more
	 * than once with different parameters.
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport001() throws Exception {
		Bundle tb13a;
		Bundle tb13o;
		Bundle tb13p;

		try {
			System.out.println("Installing a bundle with correct export syntax");
			tb13a = bundleInstaller.installBundle("classloading.tb13a.jar", true);

			tb13o = bundleInstaller.installBundle("classloading.tb13o.jar", true);
			tb13p = bundleInstaller.installBundle("classloading.tb13p.jar", true);

			tb13p.uninstall();
			tb13o.uninstall();
			tb13a.uninstall();
		}
		catch (BundleException ex) {
			fail("A bundle with correct export syntax cannot be installed");
		}
	}

	/**
	 * Tests the attribute uses.
	 *
	 * uses - a string specifying a list of packages upon which the package(s)
	 * being exported depend. The default is that the list is empty which means
	 * the package(s) being exported do not depend on any other packages.
	 *
	 * If an import is resolved to a module which exports the same package more
	 * than once, the first export statement which matches the import is
	 * selected and only the dependencies specified (via "uses") for that export
	 * statement apply to the importer.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport002() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb13b;
		Bundle tb13c;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", false);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", false);
		tb8a.start();
		tb8b.start();

		tb13b = bundleInstaller.installBundle("classloading.tb13b.jar", false);
		tb13c = bundleInstaller.installBundle("classloading.tb13c.jar", false);

		try {
			tb13b.start();
			tb13c.start();

			System.out.println("Loading a class from the imported package");
			try {
				tb13c
						.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
			}
			catch (ClassNotFoundException ex) {
				fail("The class belongs to an imported package cannot be found");
			}

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb13c.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8b");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			tb13c.stop();
			tb13b.stop();
		}
		finally {
			tb13c.uninstall();
			tb13b.uninstall();

			tb8b.stop();
			tb8a.stop();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * Packages in the list which are neither exported nor imported by the
	 * current module are ignored and do not create dependencies.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport003() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb13c;
		Bundle tb13d;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", false);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", false);
		tb8a.start();
		tb8b.start();

		tb13d = bundleInstaller.installBundle("classloading.tb13d.jar", false);
		tb13c = bundleInstaller.installBundle("classloading.tb13c.jar", false);

		try {
			tb13d.start();
			tb13c.start();

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb13c.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8a");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			tb13d.stop();
			tb13c.stop();
		}
		finally {
			tb13d.uninstall();
			tb13c.uninstall();

			tb8b.stop();
			tb8a.stop();
			tb8b.uninstall();
			tb8a.uninstall();
		}

		// Repeat the test changing the order of package exporters

		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", false);
		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", false);
		tb8b.start();
		tb8a.start();

		tb13d = bundleInstaller.installBundle("classloading.tb13d.jar", false);
		tb13c = bundleInstaller.installBundle("classloading.tb13c.jar", false);

		try {
			tb13d.start();
			tb13c.start();

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb13c.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8b");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			tb13d.stop();
			tb13c.stop();
		}
		finally {
			tb13d.uninstall();
			tb13c.uninstall();

			tb8a.stop();
			tb8b.stop();
			tb8a.uninstall();
			tb8b.uninstall();
		}
	}

	/**
	 * Test the attributes uses.
	 *
	 * uses - The framework forms the transitive closure of the dependency
	 * relationship. For example, if a bundle exports package p, q, and r and
	 * declares that P uses Q and Q uses R, then the framework will behave as if
	 * the bundle also declared that P uses R.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport004() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb13e;
		Bundle tb13f;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", false);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", false);
		tb8a.start();
		tb8b.start();

		tb13e = bundleInstaller.installBundle("classloading.tb13e.jar", false);
		tb13f = bundleInstaller.installBundle("classloading.tb13f.jar", false);

		try {
			tb13e.start();
			tb13f.start();

			System.out.println("Loading a class from the imported package");
			try {
				tb13f
						.loadClass("org.osgi.test.cases.framework.classloading.exports.security.SomePermission");
			}
			catch (ClassNotFoundException ex) {
				fail("The class belongs to an imported package cannot be found");
			}

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb13f.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8b");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			tb13f.stop();
			tb13e.stop();
		}
		finally {
			tb13f.uninstall();
			tb13e.uninstall();

			tb8b.stop();
			tb8a.stop();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * Tests the attribute mandatory when the bundle import does matches the
	 * required attributes.
	 *
	 * mandatory - a string with no default value. The list specifies names of
	 * matching attributes which must be specified by matching Import-Package
	 * statements.
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport005(
			@InjectInstalledBundle(value = "classloading.tb13g.jar", start = true) Bundle tb13g,
			@InjectInstalledBundle(value = "classloading.tb13h.jar", start = true) Bundle tb13h
			) throws Exception {
		System.out.println("Loading a class from the imported package");
		try {
			tb13h
					.loadClass("org.osgi.test.cases.framework.classloading.exports.listener.ServiceListenerTester");
		}
		catch (ClassNotFoundException ex) {
			fail("The class belongs to an imported package must be found");
		}
	}

	/**
	 * Tests the attribute mandatory when the bundle import does not matches the
	 * required attributes.
	 *
	 * mandatory - a string with no default value. The list specifies names of
	 * matching attributes which must be specified by matching Import-Package
	 * statements.
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport006(
			@InjectInstalledBundle(value = "classloading.tb13g.jar", start = true) Bundle tb13g,
			@InjectInstalledBundle(value = "classloading.tb13i.jar", start = false) Bundle tb13i
			) throws Exception {
		try {
			System.out.println("Starting a bundle which incorrectly imports a package");
			tb13i.start();
			fail("The bundle cannot start without the mandatory attributes required by the exported package");
		}
		catch (BundleException ex) {
			// expected
		}
	}

	/**
	 * Tests the attributes include and exclude.
	 *
	 * include/exclude - a list of classes of the specified packages which must
	 * be allowed to be exported in the case of include or prevented from being
	 * exported in the case of exclude. This acts as a class load time filter
	 * and does not affect module resolution in any way.
	 *
	 * Filtering works as follows. The name of a class being loaded is checked
	 * against the include list and the exclude list. If it matches an entry in
	 * the include list but does not match any entry in the exclude list, then
	 * loading proceeds normally. In all other cases, loading fails and a
	 * ClassNotFound exception is thrown. Note that the ordering of includes and
	 * excludes is not significant.
	 *
	 * Note that the syntax permits zero or more wildcards to occur at arbitrary
	 * positions.
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport007(
			@InjectInstalledBundle(value = "classloading.tb13j.jar", start = true) Bundle tb13j,
			@InjectInstalledBundle(value = "classloading.tb13o.jar", start = true) Bundle tb13o
			) throws Exception {
		System.out.println("Loading a class which matchs the include list");
		try {
			tb13o
					.loadClass("org.osgi.test.cases.framework.classloading.exports.listener.ServiceListenerTester");
		}
		catch (ClassNotFoundException ex) {
			fail("The class belongs the exported package must be found");
		}

		System.out.println("Loading a class which matchs the exclude list");
		assertThatExceptionOfType(ClassNotFoundException.class)
				.as("The class belongs the exported package must not be found")
				.isThrownBy(() -> {
					tb13o
							.loadClass("org.osgi.test.cases.framework.classloading.exports.listener.AllServiceListenerTester");
				});
	}

	/**
	 * Tests the attributes version and specification-version.
	 *
	 * version - the version of the named packages with syntax. The default
	 * value is "0.0.0".
	 *
	 * specification-version - this is a deprecated synonym for the version
	 * attribute with the sole exception that, if both specification- version
	 * and version are specified (for the same package(s)), they must have
	 * identical values. If both are specified but with different values, the
	 * bundle fails to install.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport008() throws Exception {
		Bundle tb13k;
		Bundle tb13l;

		System.out.println("Installing a bundle with an exported package having the same value for version and specification-version attributes");
		try {
			tb13k = bundleInstaller.installBundle("classloading.tb13k.jar", false);
			tb13k.uninstall();
		}
		catch (BundleException ex) {
			fail("A bundle with an exported package having the same value for version and specification-version attributes must be installed");
		}

		try {
			System.out.println("Installing a bundle with an exported package having different values for version and specification-version attributes");
			tb13l = bundleInstaller.installBundle("classloading.tb13l.jar", false);
			tb13l.uninstall();
			fail("A bundle with an exported package having different values for version and specification-version attributes must fail to install");
		}
		catch (BundleException ex) {
			// expected
		}
	}

	/**
	 * Test the attributes bundle-symbolic-name and bundle-version.
	 *
	 * bundle-symbolic-name - the bundle symbolic name of the exporting bundle
	 * is implicitly associated with the export statement. The export statement
	 * must not specify an explicit bundle symbolic name.
	 *
	 * bundle-version - the bundle version of the exporting bundle is implicitly
	 * associated with the export statement. The export statement must not
	 * specify an explicit bundle version.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageExport009() throws Exception {
		Bundle tb13m;

		try {
			System.out.println("Installing a bundle with an exported package which incorrectly specify bundle-symbolic-name and bundle-version");
			tb13m = bundleInstaller.installBundle("classloading.tb13m.jar", false);
			tb13m.uninstall();
			fail("A bundle with an exported package which incorrectly specify bundle-symbolic-name and bundle-version must fail to install");
		}
		catch (BundleException ex) {
			// expected
		}
	}

	// Importing Packages -----------------------------------

	/**
	 * If multiple packages need to be imported with identical parameters, the
	 * syntax permits a list of packages, separated by semicolons, to be
	 * specified before the parameters.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport001() throws Exception {
		Bundle tb8a;
		Bundle tb15a;
		Bundle tb15b;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", true);
		tb15a = bundleInstaller.installBundle("classloading.tb15a.jar", true);

		try {
			// Install a bundle which import packages
			tb15b = bundleInstaller.installBundle("classloading.tb15b.jar", false);

			// Check if packages are correctly imported
			System.out.println("Loading a class from the imported package");
			try {
				tb15b
						.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
			}
			catch (ClassNotFoundException ex) {
				fail("A class from imported class cannot be found");
			}

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb15b.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb15a");
			}
			finally {
				reader.close();
				inputStream.close();
			}

			tb15b.uninstall();
		}
		finally {
			tb15a.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * Test the attribute resolution with value mandatory.
	 *
	 * resolution - a string taking one of the values "mandatory" or "optional".
	 * The default value is "mandatory".
	 *
	 * "mandatory" indicates that the import must be resolved when the importing
	 * module is resolved. If such an import cannot be resolved and the
	 * importing module does not also export the specified packages, the module
	 * fails to resolve.
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport002() throws Exception {
		Bundle tb1;
		Bundle tb15c;

		// Try to install when the imported package is not available
		tb15c = bundleInstaller.installBundle("classloading.tb15c.jar", false);

		try {
			System.out.println("Starting a bundle with an import package that is not available");
			tb15c.start();
			fail("The bundle must not be resolved, since the imported package is not available");
		}
		catch (BundleException ex) {
			// expected
		}
		finally {
			tb15c.uninstall();
		}

		// Try to install when the imported package is available
		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);

		try {
			// Install a bundle which import package
			tb15c = bundleInstaller.installBundle("classloading.tb15c.jar", false);

			try {
				System.out.println("Starting a bundle with an import package that is available");
				tb15c.start();
			}
			catch (BundleException ex) {
				fail("The bundle must be resolved, since the imported package is available");
			}

			// Check if packages are correctly imported
			System.out.println("Loading a class from the imported package");
			try {
				tb15c
						.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
			}
			catch (ClassNotFoundException ex) {
				fail("A class from imported class cannot be found");
			}

			tb15c.uninstall();
		}
		finally {
			tb1.uninstall();
		}
	}

	/**
	 * Test the attibute resolution with value "optional".
	 *
	 * "optional" indicates that the import is optional and the importing module
	 * may be resolved without the import being resolved. If the import is not
	 * resolved when the module is resolved, the import may not be resolved
	 * before the module is re-resolved.
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport003(
			@InjectInstalledBundle(value = "classloading.tb15d.jar", start = false) Bundle tb15d
			) throws Exception {
		Bundle tb1;

		// Try to install when the imported package is not available

		try {
			System.out.println("Starting a bundle with an import package that is not available");
			tb15d.start();
		}
		catch (BundleException ex) {
			fail("The bundle must be resolved, even though the imported package is not available");
		}

		// Now, install a bundle with the imported package
		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);

		FrameworkWirings.refreshBundles(bundleContext, tb15d);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) {
			// Ignore this exception
		}

		// Check if the package is available
		try {
			tb15d
					.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
		}
		catch (ClassNotFoundException ex) {
			fail("The package must be available after resolve the bundle");
		}
		finally {
			tb1.uninstall();
		}
	}

	/**
	 * Tests the attributes version and specification-version.
	 *
	 * version - a version range to select the (re-)exporter's implementation
	 * version. The default value is "0.0.0".
	 *
	 * specification-version - this is a deprecated synonym for the version
	 * attribute with the sole exception that, if both specification- version
	 * and version are specified (for the same package(s)), they must have
	 * identical values. If both are specified but with different values, the
	 * bundle fails to install.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport004(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1
			) throws Exception {
		Bundle tb15e;
		Bundle tb15f;

		System.out.println("Installing a bundle with an import package having the same value for version and specification-version attributes");
		try {
			tb15e = bundleInstaller.installBundle("classloading.tb15e.jar", false);
			tb15e.uninstall();
		}
		catch (BundleException ex) {
			fail("A bundle with an imported package having the same value for version and specification-version attributes must be installed");
		}

		try {
			System.out.println("Installing a bundle with an import package having different values for version and specification-version attributes");
			tb15f = bundleInstaller.installBundle("classloading.tb15f.jar", false);
			tb15f.uninstall();
			fail("A bundle with an imported package having different values for version and specification-version attributes must fail to install");
		}
		catch (BundleException ex) {
			// expected
		}
	}

	/**
	 * Tests the attributes bundle-symbolic-name and bundle-version.
	 *
	 * bundle-symbolic-name - the bundle symbolic name of the (re-)exporting
	 * bundle.
	 *
	 * bundle-version - a version range to select the bundle version of the
	 * (re-)exporting bundle. The default value is "0.0.0".
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport005(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb15g.jar", start = false) Bundle tb15g
			) throws Exception {
		try {
			System.out.println("Starting a bundle which imports a package specifying the bundle name and version");
			tb15g.start();
			tb15g.stop();
		}
		catch (BundleException ex) {
			fail("A bundle which imports a package specifying the bundle name and version must be started");
		}
	}

	/**
	 * A module which imports a given package more than once is in error and
	 * fails to resolve.
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport006() throws Exception {
		Bundle tb1;
		Bundle tb15h;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		tb15h = null;

		try {
			tb15h = bundleInstaller.installBundle("classloading.tb15h.jar", false);

			System.out.println("Starting a bundle which imports a given package more than once");
			tb15h.start();
			tb15h.stop();
			fail("A bundle which imports a package more than once must not be resolved");
		}
		catch (BundleException ex) {
			// expected
		}
		finally {
			if (tb15h != null) {
				tb15h.uninstall();
			}
			tb1.uninstall();
		}
	}

	/**
	 * If an import matches exports in more than one module, the module with
	 * lowest bundle identifier is selected.
	 *
	 * @osgi.spec Bundle.getBundleId()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageImport007() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb15i;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", true);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", true);
		tb15i = bundleInstaller.installBundle("classloading.tb15i.jar", true);

		try {
			// Check that tb8a has the lowest bundle id
			if (tb8a.getBundleId() > tb8b.getBundleId()) {
				fail("This test requires that the id of first installed bundle greatest than the id of others bundles");
			}

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb15i.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8a");
			}
			finally {
				reader.close();
				inputStream.close();
			}
		}
		finally {
			tb15i.uninstall();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	// Dynamically Importing Packages -----------------------

	/**
	 * Dynamic imports are matched to exports (to form package wirings) during
	 * class loading and do not affect module resolution.
	 *
	 * @osgi.spec Bundle.start()
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport001(
			@InjectInstalledBundle(value = "classloading.tb17a.jar", start = false) Bundle tb17a
			) throws Exception {
		try {
			System.out.println("Starting a bundle importing dynamically an exported package");
			tb17a.start();
			tb17a.stop();
		}
		catch (BundleException ex) {
			fail("When using dynamic imports, a bundle must be started even tough the imported package is not available");
		}
	}

	/**
	 * Packages may be named explicitly or by using wild-carded expressions such
	 * as "org.foo.*" and "*".
	 *
	 * If multiple packages need to be dynamically imported with identical
	 * parameters, the syntax permits a list of packages, separated by
	 * semicolons, to be specified before the parameters.
	 *
	 * Test the attribute bundle-version.
	 *
	 * bundle-version - a version range to select the bundle version of the (re-
	 * )exporting bundle. The default value is "0.0.0".
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport002() throws Exception {
		Bundle tb1;
		Bundle tb8a;
		Bundle tb17b;
		URL url;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);
		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", true);
		tb17b = bundleInstaller.installBundle("classloading.tb17b.jar", true);

		try {
			System.out.println("Loading resources from a dynamic imported package");

			try {
				tb17b
						.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
			}
			catch (ClassNotFoundException ex) {
				fail("Cannot load a class from a dynamic imported package");
			}

			url = tb17b.getResource("resources/tb8a-resource.txt");
			if (url == null) {
				fail("Cannot load a resource from a dynamic imported package");
			}
		}
		finally {
			tb17b.uninstall();
			tb8a.uninstall();
			tb1.uninstall();
		}
	}

	/**
	 * Dynamic imports are searched in the order in which they are specified.
	 * The order is particularly important when wildcarded package names are
	 * used as the order will then determine the order in which matching occurs. *
	 * Test the attribute bundle-symbolic-name.
	 *
	 * bundle-symbolic-name - the bundle symbolic name of the (re-)exporting
	 * bundle.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport003() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb17c;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", true);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", true);
		tb17c = bundleInstaller.installBundle("classloading.tb17c.jar", true);

		try {
			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb17c.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8b");
			}
			finally {
				reader.close();
				inputStream.close();
			}
		}
		finally {
			tb17c.uninstall();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * Dynamic imports may not be used when the package is exported.
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport004(
			@InjectInstalledBundle(value = "classloading.tb1.jar", start = true) Bundle tb1,
			@InjectInstalledBundle(value = "classloading.tb17i.jar", start = true) Bundle tb17i,
			@InjectInstalledBundle(value = "classloading.tb17j.jar", start = true) Bundle tb17j
			) throws Exception {
		assertThatExceptionOfType(ClassNotFoundException.class)
				.as("bundle does not export package")
				.isThrownBy(() -> {
					tb17i
							.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
				});
		assertThatExceptionOfType(ClassNotFoundException.class)
				.as("The dynamic import must not be propagated")
				.isThrownBy(() -> {
					tb17j
							.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
				});
	}

	/**
	 * Test the attributes version and specification-version.
	 *
	 * version - a version range to select the exporter's implementation
	 * version. The default value is "0.0.0".
	 *
	 * specification-version - this is a deprecated synonym for the version
	 * attribute with the sole exception that, if both specification- version
	 * and version are specified (for the same package(s)), they must have
	 * identical values. If both are specified but with different values, the
	 * bundle fails to install.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport005() throws Exception {
		Bundle tb1;
		Bundle tb17d;
		Bundle tb17e;

		tb1 = bundleInstaller.installBundle("classloading.tb1.jar", true);

		tb17d = null;
		try {
			System.out.println("Installing a bundle with a dynamic imported package having the same value for version and specification-version attributes");
			tb17d = bundleInstaller.installBundle("classloading.tb17d.jar", true);

			System.out.println("Loading a class from a dynamic imported package");
			tb17d
					.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
		}
		catch (ClassNotFoundException ex) {
			fail("The class must be found when belongs a dynamic imported package");
		}
		finally {
			if ((tb17d != null) && (tb17d.getState() != Bundle.UNINSTALLED)) {
				tb17d.uninstall();
			}

			tb1.uninstall();
		}

		try {
			System.out.println("Installing a bundle with a dynamic imported package having different values for version and specification-version attributes");
			tb17e = bundleInstaller.installBundle("classloading.tb17e.jar", false);
			tb17e.uninstall();
			fail("A bundle with a dynamic imported package having different values for version and specification-version attributes must fail to install");
		}
		catch (BundleException ex) {
			// expected
		}
	}

	/**
	 * In order for a DynamicImport-Package to be resolved to an export
	 * statement, any arbitrary attributes specified must match the attributes
	 * of the export statement. However, a match is not prevented if the export
	 * statement includes attributes which are not specified by the
	 * corresponding DynamicImport-Package statement, unless one or more of
	 * those attributes are exported as required (using the "mandatory"
	 * directive).
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport006(
			@InjectInstalledBundle(value = "classloading.tb13g.jar", start = true) Bundle tb13g,
			@InjectInstalledBundle(value = "classloading.tb17g.jar", start = true) Bundle tb17g
			) throws Exception {
		System.out.println("Loading a class from a dynamic imported package");
		try {
			tb17g
					.loadClass("org.osgi.test.cases.framework.classloading.exports.listener.ServiceListenerTester");
		}
		catch (ClassNotFoundException ex) {
			fail("The class belongs to a dynamic imported package must be found");
		}
	}

	/**
	 * In order for a DynamicImport-Package to be resolved to an export
	 * statement, any arbitrary attributes specified must match the attributes
	 * of the export statement. However, a match is not prevented if the export
	 * statement includes attributes which are not specified by the
	 * corresponding DynamicImport-Package statement, unless one or more of
	 * those attributes are exported as required (using the "mandatory"
	 * directive).
	 *
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testPackageDynamicImport007(
			@InjectInstalledBundle(value = "classloading.tb13g.jar", start = true) Bundle tb13g,
			@InjectInstalledBundle(value = "classloading.tb17h.jar", start = true) Bundle tb17h
			) throws Exception {
		System.out.println("Loading a class from a dynamic imported package");
		assertThatExceptionOfType(ClassNotFoundException.class)
				.as("The class belongs to a dynamic imported package must not be found")
				.isThrownBy(() -> {
					tb17h
							.loadClass("org.osgi.test.cases.framework.classloading.exports.listener.ServiceListenerTester");
				});
	}

	// Requiring a Bundle -------------------------------------
	// Require Bundle Cycles --------------------------------

	/**
	 * Test that the Require-Bundle manifest header imports all the exported
	 * packages of one or more named bundles. A given package may be exported by
	 * more than one of the named bundles in which case it is treated as a split
	 * package and the bundles are searched in the order in which they were
	 * specified after which the requiring bundle is searched for any additional
	 * classes of the package.
	 *
	 * Tests that multiple bundles may export the same package. Bundles which
	 * export the same package that are involved in a require bundle cycle can
	 * lead to lookup cycles when searching for classes and resources from the
	 * package.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle001() throws Exception {
		Bundle tb16b = bundleInstaller.installBundle("classloading.tb16b.jar", false);
		Bundle tb16c = bundleInstaller.installBundle("classloading.tb16c.jar", false);
		Bundle tb16a = bundleInstaller.installBundle("classloading.tb16a.jar", false);
		tb16b.start();
		tb16c.start();
		tb16a.start();
		URL url = tb16a.getResource("test/package/resources/resource.txt");
		try {
			assertThat(url)
					.as("Expecting to find resource")
					.isNotNull();

			BufferedReader bufr = new BufferedReader(new InputStreamReader(url
					.openStream()));
			String line = bufr.readLine();
			bufr.close();
			assertThat(line)
					.as("Expecting to get resource from classloading.tb16c")
					.isEqualTo("tb16c");

			// Test that own resources are available to requiring bundle
			url = tb16a
					.getResource("test/package/resources/tb16a-resource.txt");
			assertThat(url)
					.as("Expecting to find the resource in requiring bundle")
					.isNotNull();
		}
		finally {
			tb16a.stop();
			tb16b.stop();
			tb16c.stop();
			tb16a.uninstall();
			tb16b.uninstall();
			tb16c.uninstall();
		}
	}

	/**
	 * When searching for classes in a required bundle, the framework should
	 * only search the packages (re- ) exported by the required bundle. Any
	 * other packages contained in the required bundle must not be searched.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle002() throws Exception {
		Bundle tb16b = bundleInstaller.installBundle("classloading.tb16b.jar", false);
		Bundle tb16c = bundleInstaller.installBundle("classloading.tb16c.jar", false);
		Bundle tb16a = bundleInstaller.installBundle("classloading.tb16a.jar", false);
		URL url = tb16a
				.getResource("org/osgi/test/cases/framework/classloading/tb16b/TestClass.class");
		try {
			assertThat(url)
					.as("Not expecting to find the resource in required bundle")
					.isNull();
		}
		finally {
			tb16a.uninstall();
			tb16b.uninstall();
			tb16c.uninstall();
		}
	}

	/**
	 * Test visibility directive with value of "private" which indicates that
	 * any packages that are exported by the required bundle are not made
	 * visible on the export signature of the requiring bundle.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle004(
			@InjectInstalledBundle(value = "classloading.tb16b.jar", start = false) Bundle tb16b,
			@InjectInstalledBundle(value = "classloading.tb16k.jar", start = false) Bundle tb16a,
			@InjectInstalledBundle(value = "classloading.tb16i.jar", start = false) Bundle tb16i
			) throws Exception {
		tb16a.start();
		tb16b.start();
		tb16i.start();
		URL url = tb16i
				.getResource("test/package/resources/tb16b-resource.txt");
		assertThat(url)
				.as("Not expecting to find the resource tb16b-resource.txt")
				.isNull();
	}

	/**
	 * Test that when the resolution directive has a value of "optional" the the
	 * requiring module may be resolved without the required module being
	 * resolved. If the required bundle is not resolved when the requiring
	 * bundle is resolved, the requiring bundle is not affected if the required
	 * bundle may subsequently be resolved until such time as the requiring
	 * bundle is re-resolved.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle005() throws Exception {
		// Install and start requiring bundle
		Bundle tb16e = bundleInstaller.installBundle("classloading.tb16e.jar", false);
		tb16e.start(); // this will cause requiring bundle to be resolved

		// Install and start required bundle
		Bundle tb16b = bundleInstaller.installBundle("classloading.tb16b.jar", false);
		tb16b.start(); // this will cause required bundle to be resolved
		try {
			// The resource in required bundle is not expected
			URL url = tb16e.getResource("test/package/resources/resource.txt");
			assertThat(url)
					.as("Not expecting to find resource from required bundle")
					.isNull();

		}
		finally {
			tb16b.stop();
			tb16b.uninstall();
			tb16e.stop();
			tb16e.uninstall();
		}
	}

	/**
	 * Test bundle-version attribute allows a version-range to select the bundle
	 * version of the required bundle.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle006() throws Exception {
		// Install and start required bundle
		Bundle tb16b = bundleInstaller.installBundle("classloading.tb16b.jar", false);
		tb16b.start(); // this will cause required bundle to be resolved

		// Install and start requiring bundle
		Bundle tb16f = bundleInstaller.installBundle("classloading.tb16f.jar", false);
		tb16f.start(); // this will cause requiring bundle to be resolved

		try {
			// The resource in required bundle is not expected
			URL url = tb16f.getResource("test/package/resources/resource.txt");
			assertThat(url)
					.as("Not expecting to find resource from required bundle")
					.isNull();

		}
		finally {
			tb16b.stop();
			tb16b.uninstall();
			tb16f.stop();
			tb16f.uninstall();
		}
	}

	/**
	 * A bundle may both import packages (via Import-Package) and require one or
	 * more bundles (via Require-Bundle), but if a package is imported via
	 * Import-Package it is not also imported via Require-Bundle: Import-
	 * Package takes priority over Require-Bundle and packages which are
	 * exported by a required bundle and imported via Import-Package are not
	 * treated as split
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle007() throws Exception {
		Bundle tb16b = bundleInstaller.installBundle("classloading.tb16b.jar", false);
		tb16b.start();
		Bundle tb16g = bundleInstaller.installBundle("classloading.tb16g.jar", false);
		tb16g.start();
		URL url = tb16g.getResource("test/package/resources/resource.txt");
		try {
			assertThat(url)
					.as("Expecting to find resource")
					.isNotNull();
			BufferedReader bufr = new BufferedReader(new InputStreamReader(url
					.openStream()));
			String line = bufr.readLine();
			bufr.close();
			assertThat(line)
					.as("Expecting to get resource from classloading.tb16b")
					.isEqualTo("tb16b");
			url = tb16g
					.getResource("test/package/resources/tb16g-resource.txt");
			assertThat(url)
					.as("Not expecting to find the resource in requiring bundle")
					.isNull();
		}
		finally {
			tb16g.stop();
			tb16b.stop();
			tb16g.uninstall();
			tb16b.uninstall();
		}
	}

	/**
	 * Tests that if the required bundle exports the same package more than
	 * once, the first export statement is used and only the dependencies
	 * specified (via "uses") for that export statement apply to the requiring
	 * bundle.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @osgi.spec Bundle.loadClass(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle008() throws Exception {
		Bundle tb8a;
		Bundle tb8b;
		Bundle tb13b;
		Bundle tb16h;
		BufferedReader reader;
		InputStream inputStream;
		URL url;

		// start bundles that export different versions of the same package
		tb8a = bundleInstaller.installBundle("classloading.tb8a.jar", false);
		tb8b = bundleInstaller.installBundle("classloading.tb8b.jar", false);
		tb8a.start();
		tb8b.start();

		// start required bundle that exports the package used by the
		// requiring bundle
		tb13b = bundleInstaller.installBundle("classloading.tb13b.jar", false);
		tb13b.start();

		// start requiring bundle
		tb16h = bundleInstaller.installBundle("classloading.tb16h.jar", false);
		tb16h.start();

		try {

			System.out.println("Loading a class from the imported package");
			try {
				tb16h
						.loadClass("org.osgi.test.cases.framework.classloading.exports.service.SomeService");
			}
			catch (ClassNotFoundException ex) {
				fail("The class belongs to an imported package cannot be found");
			}

			// Read the resource content to check the loading bundle
			System.out.println("Loading a resource and checking its loading bundle");
			url = tb16h.getResource("/resources/tb8a-resource.txt");

			inputStream = url.openStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				assertThat(reader
						.readLine())
						.as("Checking the loading bundle name")
						.isEqualTo("tb8b");
			}
			finally {
				reader.close();
				inputStream.close();
			}

		}
		finally {
			tb16h.stop();
			tb13b.stop();
			tb16h.uninstall();
			tb13b.uninstall();

			tb8b.stop();
			tb8a.stop();
			tb8b.uninstall();
			tb8a.uninstall();
		}
	}

	/**
	 * Test visibility directive with value of "reexport" which indicates that
	 * any packages that are exported by the required bundle are re-exported by
	 * the requiring bundle. Any arbitrary matching attributes with which they
	 * were exported by the required bundle are deleted.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception If any exception occurs or an assert fails
	 */
	@Test
	public void testRequiredBundle009(
			@InjectInstalledBundle(value = "classloading.tb16b.jar", start = false) Bundle tb16b,
			@InjectInstalledBundle(value = "classloading.tb16c.jar", start = false) Bundle tb16c,
			@InjectInstalledBundle(value = "classloading.tb16a.jar", start = false) Bundle tb16a,
			@InjectInstalledBundle(value = "classloading.tb16i.jar", start = false) Bundle tb16i
			) throws Exception {
		tb16a.start();
		tb16b.start();
		tb16c.start();
		tb16i.start();
		// test if resources from package re-exported by tb16a is available
		// in tb16i due to the visibility directive being set to "reexport"
		URL url = tb16i
				.getResource("test/package/resources/tb16a-resource.txt");
		assertThat(url)
				.as("Expecting to find the resource tb16a-resource.txt")
				.isNotNull();
		url = tb16i
				.getResource("test/package/resources/tb16b-resource.txt");
		assertThat(url)
				.as("Expecting to find the resource tb16b-resource.txt")
				.isNotNull();
	}

	/**
	 * Tests that multiple bundles may export the same package. Bundles which
	 * re-export the same package that are involved in a require bundle cycle
	 * can lead to lookup cycles when searching for classes and resources from
	 * the package.
	 *
	 * Tests that the re-export statement does not inherit matching attributes
	 * or the "mandatoryness" of matching attributes. If these are not specified
	 * on the re-export statement, they are either unspecified or take default
	 * values where defaults are defined.
	 *
	 * @osgi.spec Bundle.getResource(String)
	 * @throws Exception if any failure occurs or any assert fails
	 */
	@Test
	public void testRequiredBundle010() throws Exception {
		Bundle tb16b = bundleInstaller.installBundle("classloading.tb16b.jar", false);
		Bundle tb16c = bundleInstaller.installBundle("classloading.tb16c.jar", false);
		Bundle tb16a = bundleInstaller.installBundle("classloading.tb16j.jar", false);
		tb16b.start();
		tb16c.start();
		tb16a.start();
		URL url = tb16a.getResource("test/package/resources/resource.txt");
		try {
			assertThat(url)
					.as("Expecting to find resource")
					.isNotNull();

			BufferedReader bufr = new BufferedReader(new InputStreamReader(url
					.openStream()));
			String line = bufr.readLine();
			bufr.close();
			assertThat(line)
					.as("Expecting to get resource from classloading.tb16c")
					.isEqualTo("tb16c");
		}
		finally {
			tb16a.stop();
			tb16b.stop();
			tb16c.stop();
			tb16a.uninstall();
			tb16b.uninstall();
			tb16c.uninstall();
		}
	}

	// Installing Modules -------------------------------------

	/**
	 * Test that any duplicate attribute or duplicate directive causes a R4
	 * bundle to fail to install.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testInstallingModules001() throws Exception {
		// a bundle installed by mistake is removed by osgi-test
		assertThatExceptionOfType(BundleException.class)
				.as("Expecting bundle install to fail because the manifest has a duplicate directive.")
				.isThrownBy(() -> bundleInstaller.installBundle("classloading.tb14b.jar", false));
	}

	/**
	 * Test that mutiple imports of a given package causes a R4 bundle to fail
	 * to install.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testInstallingModules002() throws Exception {
		// a bundle installed by mistake is removed by osgi-test
		assertThatExceptionOfType(BundleException.class)
				.as("Expecting bundle install to fail because a package has been imported multiple times.")
				.isThrownBy(() -> bundleInstaller.installBundle("classloading.tb14c.jar", false));
	}

	/**
	 * Test that any syntactic error (e.g. improperly formatted version or
	 * bundle symbolic name, unrecognized directive, unrecognized directive
	 * value, etc.) causes a R4 bundle to fail to install.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testInstallingModules003() throws Exception {

		try {
			Bundle tb14f = bundleInstaller.installBundle("classloading.tb14f.jar", false);
			tb14f.uninstall();
			fail("Expecting bundle install to fail because of errors in manifest.");
		}
		catch (BundleException e) {
			// expected
		}
		catch (Exception e) {
			fail("Unexpected exception " + e.getClass().getName() + " - "
					+ e.getMessage());
		}

	}

	/**
	 * Test that specification-version and version specified together (for the
	 * same package(s)) causes a R4 bundle to fail to install. For example:
	 * Import-Package p;specification-version=1;version=2 would fail to install,
	 * but: Import-Package p;specification-version=1, q;version=2 would not be
	 * an error.
	 *
	 * @osgi.spec BundleContext.installBundle(String)
	 * @throws Exception if there is any problem or an assert fails
	 */
	@Test
	public void testInstallingModules004() throws Exception {

		try {
			Bundle tb14g = bundleInstaller.installBundle("classloading.tb14g.jar", false);
			tb14g.uninstall();
			fail("Expecting bundle install to fail because of error in Import-Package specification.");
		}
		catch (BundleException e) {
			e.printStackTrace();
		}

	}

}
