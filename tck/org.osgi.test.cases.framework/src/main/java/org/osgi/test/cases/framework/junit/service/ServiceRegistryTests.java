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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.UnfilteredServiceListener;
import org.osgi.test.common.annotation.InjectBundleContext;

public class ServiceRegistryTests {

	@InjectBundleContext
	BundleContext bundleContext;

	private String testName;

	@BeforeEach
	public void captureTestName(TestInfo testInfo) {
		testName = testInfo.getTestMethod().get().getName();
	}

	/** The name of the running test method, as TestCase.getName() gave it. */
	private String getName() {
		return testName;
	}

	@Test
	public void testServiceListener01() {
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				switch (event.getType()) {
					case ServiceEvent.REGISTERED :
						results[0] = true;
						break;
					case ServiceEvent.MODIFIED :
						results[1] = true;
						break;
					case ServiceEvent.MODIFIED_ENDMATCH :
						results[2] = true;
						break;
					case ServiceEvent.UNREGISTERING :
						results[3] = true;
						break;
				}
			}
		};
		try {
			bundleContext
					.addServiceListener(
							testListener,
							"(&(objectClass=java.lang.Runnable)(" + getName()
							+ "=true))");
		} catch (InvalidSyntaxException e) {
			fail("filter error", e);
		}
		ServiceRegistration<Runnable> reg = null;
		try {
			// register service which matches
			Hashtable<String,Object> props = new Hashtable<>();
			props.put(getName(), Boolean.TRUE);
			reg = bundleContext.registerService(Runnable.class, runIt,
					props);
			assertThat(results[0])
					.as("Did not get ServiceEvent.REGISTERED")
				.isTrue();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.FALSE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did not get ServiceEvent.MODIFIED")
				.isTrue();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to no longer match
			props.put(getName(), Boolean.FALSE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did not get ServiceEvent.MODIFIED_ENDMATCH")
				.isTrue();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to no longer match
			props.put("testChangeProp", Boolean.TRUE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props back to match
			props.put(getName(), Boolean.TRUE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did not get ServiceEvent.MODIFIED")
				.isTrue();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// unregister
			reg.unregister();
			reg = null;
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did not get ServiceEvent.UNREGISTERING")
				.isTrue();
			clearResults(results);
		} finally {
			bundleContext.removeServiceListener(testListener);
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testServiceListener02() {
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final boolean[] results = new boolean[] {false, false, false, false};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				switch (event.getType()) {
					case ServiceEvent.REGISTERED :
						results[0] = true;
						break;
					case ServiceEvent.MODIFIED :
						results[1] = true;
						break;
					case ServiceEvent.MODIFIED_ENDMATCH :
						results[2] = true;
						break;
					case ServiceEvent.UNREGISTERING :
						results[3] = true;
						break;
				}
			}
		};
		try {
			bundleContext
					.addServiceListener(
							testListener,
							"(&(objectClass=java.lang.Runnable)(" + getName()
							+ "=true))");
		} catch (InvalidSyntaxException e) {
			fail("filter error", e);
		}
		ServiceRegistration<Runnable> reg = null;
		try {
			// register service which does not match
			Hashtable<String,Object> props = new Hashtable<>();
			props.put(getName(), Boolean.FALSE);
			reg = bundleContext.registerService(Runnable.class, runIt,
					props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to still not match
			props.put("testChangeProp", Boolean.FALSE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to match
			props.put(getName(), Boolean.TRUE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did not get ServiceEvent.MODIFIED")
				.isTrue();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did not get ServiceEvent.MODIFIED")
				.isTrue();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// change props to no longer match
			props.put(getName(), Boolean.FALSE);
			reg.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did not get ServiceEvent.MODIFIED_ENDMATCH")
				.isTrue();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);

			// unregister
			reg.unregister();
			reg = null;
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isFalse();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED")
				.isFalse();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH")
				.isFalse();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING")
				.isFalse();
			clearResults(results);
		} finally {
			bundleContext.removeServiceListener(testListener);
			if (reg != null)
				reg.unregister();
		}
	}

	@Test
	public void testServiceListener03() {
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final int[] results = new int[] {0, 0, 0, 0};
		ServiceListener testListener = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				switch (event.getType()) {
					case ServiceEvent.REGISTERED :
						results[0]++;
						break;
					case ServiceEvent.MODIFIED :
						results[1]++;
						break;
					case ServiceEvent.MODIFIED_ENDMATCH :
						results[2]++;
						break;
					case ServiceEvent.UNREGISTERING :
						results[3]++;
						break;
				}
			}
		};
		try {
			bundleContext
					.addServiceListener(
							testListener,
					"(&(objectclass=java.lang.Runnable)("
							+ getName().toLowerCase() + "=true))");
		}
		catch (InvalidSyntaxException e) {
			fail("filter error", e);
		}
		ServiceRegistration<Runnable> reg1 = null;
		ServiceRegistration<Runnable> reg2 = null;
		try {
			// register service which does not match
			Hashtable<String,Object> props = new Hashtable<>();
			props.put(getName(), Boolean.FALSE);
			reg1 = bundleContext.registerService(
					Runnable.class, runIt, props);
			reg2 = bundleContext.registerService(
					Runnable.class, runIt, props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to still not match
			props.put("testChangeProp", Boolean.FALSE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to match
			props.put(getName(), Boolean.TRUE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to no longer match
			props.put(getName(), Boolean.FALSE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isOne();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isOne();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// unregister
			reg1.unregister();
			reg1 = null;
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.unregister();
			reg2 = null;
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
		}
		finally {
			bundleContext.removeServiceListener(testListener);
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
		}
	}

	@Test
	public void testServiceOrdering01() {
		// test that getServiceReference returns the proper service
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("name", getName());
		props.put(Constants.SERVICE_DESCRIPTION, "min value");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration<Runnable> reg1 = bundleContext
				.registerService(Runnable.class, runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value first");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<Runnable> reg2 = bundleContext
				.registerService(Runnable.class, runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "max value second");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<Runnable> reg3 = bundleContext
				.registerService(Runnable.class, runIt, props);

		try {
			ServiceReference<Runnable> ref = null;
			ref = bundleContext.getServiceReference(Runnable.class);
			assertThat(ref)
					.as("service ref is null")
				.isNotNull();
			assertThat(ref)
					.as("Wrong references")
				.isEqualTo(reg2.getReference());
		} finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	@Test
	public void testDuplicateObjectClass() {
		ServiceRegistration< ? > reg = null;
		try {
			reg = bundleContext.registerService(
					new String[] {Runnable.class.getName(),
							Object.class.getName(), Object.class.getName()},
					new Runnable() {
				public void run() {
					// nothing
				}
			}, null);
		} catch (Throwable t) {
			fail("Failed to register service with duplicate objectClass names", t);
		} finally {
			if (reg != null)
				reg.unregister();
		}
	}

	private void clearResults(boolean[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = false;
	}

	private void clearResults(int[] results) {
		for (int i = 0; i < results.length; i++)
			results[i] = 0;
	}

	@Test
	public void testEventSpan() throws Exception {
	    final boolean[] visited = new boolean[1];
	    ServiceListener sl = new ServiceListener() {
	        public void serviceChanged( ServiceEvent e ) {
	        	synchronized (visited) {
					visited[0] = true;
				}
	        }
	    };

	    bundleContext.addServiceListener(sl);
        synchronized (visited) {
			visited[0] = false;
		}

        String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");

		ServiceRegistration< ? > registration = bundleContext.registerService(
				classes, new ConcreteMarker(30), properties);

	    boolean result;
		synchronized (visited) {
			result = visited[0];
		}
		assertThat(result)
				.as("Visited should now be true after registration ")
				.isTrue();

        synchronized (visited) {
			visited[0] = false;
		}
	    registration.setProperties(properties);
        synchronized (visited) {
			result = visited[0];
		}
        assertThat(result)
        		.as("Visited should now be true after setProperties ")
				.isTrue();

        synchronized (visited) {
			visited[0] = false;
		}
		registration.unregister();
		synchronized (visited) {
			result = visited[0];
		}
		assertThat(result)
				.as("Visited should now be true after unregistration ")
				.isTrue();

	    bundleContext.removeServiceListener(sl);
	}

	@Test
	public void testBasicRegistration() throws Exception {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");
		ServiceRegistration< ? > registration = bundleContext.registerService(
				classes, new ConcreteMarker(1), properties);
		assertThat(registration).isNotNull();
		assertServiceProperties(registration.getReference());
		registration.setProperties(properties);
		registration.unregister();
	}

	@Test
	public void testBasicFactory() {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");
		ServiceRegistration< ? > registration = bundleContext
				.registerService(classes, new ServiceFactory<Object>() {
					int	counter	= 2;
					Hashtable<Bundle,Object>	services	= new Hashtable<>();

					public Object getService(Bundle bundle,
							ServiceRegistration<Object> reg) {
						Object service = new ConcreteMarker(counter++);
						services.put(bundle, service);
						return service;
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<Object> reg, Object service) {
						Object expected = services.remove(bundle);
						assertThat(service)
								.as("wrong service sent to factory")
				.isEqualTo(expected);
					}
				}, properties);
		assertServiceProperties(registration.getReference());

		ServiceReference<Marker1> reference = bundleContext
				.getServiceReference(Marker1.class);
		Marker1 service1 = bundleContext.getService(reference);
		assertThat(service1.getValue())
				.as("wrong service returned")
				.isEqualTo(2);

		Object service12 = bundleContext.getService(reference);
		assertThat(service12)
				.as("wrong service returned")
				.isSameAs(service1);
		Object service13 = bundleContext.getService(reference);
		assertThat(service13)
				.as("wrong service returned")
				.isSameAs(service1);
		Object service14 = bundleContext.getService(reference);
		assertThat(service14)
				.as("wrong service returned")
				.isSameAs(service1);
		Object service15 = bundleContext.getService(reference);
		assertThat(service15)
				.as("wrong service returned")
				.isSameAs(service1);

		assertThat(bundleContext.ungetService(reference)).isTrue();
		assertThat(bundleContext.ungetService(reference)).isTrue();
		assertThat(bundleContext.ungetService(reference)).isTrue();
		assertThat(bundleContext.ungetService(reference)).isTrue();
		assertThat(bundleContext.ungetService(reference)).isTrue();
		assertThat(bundleContext.ungetService(reference)).isFalse();
		assertThat(bundleContext.ungetService(reference)).isFalse();
		assertThat(bundleContext.ungetService(reference)).isFalse();

		ServiceReference<Marker2> reference2 = bundleContext
				.getServiceReference(Marker2.class);
		Marker2 service2 = bundleContext.getService(reference2);
		assertThat(service2.getValue())
				.as("wrong service returned")
				.isEqualTo(3);
		assertThat(bundleContext.ungetService(reference2)).isTrue();

		registration.unregister();
	}

	@Test
	public void testFactoryUngetOnUnregister() {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String, String> properties = new Hashtable<String, String>();
		final AtomicInteger ungetCalled = new AtomicInteger(0);
		properties.put("test", "yes");
		ServiceRegistration< ? > registration = bundleContext
				.registerService(classes, new ServiceFactory<Object>() {

					public Object getService(Bundle bundle,
							ServiceRegistration<Object> reg) {
						return new ConcreteMarker(0);
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<Object> reg, Object service) {
						ungetCalled.incrementAndGet();
					}
				}, properties);
		assertServiceProperties(registration.getReference());

		ServiceReference<Marker1> reference = bundleContext.getServiceReference(
				Marker1.class);
		Marker1 service1 = bundleContext.getServiceObjects(reference).getService();
		assertThat(service1.getValue())
				.as("wrong service returned").isZero();

		registration.unregister();
		assertThat(ungetCalled.get())
				.as("The ServiceFactory.ungetService method was not called.").isOne();
	}

	@Test
	public void testFactoryGetException() {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");

		ServiceRegistration< ? > registration = bundleContext
				.registerService(classes, new ServiceFactory<Object>() {
					public Object getService(Bundle bundle,
							ServiceRegistration<Object> reg) {
						throw new RuntimeException(
								"testFactoryException:getService");
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<Object> reg, Object service) {
						// empty
					}
				}, properties);
		assertServiceProperties(registration.getReference());

		try {
			ServiceReference<Marker1> reference = bundleContext
					.getServiceReference(Marker1.class);
			assertThat(reference).isNotNull();

			Object service = bundleContext.getService(reference);
			assertThat(service).isNull();
		}
		finally {
			registration.unregister();
		}
	}

	@Test
	public void testFactoryUngetException() {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");

		ServiceRegistration< ? > registration = bundleContext
				.registerService(classes, new ServiceFactory<Object>() {
					public Object getService(Bundle bundle,
							ServiceRegistration<Object> reg) {
						return new ConcreteMarker(10);
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<Object> reg, Object service) {
						throw new RuntimeException(
								"testFactoryException:ungetService");
					}
				}, properties);
		assertServiceProperties(registration.getReference());

		try {
			ServiceReference<Marker1> reference = bundleContext
					.getServiceReference(Marker1.class);
			assertThat(reference).isNotNull();

			Marker1 service = bundleContext.getService(reference);
			assertThat(service).isNotNull();
			assertThat(service.getValue()).isEqualTo(10);

			bundleContext.ungetService(reference);
		}
		finally {
			registration.unregister();
		}
	}

	@Test
	public void testFactoryRecursion() {
		String[] classes = new String[] {Marker1.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");

		ServiceRegistration< ? > registration = bundleContext
				.registerService(classes, new ServiceFactory<Object>() {
					int	depth	= 0;

					public Object getService(Bundle bundle,
							ServiceRegistration<Object> reg) {
						depth++;
						if (depth > 1) {
							return new ConcreteMarker(1);
						}
						return bundleContext.getService(reg.getReference());
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<Object> reg, Object service) {
						// empty
					}
				}, properties);
		assertServiceProperties(registration.getReference());

		try {
			ServiceReference<Marker1> reference = bundleContext
					.getServiceReference(Marker1.class);
			assertThat(reference).isNotNull();

			Object service = bundleContext.getService(reference);
			assertThat(service).isNull();
		}
		finally {
			registration.unregister();
		}
	}

	@Test
	public void testWrongClass() {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("! Invalid, registration due to wrong classes")
				.isThrownBy(() -> bundleContext.registerService(classes, "wrong class", properties));
	}

	@Test
	public void testNullService() {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("! Invalid, registration due to null service")
				.isThrownBy(() -> bundleContext.registerService(classes, null, properties));
	}

	@Test
	public void testManyRegistrations() throws Exception {
		String[] classes = new String[] {Marker1.class.getName(),
				Marker2.class.getName()};
		Hashtable<String,Object> properties = new Hashtable<>();
		properties.put("test", "yes");
		ServiceRegistration< ? >[] registrations = new ServiceRegistration[1000];
		for (int i = 0; i < 1000; i++) {
			registrations[i] = bundleContext.registerService(classes,
					new ConcreteMarker(i + 1000), properties);
			assertServiceProperties(registrations[i].getReference());
		}

		ServiceReference< ? > reference[] = bundleContext.getServiceReferences(
				Marker1.class.getName(), null);

		assertThat(reference.length)
				.as("Should have 1000 references ")
				.isEqualTo(1000);

		for (int i = 0; i < 500; i++) {
			registrations[i].unregister();
		}

		reference = bundleContext.getServiceReferences((String) null,
				"(objectClass=" + Marker1.class.getName() + ")");
		assertThat(reference.length)
				.as("Should have 500 references (via filter)")
				.isEqualTo(500);
		reference = bundleContext.getServiceReferences(Marker1.class.getName(),
				null);
		assertThat(reference.length)
				.as("Should have 500 references (via class)")
				.isEqualTo(500);

		for (int i = 500; i < 1000; i++)
			registrations[i].unregister();

		reference = bundleContext.getServiceReferences(Marker1.class.getName(),
				null);
		assertThat(reference)
				.as("Should have 0 references (=null)")
				.isNull();
	}

	@Test
	public void testServiceReferenceCompare01() {
		// test that getServiceReference returns the proper service
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("name", getName());
		props.put(Constants.SERVICE_DESCRIPTION, "min value");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration<Runnable> reg1 = bundleContext
				.registerService(Runnable.class, runIt, props);
		assertServiceProperties(reg1.getReference());
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<Runnable> reg2 = bundleContext
				.registerService(Runnable.class, runIt, props);
		assertServiceProperties(reg2.getReference());

		props.put(Constants.SERVICE_DESCRIPTION, "max value second");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<Runnable> reg3 = bundleContext
				.registerService(Runnable.class, runIt, props);
		assertServiceProperties(reg3.getReference());

		try {
			ServiceReference<Runnable> ref = bundleContext
					.getServiceReference(Runnable.class);
			ServiceReference<Runnable> ref1 = reg1.getReference();
			ServiceReference<Runnable> ref2 = reg2.getReference();
			ServiceReference<Runnable> ref3 = reg3.getReference();

			assertThat(ref)
					.as("service ref is null")
				.isNotNull();
			assertThat(ref)
					.as("Wrong reference")
				.isEqualTo(ref2);

			assertThat(ref2.compareTo(ref))
					.as("Wrong references").isZero();
			assertThat(ref.compareTo(ref2))
					.as("Wrong references").isZero();

			assertThat(ref1
					.compareTo(ref1) == 0)
					.as("Wrong compareTo value: " + ref1.compareTo(ref1))
				.isTrue();
			assertThat(ref1
					.compareTo(ref2) < 0)
					.as("Wrong compareTo value: " + ref1.compareTo(ref2))
				.isTrue();
			assertThat(ref1
					.compareTo(ref3) < 0)
					.as("Wrong compareTo value: " + ref1.compareTo(ref3))
				.isTrue();

			assertThat(ref2
					.compareTo(ref1) > 0)
					.as("Wrong compareTo value: " + ref2.compareTo(ref1))
				.isTrue();
			assertThat(ref2
					.compareTo(ref2) == 0)
					.as("Wrong compareTo value: " + ref2.compareTo(ref2))
				.isTrue();
			assertThat(ref2
					.compareTo(ref3) > 0)
					.as("Wrong compareTo value: " + ref2.compareTo(ref3))
				.isTrue();

			assertThat(ref3
					.compareTo(ref1) > 0)
					.as("Wrong compareTo value: " + ref3.compareTo(ref1))
				.isTrue();
			assertThat(ref3
					.compareTo(ref2) < 0)
					.as("Wrong compareTo value: " + ref3.compareTo(ref2))
				.isTrue();
			assertThat(ref3
					.compareTo(ref3) == 0)
					.as("Wrong compareTo value: " + ref3.compareTo(ref3))
				.isTrue();
		}
		finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	@Test
	public void testModifiedRanking() {
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		Hashtable<String,Object> props = new Hashtable<>();
		props.put(getName(), Boolean.TRUE);
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(15));
		ServiceRegistration<Runnable> reg1 = bundleContext
				.registerService(Runnable.class, runIt, props);
		assertServiceProperties(reg1.getReference());
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(10));
		ServiceRegistration<Runnable> reg2 = bundleContext
				.registerService(Runnable.class, runIt, props);
		assertServiceProperties(reg2.getReference());
		try {
			assertThat(bundleContext.getServiceReference("java.lang.Runnable"))
					.as("wrong service reference")
				.isEqualTo(reg1.getReference());

			props.put(Constants.SERVICE_RANKING, Integer.valueOf(20));
			reg2.setProperties(props);
			assertThat(bundleContext.getServiceReference("java.lang.Runnable"))
					.as("wrong service reference")
				.isEqualTo(reg2.getReference());
		}
		finally {
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
		}
	}

	@Test
	public void testUnfilteredServiceListener01() throws Exception {
		// simple ServiceListener test
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final String filterString = "(&(objectclass=java.lang.Runnable)("
				+ getName().toLowerCase() + "=true))";
		final Filter listenerFilter = bundleContext.createFilter(
				"(objectclass=java.lang.Runnable)");
		final int[] results = new int[] {0, 0, 0, 0};
		ServiceListener testListener = new UnfilteredServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				if (!listenerFilter.match(event.getServiceReference())) {
					return;
				}
				switch (event.getType()) {
					case ServiceEvent.REGISTERED :
						results[0]++;
						break;
					case ServiceEvent.MODIFIED :
						results[1]++;
						break;
					case ServiceEvent.MODIFIED_ENDMATCH :
						results[2]++;
						break;
					case ServiceEvent.UNREGISTERING :
						results[3]++;
						break;
				}
			}
		};
		try {
			bundleContext.addServiceListener(testListener, filterString);
		}
		catch (InvalidSyntaxException e) {
			fail("filter error", e);
		}
		ServiceRegistration<Runnable> reg1 = null;
		ServiceRegistration<Runnable> reg2 = null;
		try {
			// register service which does not match
			Hashtable<String,Object> props = new Hashtable<>();
			props.put(getName(), Boolean.FALSE);
			reg1 = bundleContext.registerService(Runnable.class,
					runIt, props);
			assertServiceProperties(reg1.getReference());
			reg2 = bundleContext.registerService(Runnable.class,
					runIt, props);
			assertServiceProperties(reg2.getReference());
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED")
				.isEqualTo(2);
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to still not match
			props.put("testChangeProp", Boolean.FALSE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to match
			props.put(getName(), Boolean.TRUE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to still match
			props.put("testChangeProp", Boolean.TRUE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// change props to no longer match
			props.put(getName(), Boolean.FALSE);
			reg1.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);
			reg2.setProperties(props);
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isOne();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isZero();
			clearResults(results);

			// unregister
			reg1.unregister();
			reg1 = null;
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isOne();
			clearResults(results);
			reg2.unregister();
			reg2 = null;
			assertThat(results[0])
					.as("Did get ServiceEvent.REGISTERED").isZero();
			assertThat(results[1])
					.as("Did get ServiceEvent.MODIFIED").isZero();
			assertThat(results[2])
					.as("Did get ServiceEvent.MODIFIED_ENDMATCH").isZero();
			assertThat(results[3])
					.as("Did get ServiceEvent.UNREGISTERING").isOne();
			clearResults(results);
		}
		finally {
			bundleContext.removeServiceListener(testListener);
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
		}
	}

	private static List<String>	scopes	= Arrays.asList(Constants.SCOPE_SINGLETON, Constants.SCOPE_BUNDLE, Constants.SCOPE_PROTOTYPE);

	private static void assertServiceProperties(ServiceReference<?> ref) {
		Object id = ref.getProperty(Constants.SERVICE_ID);
		assertThat(id)
				.as("service.id not set")
				.isNotNull();
		assertThat(id instanceof Long)
				.as("service.id not Long")
				.isTrue();
		assertThat(((Long) id).longValue() >= 0)
				.as("service.id is negative")
				.isTrue();

		Object bundleid = ref.getProperty(Constants.SERVICE_BUNDLEID);
		assertThat(bundleid)
				.as("service.bundleid not set")
				.isNotNull();
		assertThat(bundleid instanceof Long)
				.as("service.bundleid not Long")
				.isTrue();
		assertThat(((Long) bundleid).longValue())
				.as("service.bundleid is incorrect")
				.isEqualTo(ref.getBundle().getBundleId());

		Object scope = ref.getProperty(Constants.SERVICE_SCOPE);
		assertThat(scope)
				.as("service.scope not set")
				.isNotNull();
		assertThat(scope instanceof String)
				.as("service.scope not String")
				.isTrue();
		assertThat(scopes.contains(scope))
				.as("service.scope has invalid value: " + scope)
				.isTrue();

		Object objectClass = ref.getProperty(Constants.OBJECTCLASS);
		assertThat(objectClass)
				.as("objectClass not set")
				.isNotNull();
		assertThat(objectClass instanceof String[])
				.as("objectClass not String[]")
				.isTrue();
		assertThat(((String[]) objectClass).length > 0)
				.as("objectClass empty array")
				.isTrue();
		for (String o : (String[]) objectClass) {
			assertThat(o)
					.as("objectClass has null value")
				.isNotNull();
			assertThat(o.length() > 0)
					.as("objectClass has an empty string")
				.isTrue();
		}
	}

	static interface Marker1 {
		public int getValue();
	}

	static interface Marker2 {
		public int getValue();
	}

	static class ConcreteMarker implements Marker1, Marker2 {
		int	value;

		ConcreteMarker(int n) {
			value = n;
		}

		public int getValue() {
			return value;
		}
	}
}
