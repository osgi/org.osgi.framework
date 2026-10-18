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
import static org.assertj.core.api.Assertions.fail;

import java.util.Hashtable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.cases.framework.junit.tck.TckProperties;

public class ServiceExceptionTests {

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
	public void testServiceException01() {
		// test a service factory which returns wrong object types
		ServiceExceptionServiceFactory wrongObjectFactory = new ServiceExceptionServiceFactory("A String"); //$NON-NLS-1$
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("name", getName()); //$NON-NLS-1$ 
		ServiceRegistration< ? > reg = bundleContext.registerService(
				Runnable.class.getName(), wrongObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(
				bundleContext.getBundle(), null, ServiceException.FACTORY_ERROR);
		bundleContext.addFrameworkListener(listener);
		try {
			ServiceReference< ? >[] refs = null;
			try {
				refs = bundleContext.getServiceReferences(
						Runnable.class.getName(),
						"(name=" + getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull(); //$NON-NLS-1$
			assertThat(refs.length)
					.as("Wrong number of references").isOne(); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) bundleContext.getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertThat(service)
					.as("service is not null")
				.isNull(); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			bundleContext.ungetService(refs[0]);
			Error error = wrongObjectFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				bundleContext.removeFrameworkListener(listener);
		}
	}

	@Test
	public void testServiceException02() {
		// test a service factory which returns null objects
		ServiceExceptionServiceFactory nullObjectFactory = new ServiceExceptionServiceFactory(null);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("name", getName()); //$NON-NLS-1$ 
		ServiceRegistration< ? > reg = bundleContext.registerService(
				Runnable.class.getName(), nullObjectFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(
				bundleContext.getBundle(), null, ServiceException.FACTORY_ERROR);
		bundleContext.addFrameworkListener(listener);
		try {
			ServiceReference< ? >[] refs = null;
			try {
				refs = bundleContext.getServiceReferences(
						Runnable.class.getName(),
						"(name=" + getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull(); //$NON-NLS-1$
			assertThat(refs.length)
					.as("Wrong number of references").isOne(); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) bundleContext.getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertThat(service)
					.as("service is not null")
				.isNull(); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			bundleContext.ungetService(refs[0]);
			Error error = nullObjectFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				bundleContext.removeFrameworkListener(listener);
		}
	}

	@Test
	public void testServiceException03() {
		// test a service factory which throws a RuntimeException
		RuntimeException cause = new RuntimeException(getName());
		ServiceExceptionServiceFactory runtimeExceptionFactory = new ServiceExceptionServiceFactory(cause);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("name", getName()); //$NON-NLS-1$ 
		ServiceRegistration< ? > reg = bundleContext.registerService(
				Runnable.class.getName(), runtimeExceptionFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(
				bundleContext.getBundle(), cause,
				ServiceException.FACTORY_EXCEPTION);
		bundleContext.addFrameworkListener(listener);
		try {
			ServiceReference< ? >[] refs = null;
			try {
				refs = bundleContext.getServiceReferences(
						Runnable.class.getName(),
						"(name=" + getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull(); //$NON-NLS-1$
			assertThat(refs.length)
					.as("Wrong number of references").isOne(); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) bundleContext.getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertThat(service)
					.as("service is not null")
				.isNull(); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			bundleContext.ungetService(refs[0]);
			Error error = runtimeExceptionFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				bundleContext.removeFrameworkListener(listener);
		}
	}

	@Test
	public void testServiceException04() {
		// test a service factory which throws an Error
		Error cause = new Error(getName());
		ServiceExceptionServiceFactory errorFactory = new ServiceExceptionServiceFactory(cause);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("name", getName()); //$NON-NLS-1$ 
		ServiceRegistration< ? > reg = bundleContext.registerService(
				Runnable.class.getName(), errorFactory, props);
		ServiceExceptionFrameworkListener listener = new ServiceExceptionFrameworkListener(
				bundleContext.getBundle(), cause,
				ServiceException.FACTORY_EXCEPTION);
		bundleContext.addFrameworkListener(listener);
		try {
			ServiceReference< ? >[] refs = null;
			try {
				refs = bundleContext.getServiceReferences(
						Runnable.class.getName(),
						"(name=" + getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (InvalidSyntaxException e) {
				fail("Unexpected syntax error", e); //$NON-NLS-1$
			}
			assertThat(refs)
					.as("service refs is null")
				.isNotNull(); //$NON-NLS-1$
			assertThat(refs.length)
					.as("Wrong number of references").isOne(); //$NON-NLS-1$
			Runnable service = null;
			try {
				service = (Runnable) bundleContext.getService(refs[0]);
			} catch (ClassCastException e) {
				fail("Unexpected cast exception", e); //$NON-NLS-1$
			}
			assertThat(service)
					.as("service is not null")
				.isNull(); //$NON-NLS-1$
			listener.waitForEvent("Failed to fire ServiceException"); //$NON-NLS-1$
			bundleContext.ungetService(refs[0]);
			Error error = errorFactory.getUngetFailure();
			if (error != null)
				throw error;
		} finally {
			if (reg != null)
				reg.unregister();
			if (listener != null)
				bundleContext.removeFrameworkListener(listener);
		}
	}

	class ServiceExceptionServiceFactory implements ServiceFactory<Object> {
		private final Object serviceOrThrowable;
		private Error ungetFailure;

		public ServiceExceptionServiceFactory(Object serviceOrThrowable) {
			this.serviceOrThrowable = serviceOrThrowable;
		}

		public Object getService(Bundle bundle,
				ServiceRegistration<Object> registration) {
			if (serviceOrThrowable instanceof RuntimeException)
				throw (RuntimeException) serviceOrThrowable;
			if (serviceOrThrowable instanceof Error)
				throw (Error) serviceOrThrowable;
			return serviceOrThrowable;
		}

		public synchronized void ungetService(Bundle bundle,
				ServiceRegistration<Object> registration, Object service) {
			try {
				if (serviceOrThrowable instanceof RuntimeException)
					fail("Unexpected call to ungetService: " + serviceOrThrowable); //$NON-NLS-1$
				if (serviceOrThrowable instanceof Error)
					fail("Unexpected call to ungetService: " + serviceOrThrowable); //$NON-NLS-1$
			} catch (Error error) {
				ungetFailure = error;
			}
		}

		public Error getUngetFailure() {
			return ungetFailure;
		}
	}

	static final long	waitTime	= 10000l * TckProperties
													.getScaling();
	class ServiceExceptionFrameworkListener implements FrameworkListener {
		private final Bundle registrationBundle;
		private final Throwable exception;
		private final int exceptionType;
		private boolean waitForEvent = true;

		public ServiceExceptionFrameworkListener(Bundle registrationBundle, Throwable exception, int exceptionType) {
			this.registrationBundle = registrationBundle;
			this.exception = exception;
			this.exceptionType = exceptionType;
		}

		public void frameworkEvent(FrameworkEvent event) {
			if (event.getBundle() != registrationBundle)
				return;
			if (!(event.getThrowable() instanceof ServiceException))
				return;
			if (((ServiceException) event.getThrowable()).getCause() != exception)
				return;
			if (((ServiceException) event.getThrowable()).getType() != exceptionType)
				return;
			notifyWaiter();
		}

		private synchronized void notifyWaiter() {
			waitForEvent = false;
			notifyAll();
		}

		public synchronized void waitForEvent(String failMessage) {
			long timeout = System.currentTimeMillis() + waitTime;
			while (waitForEvent && (System.currentTimeMillis() < timeout)) {
				try {
					wait(waitTime);
				} catch (InterruptedException e) {
					fail("unexpected interuption", e); //$NON-NLS-1$
				}
			}
			// still waiting for event; we now fail
			if (waitForEvent)
				fail(failMessage);
		}
	}
}
