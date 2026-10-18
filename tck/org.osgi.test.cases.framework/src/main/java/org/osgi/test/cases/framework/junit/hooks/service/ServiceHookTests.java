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

package org.osgi.test.cases.framework.junit.hooks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.opentest4j.AssertionFailedError;
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
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.test.common.annotation.InjectBundleContext;

@SuppressWarnings("deprecation")
public class ServiceHookTests {

	@InjectBundleContext
	BundleContext bundleContext;

	private String testName;

	/** The name of the running test method, as TestCase.getName() gave it. */
	private String getName() {
		return testName;
	}
	
	private ServiceListener	dummysl;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		testName = testInfo.getTestMethod().get().getName();
		dummysl = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				// do nothing
			}
		};
		bundleContext.addServiceListener(dummysl,
				"(reason=force one registered ServiceListener)");
	}

	@AfterEach
	public void tearDown() {
		bundleContext.removeServiceListener(dummysl);
	}

	@Test
	public void testFindHook01() {
		// test the FindHook is called and can remove a reference from the
		// results
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
		// register services
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		props.put(Constants.SERVICE_DESCRIPTION, "service 1");
		final ServiceRegistration<Runnable> reg1 = testContext.registerService(
				Runnable.class, runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "service 2");
		final ServiceRegistration<Runnable> reg2 = testContext.registerService(
				Runnable.class, runIt, props);

		props.put(Constants.SERVICE_DESCRIPTION, "service 3");
		final ServiceRegistration<Runnable> reg3 = testContext.registerService(
				Runnable.class, runIt, props);

		final int[] hookCalled = new int[] {0, 0, 0, 0, 0};
		final AssertionFailedError[] hookError = new AssertionFailedError[] {
				null, null, null, null};

		// register find hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "min value");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration<FindHook> regHook1 = testContext.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 1;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(testContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(1);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg1.getReference())) {
									fail("service 1 is present");
								}
								if (ref.equals(reg2.getReference())) {
									fail("service 2 is present");
								}
							}

							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg1.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg1
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[0] = a;
							return;
						}
					}
				}, props);

		// register find hook 2
		props.put(Constants.SERVICE_DESCRIPTION, "max value first");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<FindHook> regHook2 = testContext.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 2;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(testContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(3);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg2.getReference())) {
									iter.remove();
								}
							}

							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg2.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg2
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[1] = a;
							return;
						}
					}
				}, props);

		// register find hook 3
		props.put(Constants.SERVICE_DESCRIPTION, "max value second");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<FindHook> regHook3 = testContext.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 3;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(testContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(2);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg2.getReference())) {
									fail("service 2 is present");
								}
							}

							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg2.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg2
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[2] = a;
							return;
						}
						// throw an exception from the hook to test that the
						// next hooks are called.
						throw new RuntimeException(getName());
					}
				}, props);

		// register find hook 4
		props.put(Constants.SERVICE_DESCRIPTION, "max value third");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<FindHook> regHook4 = testContext.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 4;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(testContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(2);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg1.getReference())) {
									iter.remove();
								}
								if (ref.equals(reg2.getReference())) {
									fail("service 2 is present");
								}
							}

							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg2.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg2
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[3] = a;
							return;
						}
					}
				}, props);

		// get reference and hook removes some services
		try {
			ServiceReference<?>[] refs = null;
			try {
				refs = testContext.getServiceReferences(Runnable.class
						.getName(), "(name=" + getName() + ")"); //$NON-NLS-2$
			}
			catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e);
			}
			assertThat(hookCalled[0])
					.as("all hooks not called")
				.isEqualTo(4);
			assertThat(hookCalled[1])
					.as("hook 2 not called first")
				.isEqualTo(2);
			assertThat(hookCalled[2])
					.as("hook 3 not called second")
				.isEqualTo(3);
			assertThat(hookCalled[3])
					.as("hook 4 not called third")
				.isEqualTo(4);
			assertThat(hookCalled[4])
					.as("hook 1 not called fourth ").isOne();
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull();
			assertThat(refs.length)
					.as("Wrong number of references").isOne();

			// test removed services are not in the result
			List<?> refList = Arrays.asList(refs);
			assertThat(refList.contains(reg1
					.getReference()))
					.as("contains service 1")
				.isFalse();
			assertThat(refList.contains(reg2
					.getReference()))
					.as("contains service 2")
				.isFalse();
			assertThat(refList.contains(reg3
					.getReference()))
					.as("missing service 3")
				.isTrue();

			// remove the hooks
			regHook1.unregister();
			regHook1 = null;
			regHook2.unregister();
			regHook2 = null;
			regHook3.unregister();
			regHook3 = null;
			regHook4.unregister();
			regHook4 = null;

			// get services and make sure none are filtered
			refs = null;
			hookCalled[0] = 0;

			try {
				refs = testContext.getServiceReferences(Runnable.class
						.getName(), "(name=" + getName() + ")"); //$NON-NLS-2$
			}
			catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e);
			}
			assertThat(hookCalled[0])
					.as("hooks called").isZero();
			assertThat(refs)
					.as("service refs is null")
				.isNotNull();
			assertThat(refs.length)
					.as("Wrong number of references")
				.isEqualTo(3);

			// test result contains all expected services
			refList = Arrays.asList(refs);
			assertThat(refList.contains(reg1
					.getReference()))
					.as("missing service 1")
				.isTrue();
			assertThat(refList.contains(reg2
					.getReference()))
					.as("missing service 2")
				.isTrue();
			assertThat(refList.contains(reg3
					.getReference()))
					.as("missing service 3")
				.isTrue();
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (regHook2 != null)
				regHook2.unregister();
			if (regHook3 != null)
				regHook3.unregister();
			if (regHook4 != null)
				regHook4.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	@Test
	public void testFindHook02() {
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		// register services
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		props.put(Constants.SERVICE_DESCRIPTION, "service 1");
		final ServiceRegistration<Runnable> reg1 = testContext.registerService(
				Runnable.class, runIt, props);

		final AssertionFailedError[] factoryError = new AssertionFailedError[] {null};
		final boolean[] factoryCalled = new boolean[] {false, false};
		final boolean[] hookCalled = new boolean[] {false};
		final FindHook findHook1 = new FindHook() {
			public void find(BundleContext arg0, String arg1, String arg2,
					boolean arg3, Collection<ServiceReference<?>> arg4) {
				synchronized (hookCalled) {
					hookCalled[0] = true;
				}
			}
		};
		props.put(Constants.SERVICE_DESCRIPTION, "find hook 1");
		synchronized (factoryCalled) {
			factoryCalled[0] = false;
			factoryCalled[1] = false;
		}
		synchronized (factoryError) {
			factoryError[0] = null;
		}
		ServiceRegistration<?> regHook1 = testContext.registerService(
				FindHook.class.getName(), new ServiceFactory<FindHook>() {

					public FindHook getService(Bundle bundle,
							ServiceRegistration<FindHook> registration) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[0] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							ServiceReference<FindHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList
									.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
						return findHook1;
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<FindHook> registration,
							FindHook service) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[1] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							assertThat(service)
									.as("wrong service")
				.isEqualTo(findHook1);
							ServiceReference<FindHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList
									.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
					}
				}, props);

		try {
			synchronized (hookCalled) {
				hookCalled[0] = false;
			}
			ServiceReference<?>[] refs = null;
			try {
				refs = testContext.getServiceReferences(Runnable.class
						.getName(), "(name=" + getName() + ")"); //$NON-NLS-2$
			}
			catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e);
			}
			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[0])
						.as("factory getService not called")
				.isTrue();
			}
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("hook not called")
				.isTrue();
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull();
			assertThat(refs.length)
					.as("Wrong number of references")
				.isEqualTo(1);

			List<ServiceReference<?>> refList = Arrays.asList(refs);
			assertThat(refList.contains(reg1
					.getReference()))
					.as("missing service 1")
				.isTrue();

			regHook1.unregister();
			regHook1 = null;

			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[1])
						.as("factory ungetService not called")
				.isTrue();
			}
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (reg1 != null)
				reg1.unregister();
		}
	}

	@Test
	public void testEventHook01() {
		// test the EventHook is called and can remove a reference from the
		// results
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		final int[] hookCalled = new int[] {0, 0};
		final AssertionFailedError[] hookError = new AssertionFailedError[] {null};
		final List<ServiceEvent> events = new ArrayList<ServiceEvent>();

		final ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				synchronized (events) {
					events.add(event);
				}
			}
		};

		final String filterString = "(&(name=" + getName() + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-2$
		Filter tmpFilter = null;
		try {
			tmpFilter = testContext.createFilter(filterString);
			testContext.addServiceListener(sl, filterString);
		}
		catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e);
		}

		final Filter filter = tmpFilter;
		org.osgi.framework.hooks.service.EventHook hook1 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(contexts
							.contains(testContext))
							.as("does not contain test context")
				.isTrue();

					assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
							.isThrownBy(() -> contexts.add(testContext.getBundle(0) .getBundleContext()));
					try {
						contexts.addAll(Arrays
								.asList(new BundleContext[] {testContext
										.getBundle(0).getBundleContext()}));
						fail("addAll to collection succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
				}
				catch (AssertionFailedError a) {
					hookError[0] = a;
					return;
				}
			}
		};
		org.osgi.framework.hooks.service.EventHook hook2 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(contexts
							.contains(testContext))
							.as("does not contain test context")
				.isTrue();
					contexts.remove(testContext);
					assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
							.isThrownBy(() -> contexts.add(testContext.getBundle(0) .getBundleContext()));
					try {
						contexts.addAll(Arrays
								.asList(new BundleContext[] {testContext
										.getBundle(0).getBundleContext()}));
						fail("addAll to collection succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
				}
				catch (AssertionFailedError a) {
					hookError[0] = a;
					return;
				}
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		// register event hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook1, props);

		ServiceRegistration<Runnable> reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1");
			synchronized (events) {
				events.clear();
			}
			reg1 = testContext.registerService(Runnable.class, runIt,
					props);
			assertThat(hookCalled[0])
					.as("all hooks not called").isOne();
			assertThat(hookCalled[1])
					.as("hook 1 not called first").isOne();
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}
			synchronized (events) {
				assertThat(events)
						.as("listener not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.REGISTERED);
					assertThat(event
							.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}

			regHook.unregister();
			regHook = null;

			synchronized (events) {
				events.clear();
			}
			hookCalled[0] = 0;
			props.put(Constants.SERVICE_DESCRIPTION, "service 2");
			reg1.setProperties(props);
			synchronized (events) {
				assertThat(events)
						.as("listener not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.MODIFIED);
					assertThat(event
							.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}
			assertThat(hookCalled[0])
					.as("hooks called").isZero();

			props.put(Constants.SERVICE_DESCRIPTION, "event hook 2");
			regHook = testContext
					.registerService(org.osgi.framework.hooks.service.EventHook.class,
					hook2, props);

			synchronized (events) {
				events.clear();
			}
			hookCalled[0] = 0;
			reg1.unregister();
			reg1 = null;
			synchronized (events) {
				assertThat(events)
						.as("listener called").hasSize(0);
			}
			assertThat(hookCalled[0])
					.as("all hooks not called").isOne();
			assertThat(hookCalled[1])
					.as("hook 1 not called first").isOne();
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}

		}
		finally {
			// unregister hook and services
			if (regHook != null)
				regHook.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (sl != null)
				testContext.removeServiceListener(sl);
		}
	}

	@Test
	public void testEventHook02() {
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		// register services
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		final AssertionFailedError[] factoryError = new AssertionFailedError[] {null};
		final boolean[] factoryCalled = new boolean[] {false, false};
		final boolean[] hookCalled = new boolean[] {false};
		final org.osgi.framework.hooks.service.EventHook eventHook1 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent arg0, Collection<BundleContext> arg1) {
				synchronized (hookCalled) {
					hookCalled[0] = true;
				}
			}
		};
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		synchronized (factoryCalled) {
			factoryCalled[0] = false;
			factoryCalled[1] = false;
		}
		synchronized (factoryError) {
			factoryError[0] = null;
		}
		ServiceRegistration<?> regHook1 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class.getName(), new ServiceFactory<org.osgi.framework.hooks.service.EventHook>() {

					public org.osgi.framework.hooks.service.EventHook getService(Bundle bundle,
							ServiceRegistration<org.osgi.framework.hooks.service.EventHook> registration) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[0] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							ServiceReference<org.osgi.framework.hooks.service.EventHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList
									.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
						return eventHook1;
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<org.osgi.framework.hooks.service.EventHook> registration,
							org.osgi.framework.hooks.service.EventHook service) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[1] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							assertThat(service)
									.as("wrong service")
				.isEqualTo(eventHook1);
							ServiceReference<org.osgi.framework.hooks.service.EventHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList
									.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
					}
				}, props);

		synchronized (hookCalled) {
			hookCalled[0] = false;
		}
		props.put(Constants.SERVICE_DESCRIPTION, "service 1");
		final ServiceRegistration<Runnable> reg1 = testContext.registerService(
				Runnable.class, runIt, props);
		try {
			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[0])
						.as("factory getService not called")
				.isTrue();
			}
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("hook not called")
				.isTrue();
			}

			regHook1.unregister();
			regHook1 = null;

			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[1])
						.as("factory ungetService not called")
				.isTrue();
			}
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (reg1 != null)
				reg1.unregister();
		}
	}

	@Test
	public void testEventListenerHook01() {
		// test the EventListenerHook is called and can remove reference(s) from
		// the results
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		final int[] hookCalled = new int[] {0, 0};
		final AssertionFailedError[] hookError = new AssertionFailedError[] {null};
		final List<ServiceEvent> events1 = new ArrayList<ServiceEvent>();
		final List<ServiceEvent> events2 = new ArrayList<ServiceEvent>();

		final ServiceListener sl1 = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				synchronized (events1) {
					events1.add(event);
				}
			}
		};

		final ServiceListener sl2 = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				synchronized (events2) {
					events2.add(event);
				}
			}
		};

		final String filterString = "(&(name=" + getName() + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-2$
		Filter tmpFilter = null;
		try {
			tmpFilter = testContext.createFilter(filterString);
			testContext.addServiceListener(sl1, filterString);
			testContext.addServiceListener(sl2, filterString);
		}
		catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e);
		}

		final Filter filter = tmpFilter;
		EventListenerHook hook1 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(listeners)
							.as("does not contain test context").containsKey(testContext);
					Collection<ListenerInfo> c = listeners.get(testContext);
					assertThat(c)
							.as("listener collection is null")
				.isNotNull();
					for (Iterator<ListenerInfo> i = c.iterator(); i.hasNext();) {
						ListenerInfo info = i.next();
						assertThat(info.isRemoved())
								.as("isRemoved true")
				.isFalse();
						BundleContext context = info.getBundleContext();
						assertThat(context)
								.as("info context is null")
				.isNotNull();
						String f = info.getFilter();
						if (f.indexOf(getName()) >= 0) {
							assertThat(context)
									.as("wrong info context")
				.isEqualTo(testContext);
							assertThat(testContext.createFilter(f).match(
											event.getServiceReference()))
									.as("info filter does not match event")
				.isTrue();
						}
					}
					try {
						c.add(new ListenerInfo() {
							public BundleContext getBundleContext() {
								throw new AssertionFailedError(
										"invalid ListenerInfo");
							}

							public String getFilter() {
								throw new AssertionFailedError(
										"invalid ListenerInfo");
							}

							public boolean isRemoved() {
								throw new AssertionFailedError(
										"invalid ListenerInfo");
							}
						});
						fail("add to collection succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
					assertThatExceptionOfType(UnsupportedOperationException.class).as("put to map succeeded")
							.isThrownBy(() -> listeners.put(testContext.getBundle(0) .getBundleContext(), new ArrayList<ListenerInfo>()));
					try {
						Map<BundleContext, Collection<ListenerInfo>> toAdd = new HashMap<BundleContext, Collection<ListenerInfo>>();
						toAdd.put(testContext.getBundle(0).getBundleContext(),
								new ArrayList<ListenerInfo>());
						listeners.putAll(toAdd);
						fail("putAll to map succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
				}
				catch (AssertionFailedError a) {
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
				catch (Exception x) {
					AssertionFailedError a = new AssertionFailedError(
							"unexpected exception");
					a.initCause(x);
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
			}
		};
		EventListenerHook hook2 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(listeners)
							.as("does not contain test context").containsKey(testContext);
					Collection<ListenerInfo> c = listeners.get(testContext);
					assertThat(c)
							.as("listener collection is null")
				.isNotNull();
					for (Iterator<ListenerInfo> i = c.iterator(); i.hasNext();) {
						ListenerInfo li = i.next();
						if (li.getFilter().indexOf(getName()) > 0) {
							i.remove();
							break;
						}
					}
					assertThatExceptionOfType(UnsupportedOperationException.class).as("put to map succeeded")
							.isThrownBy(() -> listeners.put(testContext.getBundle(0) .getBundleContext(), new ArrayList<ListenerInfo>()));
					try {
						Map<BundleContext, Collection<ListenerInfo>> toAdd = new HashMap<BundleContext, Collection<ListenerInfo>>();
						toAdd.put(testContext.getBundle(0).getBundleContext(),
								new ArrayList<ListenerHook.ListenerInfo>());
						listeners.putAll(toAdd);
						fail("putAll to map succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
				}
				catch (AssertionFailedError a) {
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
				catch (Exception x) {
					AssertionFailedError a = new AssertionFailedError(
							"unexpected exception");
					a.initCause(x);
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
			}
		};
		EventListenerHook hook3 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(listeners)
							.as("does not contain test context").containsKey(testContext);
					Collection<ListenerInfo> c = listeners.get(testContext);
					assertThat(c)
							.as("listener collection is null")
				.isNotNull();
					c.clear();
					assertThatExceptionOfType(UnsupportedOperationException.class).as("put to map succeeded")
							.isThrownBy(() -> listeners.put(testContext.getBundle(0) .getBundleContext(), new ArrayList<ListenerInfo>()));
					try {
						Map<BundleContext, Collection<ListenerInfo>> toAdd = new HashMap<BundleContext, Collection<ListenerInfo>>();
						toAdd.put(testContext.getBundle(0).getBundleContext(),
								new ArrayList<ListenerHook.ListenerInfo>());
						listeners.putAll(toAdd);
						fail("putAll to map succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
				}
				catch (AssertionFailedError a) {
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
				catch (Exception x) {
					AssertionFailedError a = new AssertionFailedError(
							"unexpected exception");
					a.initCause(x);
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
			}
		};
		EventListenerHook hook4 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(listeners)
							.as("does not contain test context").containsKey(testContext);
					listeners.remove(testContext);
					assertThatExceptionOfType(UnsupportedOperationException.class).as("put to map succeeded")
							.isThrownBy(() -> listeners.put(testContext.getBundle(0) .getBundleContext(), new ArrayList<ListenerHook.ListenerInfo>()));
					try {
						Map<BundleContext, Collection<ListenerInfo>> toAdd = new HashMap<BundleContext, Collection<ListenerInfo>>();
						toAdd.put(testContext.getBundle(0).getBundleContext(),
								new ArrayList<ListenerHook.ListenerInfo>());
						listeners.putAll(toAdd);
						fail("putAll to map succeeded");
					}
					catch (UnsupportedOperationException e) {
						// should get an exception
					}
					catch (Exception e) {
						fail("incorrect exception", e);
					}
				}
				catch (AssertionFailedError a) {
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
				catch (Exception x) {
					AssertionFailedError a = new AssertionFailedError(
							"unexpected exception");
					a.initCause(x);
					synchronized (hookError) {
						hookError[0] = a;
					}
					return;
				}
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		// register event hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		ServiceRegistration<EventListenerHook> regHook = testContext
				.registerService(EventListenerHook.class, hook1, props);

		ServiceRegistration<Runnable> reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1");
			synchronized (events1) {
				events1.clear();
			}
			synchronized (events2) {
				events2.clear();
			}
			reg1 = testContext.registerService(Runnable.class, runIt,
					props);
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("all hooks not called").isOne();
				assertThat(hookCalled[1])
						.as("hook 1 not called first").isOne();
			}
			synchronized (hookError) {
				for (int i = 0; i < hookError.length; i++) {
					if (hookError[i] != null) {
						throw hookError[i];
					}
				}
			}
			synchronized (events1) {
				assertThat(events1)
						.as("listener1 not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events1.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.REGISTERED);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}
			synchronized (events2) {
				assertThat(events2)
						.as("listener2 not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events2.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.REGISTERED);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}

			regHook.unregister();
			props.put(Constants.SERVICE_DESCRIPTION, "event hook 2");
			regHook = testContext.registerService(
EventListenerHook.class,
					hook2, props);

			synchronized (events1) {
				events1.clear();
			}
			synchronized (events2) {
				events2.clear();
			}
			synchronized (hookCalled) {
				hookCalled[0] = 0;
			}

			props.put(Constants.SERVICE_DESCRIPTION, "service 2");
			reg1.setProperties(props);

			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("all hooks not called").isOne();
				assertThat(hookCalled[1])
						.as("hook 1 not called first").isOne();
			}
			synchronized (hookError) {
				for (int i = 0; i < hookError.length; i++) {
					if (hookError[i] != null) {
						throw hookError[i];
					}
				}
			}
			synchronized (events1) {
				synchronized (events2) {
					List<ServiceEvent> events = new ArrayList<ServiceEvent>(
							events1);
					events.addAll(events2);
					assertThat(events)
							.as("more than one listener called").hasSize(1);
					for (Iterator<ServiceEvent> iter = events.iterator(); iter
							.hasNext();) {
						ServiceEvent event = iter.next();
						assertThat(event.getType())
								.as("type not registered")
				.isEqualTo(ServiceEvent.MODIFIED);
						assertThat(event.getServiceReference())
								.as("wrong service")
				.isEqualTo(reg1.getReference());
					}
				}
			}

			regHook.unregister();
			props.put(Constants.SERVICE_DESCRIPTION, "event hook 3");
			regHook = testContext.registerService(
EventListenerHook.class,
					hook3, props);

			synchronized (events1) {
				events1.clear();
			}
			synchronized (events2) {
				events2.clear();
			}
			synchronized (hookCalled) {
				hookCalled[0] = 0;
			}

			props.put(Constants.SERVICE_DESCRIPTION, "service 3");
			reg1.setProperties(props);

			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("all hooks not called").isOne();
				assertThat(hookCalled[1])
						.as("hook 1 not called first").isOne();
			}
			synchronized (hookError) {
				for (int i = 0; i < hookError.length; i++) {
					if (hookError[i] != null) {
						throw hookError[i];
					}
				}
			}
			synchronized (events1) {
				assertThat(events1)
						.as("listener1 was called").hasSize(0);
			}
			synchronized (events2) {
				assertThat(events2)
						.as("listener2 was called").hasSize(0);
			}

			regHook.unregister();
			regHook = null;

			synchronized (events1) {
				events1.clear();
			}
			synchronized (events2) {
				events2.clear();
			}
			synchronized (hookCalled) {
				hookCalled[0] = 0;
			}
			props.put(Constants.SERVICE_DESCRIPTION, "service 4");
			reg1.setProperties(props);
			synchronized (events1) {
				assertThat(events1)
						.as("listener1 not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events1.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.MODIFIED);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}
			synchronized (events2) {
				assertThat(events2)
						.as("listener2 not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events2.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.MODIFIED);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("hooks called").isZero();
			}

			props.put(Constants.SERVICE_DESCRIPTION, "event hook 4");
			regHook = testContext.registerService(
EventListenerHook.class,
					hook4, props);

			synchronized (events1) {
				events1.clear();
			}
			synchronized (events2) {
				events2.clear();
			}
			synchronized (hookCalled) {
				hookCalled[0] = 0;
			}
			reg1.unregister();
			reg1 = null;
			synchronized (events1) {
				assertThat(events1)
						.as("listener1 called").hasSize(0);
			}
			synchronized (events2) {
				assertThat(events2)
						.as("listener2 called").hasSize(0);
			}
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("all hooks not called").isOne();
				assertThat(hookCalled[1])
						.as("hook 1 not called first").isOne();
			}
			synchronized (hookError) {
				for (int i = 0; i < hookError.length; i++) {
					if (hookError[i] != null) {
						throw hookError[i];
					}
				}
			}

		}
		finally {
			// unregister hook and services
			if (regHook != null)
				regHook.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (sl1 != null)
				testContext.removeServiceListener(sl1);
			if (sl2 != null)
				testContext.removeServiceListener(sl2);
		}
	}

	@Test
	public void testEventListenerHook02() {
		// test for EventListenerHook implemented as a ServiceFactory
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		// register services
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		final AssertionFailedError[] factoryError = new AssertionFailedError[] {null};
		final boolean[] factoryCalled = new boolean[] {false, false};
		final boolean[] hookCalled = new boolean[] {false};
		final EventListenerHook eventHook1 = new EventListenerHook() {
			public void event(ServiceEvent arg0,
					Map<BundleContext, Collection<ListenerInfo>> arg1) {
				synchronized (hookCalled) {
					hookCalled[0] = true;
				}
			}
		};
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		synchronized (factoryCalled) {
			factoryCalled[0] = false;
			factoryCalled[1] = false;
		}
		synchronized (factoryError) {
			factoryError[0] = null;
		}
		ServiceRegistration<?> regHook1 = testContext.registerService(
				EventListenerHook.class.getName(),
				new ServiceFactory<EventListenerHook>() {

					public EventListenerHook getService(Bundle bundle,
							ServiceRegistration<EventListenerHook> registration) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[0] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							ServiceReference<EventListenerHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
						return eventHook1;
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<EventListenerHook> registration,
							EventListenerHook service) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[1] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							assertThat(service)
									.as("wrong service")
				.isEqualTo(eventHook1);
							ServiceReference<EventListenerHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
					}
				}, props);

		synchronized (hookCalled) {
			hookCalled[0] = false;
		}
		props.put(Constants.SERVICE_DESCRIPTION, "service 1");
		final ServiceRegistration<Runnable> reg1 = testContext.registerService(
				Runnable.class, runIt, props);
		try {
			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[0])
						.as("factory getService not called")
				.isTrue();
			}
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("hook not called")
				.isTrue();
			}

			regHook1.unregister();
			regHook1 = null;

			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[1])
						.as("factory ungetService not called")
				.isTrue();
			}
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (reg1 != null)
				reg1.unregister();
		}
	}

	@Test
	public void testEventListenerHook03() {
		// test the EventHook is called before the EventListenerHook
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		final int[] hookCalled = new int[] {0, 0, 0, 0, 0};

		final ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
			}
		};

		final String filterString = "(&(name=" + getName() + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-2$
		Filter tmpFilter = null;
		try {
			tmpFilter = testContext.createFilter(filterString);
			testContext.addServiceListener(sl, filterString);
		}
		catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e);
		}

		final Filter filter = tmpFilter;
		org.osgi.framework.hooks.service.EventHook hook1 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 1;
				}
			}
		};
		org.osgi.framework.hooks.service.EventHook hook2 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 2;
				}
			}
		};
		EventListenerHook hook3 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 3;
				}
			}
		};

		EventListenerHook hook4 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 4;
				}
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		// register event hook 4
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 4");
		ServiceRegistration<EventListenerHook> regHook4 = testContext
				.registerService(EventListenerHook.class, hook4, props);

		// register event hook 2
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 2");
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook2 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook2, props);

		// register event hook 3
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 3");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(20));
		ServiceRegistration<EventListenerHook> regHook3 = testContext
				.registerService(EventListenerHook.class, hook3, props);

		// register event hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(10));
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook1 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook1, props);

		ServiceRegistration<Runnable> reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1");
			props.remove(Constants.SERVICE_RANKING);
			reg1 = testContext.registerService(Runnable.class, runIt,
					props);
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("all hooks not called")
				.isEqualTo(4);
				assertThat(hookCalled[1])
						.as("hook 1 not called first").isOne();
				assertThat(hookCalled[2])
						.as("hook 2 not called second")
				.isEqualTo(2);
				assertThat(hookCalled[3])
						.as("hook 3 not called third")
				.isEqualTo(3);
				assertThat(hookCalled[4])
						.as("hook 4 not called fourth")
				.isEqualTo(4);
			}
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (regHook2 != null)
				regHook2.unregister();
			if (regHook3 != null)
				regHook3.unregister();
			if (regHook4 != null)
				regHook4.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (sl != null)
				testContext.removeServiceListener(sl);
		}
	}

	@Test
	public void testListenerHook01() {
		// test the ListenerHook is called
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
		final Collection<ListenerInfo> added = new ArrayList<ListenerInfo>();
		final Collection<ListenerInfo> removed = new ArrayList<ListenerInfo>();
		final int[] hookCalled = new int[] {0, 0};

		ListenerHook hook1 = new ListenerHook() {
			public void added(Collection<ListenerInfo> listeners) {
				hookCalled[0]++;
				added.addAll(listeners);
			}

			public void removed(Collection<ListenerInfo> listeners) {
				hookCalled[1]++;
				added.removeAll(listeners);
				removed.addAll(listeners);
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		// register listener hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "listener hook 1");
		ServiceRegistration<ListenerHook> regHook = testContext
				.registerService(ListenerHook.class, hook1, props);

		try {
			assertThat(added)
					.as("no service listeners found").isNotEmpty();
			assertThat(hookCalled[0])
					.as("added not called").isOne();
			assertThat(hookCalled[1])
					.as("removed called").isZero();

			int size = added.size();
			ServiceListener testSL = new ServiceListener() {
				public void serviceChanged(ServiceEvent event) {
					// do nothing
				}
			};
			String filterString1 = "(foo=bar)";
			testContext.addServiceListener(testSL, filterString1);
			assertThat(hookCalled[0])
					.as("added not called")
				.isEqualTo(2);
			assertThat(hookCalled[1])
					.as("removed called").isZero();
			assertThat(added)
					.as("listener not added").hasSize(size + 1);
			boolean found = false;
			for (Iterator<ListenerInfo> iter = added.iterator(); iter.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isFalse();
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((testContext.equals(c)) && (filterString1.equals(f))) {
					if (found) {
						fail("found more than once");
					}
					found = true;
				}
			}
			if (!found) {
				fail("listener not found");
			}
			for (Iterator<ListenerInfo> iter = removed.iterator(); iter
					.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isTrue();
			}

			String filterString2 = "(bar=foo)";
			testContext.addServiceListener(testSL, filterString2);
			assertThat(hookCalled[0])
					.as("added not called")
				.isEqualTo(3);
			assertThat(hookCalled[1])
					.as("removed not called").isOne();
			assertThat(added)
					.as("listener not removed and added").hasSize(size + 1);
			found = false;
			for (Iterator<ListenerInfo> iter = added.iterator(); iter.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isFalse();
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((testContext.equals(c)) && (filterString2.equals(f))) {
					if (found) {
						fail("found more than once");
					}
					found = true;
				}
				if ((testContext.equals(c)) && (filterString1.equals(f))) {
					fail("first listener not removed");
				}
			}
			if (!found) {
				fail("listener not found");
			}
			for (Iterator<ListenerInfo> iter = removed.iterator(); iter
					.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isTrue();
			}

			testContext.removeServiceListener(testSL);
			assertThat(hookCalled[0])
					.as("added called")
				.isEqualTo(3);
			assertThat(hookCalled[1])
					.as("removed not called")
				.isEqualTo(2);
			assertThat(added)
					.as("listener not removed").hasSize(size);
			for (Iterator<ListenerInfo> iter = added.iterator(); iter.hasNext();) {
				ListenerHook.ListenerInfo info = iter
						.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isFalse();
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((testContext.equals(c)) && (filterString2.equals(f))) {
					fail("second listener not removed");
				}
			}
			for (Iterator<ListenerInfo> iter = removed.iterator(); iter
					.hasNext();) {
				ListenerHook.ListenerInfo info = iter
						.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isTrue();
			}

			testContext.removeServiceListener(testSL);
			assertThat(hookCalled[0])
					.as("added called")
				.isEqualTo(3);
			assertThat(hookCalled[1])
					.as("removed called")
				.isEqualTo(2);
			assertThat(added)
					.as("listener removed").hasSize(size);

		}
		catch (InvalidSyntaxException e) {
			fail(e.getMessage());
		}
		finally {
			if (regHook != null) {
				regHook.unregister();
			}
		}
	}

	@Test
	public void testListenerHook02() {
		// test the ListenerHook works with the FilteredServiceListener
		// optimization in equinox
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
		final Collection<ListenerInfo> added = new ArrayList<ListenerHook.ListenerInfo>();
		final Collection<ListenerInfo> removed = new ArrayList<ListenerHook.ListenerInfo>();
		final int[] hookCalled = new int[] {0, 0};

		ListenerHook hook1 = new ListenerHook() {
			public void added(Collection<ListenerInfo> listeners) {
				hookCalled[0]++;
				added.addAll(listeners);
			}

			public void removed(Collection<ListenerInfo> listeners) {
				hookCalled[1]++;
				added.removeAll(listeners);
				removed.addAll(listeners);
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		// register listener hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "listener hook 1");
		ServiceRegistration<ListenerHook> regHook = testContext
				.registerService(ListenerHook.class, hook1, props);

		try {
			assertThat(added)
					.as("no service listeners found").isNotEmpty();
			assertThat(hookCalled[0])
					.as("added not called").isOne();
			assertThat(hookCalled[1])
					.as("removed called").isZero();

			int size = added.size();
			ServiceListener testSL = new ServiceListener() {
				public void serviceChanged(ServiceEvent event) {
					// do nothing
				}
			};
			String filterString1 = "(" + Constants.OBJECTCLASS + "=bar)"; //$NON-NLS-2$
			testContext.addServiceListener(testSL, filterString1);
			assertThat(hookCalled[0])
					.as("added not called")
				.isEqualTo(2);
			assertThat(hookCalled[1])
					.as("removed called").isZero();
			assertThat(added)
					.as("listener not added").hasSize(size + 1);
			boolean found = false;
			for (Iterator<ListenerInfo> iter = added.iterator(); iter.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isFalse();
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((testContext.equals(c)) && (filterString1.equals(f))) {
					if (found) {
						fail("found more than once");
					}
					found = true;
				}
			}
			if (!found) {
				fail("listener not found");
			}
			for (Iterator<ListenerInfo> iter = removed.iterator(); iter
					.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isTrue();
			}

			String filterString2 = null;
			testContext.addServiceListener(testSL);
			assertThat(hookCalled[0])
					.as("added not called")
				.isEqualTo(3);
			assertThat(hookCalled[1])
					.as("removed not called").isOne();
			assertThat(added)
					.as("listener not removed and added").hasSize(size + 1);
			found = false;
			for (Iterator<ListenerInfo> iter = added.iterator(); iter.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isFalse();
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((testContext.equals(c)) && (f == filterString2)) {
					if (found) {
						fail("found more than once");
					}
					found = true;
				}
				if ((testContext.equals(c)) && (filterString1.equals(f))) {
					fail("first listener not removed");
				}
			}
			if (!found) {
				fail("listener not found");
			}
			for (Iterator<ListenerInfo> iter = removed.iterator(); iter
					.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isTrue();
			}

			testContext.removeServiceListener(testSL);
			assertThat(hookCalled[0])
					.as("added called")
				.isEqualTo(3);
			assertThat(hookCalled[1])
					.as("removed not called")
				.isEqualTo(2);
			assertThat(added)
					.as("listener not removed").hasSize(size);
			for (Iterator<ListenerInfo> iter = added.iterator(); iter.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isFalse();
				BundleContext c = info.getBundleContext();
				String f = info.getFilter();
				if ((testContext.equals(c)) && (f == filterString2)) {
					fail("second listener not removed");
				}
			}
			for (Iterator<ListenerInfo> iter = removed.iterator(); iter
					.hasNext();) {
				ListenerHook.ListenerInfo info = iter.next();
				assertThat(info.isRemoved())
						.as("isRemoved true")
				.isTrue();
			}

			testContext.removeServiceListener(testSL);
			assertThat(hookCalled[0])
					.as("added called")
				.isEqualTo(3);
			assertThat(hookCalled[1])
					.as("removed called")
				.isEqualTo(2);
			assertThat(added)
					.as("listener removed").hasSize(size);

		}
		catch (InvalidSyntaxException e) {
			fail(e.getMessage());
		}
		finally {
			if (regHook != null) {
				regHook.unregister();
			}
		}
	}

	@Test
	public void testListenerHook03() {
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy

		// register services
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		final AssertionFailedError[] factoryError = new AssertionFailedError[] {null};
		final boolean[] factoryCalled = new boolean[] {false, false};
		final boolean[] hookCalled = new boolean[] {false};
		final ListenerHook listenerHook1 = new ListenerHook() {
			public void added(Collection<ListenerInfo> arg0) {
				synchronized (hookCalled) {
					hookCalled[0] = true;
				}
			}

			public void removed(Collection<ListenerInfo> arg0) {
				synchronized (hookCalled) {
					hookCalled[0] = true;
				}
			}
		};
		props.put(Constants.SERVICE_DESCRIPTION, "listener hook 1");
		synchronized (factoryCalled) {
			factoryCalled[0] = false;
			factoryCalled[1] = false;
		}
		synchronized (factoryError) {
			factoryError[0] = null;
		}
		ServiceRegistration<?> regHook1 = testContext.registerService(
				ListenerHook.class.getName(),
				new ServiceFactory<ListenerHook>() {

					public ListenerHook getService(Bundle bundle,
							ServiceRegistration<ListenerHook> registration) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[0] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							ServiceReference<ListenerHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList
									.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
						return listenerHook1;
					}

					public void ungetService(Bundle bundle,
							ServiceRegistration<ListenerHook> registration,
							ListenerHook service) {
						try {
							synchronized (factoryCalled) {
								factoryCalled[1] = true;
							}
							assertThat(bundle)
									.as("using bundle null")
				.isNotNull();
							assertThat(service)
									.as("wrong service")
				.isEqualTo(listenerHook1);
							ServiceReference<ListenerHook> reference = registration
									.getReference();
							Bundle[] users = reference.getUsingBundles();
							assertThat(users)
									.as("service not used by a bundle")
				.isNotNull();
							List<Bundle> userList = Arrays.asList(users);
							assertThat(userList
									.contains(bundle))
									.as("missing using bundle")
				.isTrue();
						}
						catch (AssertionFailedError a) {
							synchronized (factoryError) {
								factoryError[0] = a;
							}
						}
					}
				}, props);

		synchronized (hookCalled) {
			hookCalled[0] = false;
		}

		final ServiceListener testSL = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				// do nothing
			}
		};
		testContext.addServiceListener(testSL);
		try {
			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[0])
						.as("factory getService not called")
				.isTrue();
			}
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("hook not called")
				.isTrue();
			}

			regHook1.unregister();
			regHook1 = null;

			synchronized (factoryError) {
				if (factoryError[0] != null) {
					throw factoryError[0];
				}
			}
			synchronized (factoryCalled) {
				assertThat(factoryCalled[1])
						.as("factory ungetService not called")
				.isTrue();
			}
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			testContext.removeServiceListener(testSL);
		}
	}

	@Test
	public void testSystemFindHook() {
		// test the FindHook is called and can remove a reference from the
		// results for system context
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext1 = bundleContext;
		final BundleContext systemContext = testContext1.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();

		// register services
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		props.put(Constants.SERVICE_DESCRIPTION, "service 1");
		final ServiceRegistration<Runnable> reg1 = testContext1.registerService(
				Runnable.class, runIt, props);
	
		props.put(Constants.SERVICE_DESCRIPTION, "service 2");
		final ServiceRegistration<Runnable> reg2 = testContext1.registerService(
				Runnable.class, runIt, props);
	
		props.put(Constants.SERVICE_DESCRIPTION, "service 3");
		final ServiceRegistration<Runnable> reg3 = testContext1.registerService(
				Runnable.class, runIt, props);
	
		final int[] hookCalled = new int[] {0, 0, 0, 0, 0};
		final AssertionFailedError[] hookError = new AssertionFailedError[] {
				null, null, null, null};
	
		// register find hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "min value");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		ServiceRegistration<FindHook> regHook1 = testContext1.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 1;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(systemContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(1);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg1.getReference())) {
									fail("service 1 is present");
								}
								if (ref.equals(reg2.getReference())) {
									fail("service 2 is present");
								}
							}
	
							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg1.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg1
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[0] = a;
							return;
						}
					}
				}, props);
	
		// register find hook 2
		props.put(Constants.SERVICE_DESCRIPTION, "max value first");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<FindHook> regHook2 = testContext1.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 2;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(systemContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(3);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg2.getReference())) {
									iter.remove();
								}
							}
	
							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg2.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg2
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[1] = a;
							return;
						}
					}
				}, props);
	
		// register find hook 3
		props.put(Constants.SERVICE_DESCRIPTION, "max value second");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<FindHook> regHook3 = testContext1.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 3;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(systemContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(2);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg2.getReference())) {
									fail("service 2 is present");
								}
							}
	
							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg2.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg2
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[2] = a;
							return;
						}
						// throw an exception from the hook to test that the
						// next hooks are called.
						throw new RuntimeException(getName());
					}
				}, props);
	
		// register find hook 4
		props.put(Constants.SERVICE_DESCRIPTION, "max value third");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		ServiceRegistration<FindHook> regHook4 = testContext1.registerService(
				FindHook.class, new FindHook() {
					public void find(BundleContext context, String name,
							String filter, boolean allServices,
							Collection<ServiceReference<?>> references) {
						try {
							synchronized (hookCalled) {
								hookCalled[++hookCalled[0]] = 4;
							}
							assertThat(context)
									.as("wrong context in hook")
				.isEqualTo(systemContext);
							assertThat(name)
									.as("wrong name in hook")
									.isEqualTo(Runnable.class
									.getName());
							assertThat(filter)
									.as("wrong filter in hook")
				.isEqualTo("(name=" + getName() + ")"); //$NON-NLS-2$ //$NON-NLS-3$
							assertThat(allServices)
									.as("wrong allservices in hook").isFalse();
							assertThat(references)
									.as("wrong number of services in hook").hasSize(2);
							for (Iterator<ServiceReference<?>> iter = references
									.iterator(); iter
									.hasNext();) {
								ServiceReference<?> ref = iter.next();
								if (ref.equals(reg1.getReference())) {
									iter.remove();
								}
								if (ref.equals(reg2.getReference())) {
									fail("service 2 is present");
								}
							}
	
							assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
									.isThrownBy(() -> references.add(reg2.getReference()));
							try {
								references.addAll(Arrays
										.asList(new ServiceReference<?>[] { reg2
												.getReference()}));
								fail("addAll to collection succeeded");
							}
							catch (UnsupportedOperationException e) {
								// should get an exception
							}
							catch (Exception e) {
								fail("incorrect exception", e);
							}
						}
						catch (AssertionFailedError a) {
							hookError[3] = a;
							return;
						}
					}
				}, props);
	
		// get reference and hook removes some services
		try {
			ServiceReference<?>[] refs = null;
			try {
				refs = systemContext.getServiceReferences(Runnable.class
						.getName(), "(name=" + getName() + ")"); //$NON-NLS-2$
			}
			catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e);
			}
			assertThat(hookCalled[0])
					.as("all hooks not called")
				.isEqualTo(4);
			assertThat(hookCalled[1])
					.as("hook 2 not called first")
				.isEqualTo(2);
			assertThat(hookCalled[2])
					.as("hook 3 not called second")
				.isEqualTo(3);
			assertThat(hookCalled[3])
					.as("hook 4 not called third")
				.isEqualTo(4);
			assertThat(hookCalled[4])
					.as("hook 1 not called fourth ").isOne();
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull();
			assertThat(refs.length)
					.as("Wrong number of references")
				.isEqualTo(3);
	
			// test removed services ARE in the result; since removals are ignored
			List<?> refList = Arrays.asList(refs);
			assertThat(refList.contains(reg1
					.getReference()))
					.as("missing service 1")
				.isTrue();
			assertThat(refList.contains(reg2
					.getReference()))
					.as("missing service 2")
				.isTrue();
			assertThat(refList.contains(reg3
					.getReference()))
					.as("missing service 3")
				.isTrue();
		}
		finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (regHook2 != null)
				regHook2.unregister();
			if (regHook3 != null)
				regHook3.unregister();
			if (regHook4 != null)
				regHook4.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (reg2 != null)
				reg2.unregister();
			if (reg3 != null)
				reg3.unregister();
		}
	}

	@Test
	public void testSystemEventHook() {
		// test the EventHook is called and can remove a reference from the
		// results
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};

		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
		final BundleContext systemContext = testContext.getBundle(
				Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();

		final int[] hookCalled = new int[] { 0, 0, 0 };
		final AssertionFailedError[] hookError = new AssertionFailedError[] {
				null, null };
		final List<ServiceEvent> events = new ArrayList<ServiceEvent>();

		final ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				synchronized (events) {
					events.add(event);
				}
			}
		};

		final String filterString = "(&(name=" + getName() + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-2$
		Filter tmpFilter = null;
		try {
			tmpFilter = systemContext.createFilter(filterString);
			systemContext.addServiceListener(sl, filterString);
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e);
		}

		final Filter filter = tmpFilter;
		org.osgi.framework.hooks.service.EventHook hook1 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
					}
					assertThat(contexts.contains(systemContext))
							.as("does not contain system context")
				.isTrue();
					contexts.remove(systemContext);
					assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
							.isThrownBy(() -> contexts.add(testContext));
					try {
						contexts.addAll(Arrays
								.asList(new BundleContext[] { testContext }));
						fail("addAll to collection succeeded");
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e);
					}
				} catch (AssertionFailedError a) {
					hookError[0] = a;
					return;
				}
			}
		};

		org.osgi.framework.hooks.service.EventHook hook2 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				try {
					if (!filter.match(event.getServiceReference())) {
						return;
					}
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 2;
					}
					assertThat(contexts.contains(systemContext))
							.as("does contain system context")
				.isFalse();

					assertThatExceptionOfType(UnsupportedOperationException.class).as("add to collection succeeded")
							.isThrownBy(() -> contexts.add(testContext));
					try {
						contexts.addAll(Arrays
								.asList(new BundleContext[] { testContext }));
						fail("addAll to collection succeeded");
					} catch (UnsupportedOperationException e) {
						// should get an exception
					} catch (Exception e) {
						fail("incorrect exception", e);
					}
				} catch (AssertionFailedError a) {
					hookError[0] = a;
					return;
				}
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());

		// register event hooks
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook1 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook1, props);
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 2");
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook2 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook2, props);

		ServiceRegistration<Runnable> reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1");
			synchronized (events) {
				events.clear();
			}
			reg1 = testContext.registerService(Runnable.class, runIt, props);
			assertThat(hookCalled[0])
					.as("all hooks not called")
				.isEqualTo(2);
			assertThat(hookCalled[1])
					.as("hook 1 not called first").isOne();
			assertThat(hookCalled[2])
					.as("hook 2 not called second")
				.isEqualTo(2);
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}
			synchronized (events) {
				assertThat(events)
						.as("listener not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.REGISTERED);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}

			synchronized (events) {
				events.clear();
			}
			hookCalled[0] = 0;
			hookCalled[1] = 0;
			hookCalled[2] = 0;
			props.put(Constants.SERVICE_DESCRIPTION, "service 2");
			reg1.setProperties(props);
			synchronized (events) {
				assertThat(events)
						.as("listener not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.MODIFIED);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1.getReference());
				}
			}
			assertThat(hookCalled[0])
					.as("all hooks not called")
				.isEqualTo(2);
			assertThat(hookCalled[1])
					.as("hook 1 not called first").isOne();
			assertThat(hookCalled[2])
					.as("hook 2 not called second")
				.isEqualTo(2);
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}

			synchronized (events) {
				events.clear();
			}
			hookCalled[0] = 0;
			hookCalled[1] = 0;
			hookCalled[2] = 0;

			final ServiceReference<Runnable> reg1Ref = reg1.getReference();
			reg1.unregister();
			reg1 = null;
			synchronized (events) {
				assertThat(events)
						.as("listener not called once").hasSize(1);
				for (Iterator<ServiceEvent> iter = events.iterator(); iter
						.hasNext();) {
					ServiceEvent event = iter.next();
					assertThat(event.getType())
							.as("type not registered")
				.isEqualTo(ServiceEvent.UNREGISTERING);
					assertThat(event.getServiceReference())
							.as("wrong service")
				.isEqualTo(reg1Ref);
				}
			}

			assertThat(hookCalled[0])
					.as("all hooks not called")
				.isEqualTo(2);
			assertThat(hookCalled[1])
					.as("hook 1 not called first").isOne();
			assertThat(hookCalled[2])
					.as("hook 2 not called second")
				.isEqualTo(2);
			for (int i = 0; i < hookError.length; i++) {
				if (hookError[i] != null) {
					throw hookError[i];
				}
			}

		} finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (regHook2 != null)
				regHook2.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (sl != null)
				systemContext.removeServiceListener(sl);
		}
	}

	@Test
	public void testSystemEventListenerHook() {
		// test the EventHook is called before the EventListenerHook for system
		// context
		Runnable runIt = new Runnable() {
			public void run() {
				// nothing
			}
		};
		final BundleContext testContext = bundleContext.getBundle().getBundleContext(); // the injected context is a proxy
		final BundleContext systemContext = testContext.getBundle(
				Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();

		final int[] hookCalled = new int[] { 0, 0, 0, 0, 0 };
		final AssertionFailedError[] hookError = new AssertionFailedError[] {
				null, null, null, null };
		final boolean[] eventHookRemove = new boolean[] { false };

		final List<ServiceEvent> events = new ArrayList<ServiceEvent>();

		final ServiceListener sl = new ServiceListener() {
			public void serviceChanged(ServiceEvent event) {
				synchronized (events) {
					events.add(event);
				}
			}
		};

		final String filterString = "(&(name=" + getName() + ")(objectClass=java.lang.Runnable))"; //$NON-NLS-2$
		Filter tmpFilter = null;
		try {
			tmpFilter = systemContext.createFilter(filterString);
			systemContext.addServiceListener(sl, filterString);
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e);
		}

		final Filter filter = tmpFilter;
		org.osgi.framework.hooks.service.EventHook hook1 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				try {
					synchronized (hookCalled) {
						hookCalled[++hookCalled[0]] = 1;
						assertThat(contexts.contains(systemContext))
								.as("does not contain system context")
								.isTrue();
						if (eventHookRemove[0]) {
							contexts.remove(systemContext);
						}
					}
				} catch (AssertionFailedError a) {
					hookError[0] = a;
					return;
				}
			}
		};
		org.osgi.framework.hooks.service.EventHook hook2 = new org.osgi.framework.hooks.service.EventHook() {
			public void event(ServiceEvent event,
					Collection<BundleContext> contexts) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 2;
					if (eventHookRemove[0]) {
						assertThat(contexts.contains(systemContext))
								.as("does contain system context")
				.isFalse();
					} else {
						assertThat(contexts.contains(systemContext))
								.as("does not contain system context")
				.isTrue();
					}
				}
			}
		};
		EventListenerHook hook3 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 3;
					if (!eventHookRemove[0]) {
						assertThat(listeners)
								.as("does not contain system context").containsKey(systemContext);
						Collection<ListenerInfo> systemListeners = listeners
								.get(systemContext);
						assertThat(systemListeners)
								.as("no system listeiners")
				.isNotNull();
						boolean found = false;
						for (Iterator<ListenerInfo> i = systemListeners
								.iterator(); i.hasNext();) {
							ListenerInfo li = i.next();
							if (li.getFilter().indexOf(getName()) > 0) {
								i.remove();
								found = true;
								break;
							}
						}
						assertThat(found)
								.as("Did not find listener info")
				.isTrue();
					} else {
						assertThat(listeners.containsKey(systemContext))
								.as("does contain system context")
				.isFalse();
					}
				}
			}
		};

		EventListenerHook hook4 = new EventListenerHook() {
			public void event(ServiceEvent event,
					Map<BundleContext, Collection<ListenerInfo>> listeners) {
				if (!filter.match(event.getServiceReference())) {
					return;
				}
				synchronized (hookCalled) {
					hookCalled[++hookCalled[0]] = 4;
					if (!eventHookRemove[0]) {
						assertThat(listeners)
								.as("does not contain system context").containsKey(systemContext);
						Collection<ListenerInfo> systemListeners = listeners
								.get(systemContext);
						assertThat(systemListeners)
								.as("no system listeiners")
				.isNotNull();
						boolean found = false;
						for (Iterator<ListenerInfo> i = systemListeners
								.iterator(); i.hasNext();) {
							ListenerInfo li = i.next();
							if (li.getFilter().indexOf(getName()) > 0) {
								found = true;
								break;
							}
						}
						assertThat(found)
								.as("Did find listener info")
				.isFalse();
					} else {
						assertThat(listeners.containsKey(systemContext))
								.as("does contain system context")
				.isFalse();
					}
				}
			}
		};

		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("name", getName());
		// register event hook 4
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 4");
		ServiceRegistration<EventListenerHook> regHook4 = testContext
				.registerService(EventListenerHook.class, hook4, props);

		// register event hook 2
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 2");
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook2 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook2, props);

		// register event hook 3
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 3");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(20));
		ServiceRegistration<EventListenerHook> regHook3 = testContext
				.registerService(EventListenerHook.class, hook3, props);

		// register event hook 1
		props.put(Constants.SERVICE_DESCRIPTION, "event hook 1");
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(10));
		ServiceRegistration<org.osgi.framework.hooks.service.EventHook> regHook1 = testContext.registerService(
				org.osgi.framework.hooks.service.EventHook.class, hook1, props);

		ServiceRegistration<Runnable> reg1 = null;
		try {
			props.put(Constants.SERVICE_DESCRIPTION, "service 1");
			props.remove(Constants.SERVICE_RANKING);
			reg1 = testContext.registerService(Runnable.class, runIt, props);
			synchronized (hookCalled) {
				assertThat(hookCalled[0])
						.as("all hooks not called")
				.isEqualTo(4);
				assertThat(hookCalled[1])
						.as("hook 1 not called first").isOne();
				assertThat(hookCalled[2])
						.as("hook 2 not called second")
				.isEqualTo(2);
				assertThat(hookCalled[3])
						.as("hook 3 not called third")
				.isEqualTo(3);
				assertThat(hookCalled[4])
						.as("hook 4 not called fourth")
				.isEqualTo(4);
				for (int i = 0; i < hookError.length; i++) {
					if (hookError[i] != null) {
						throw hookError[i];
					}
				}
			}
			synchronized (events) {
				assertThat(events)
						.as("listener not called once").hasSize(1);
				ServiceEvent event = events.get(0);
				assertThat(event.getType())
						.as("type not registered")
				.isEqualTo(ServiceEvent.REGISTERED);
				assertThat(event.getServiceReference())
						.as("wrong service")
				.isEqualTo(reg1.getReference());
			}
		} finally {
			// unregister hook and services
			if (regHook1 != null)
				regHook1.unregister();
			if (regHook2 != null)
				regHook2.unregister();
			if (regHook3 != null)
				regHook3.unregister();
			if (regHook4 != null)
				regHook4.unregister();
			if (reg1 != null)
				reg1.unregister();
			if (sl != null)
				systemContext.removeServiceListener(sl);
		}
	}
}
