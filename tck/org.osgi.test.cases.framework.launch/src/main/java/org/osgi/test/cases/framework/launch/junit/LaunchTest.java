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
package org.osgi.test.cases.framework.launch.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public abstract class LaunchTest {

	private String testName;

	@BeforeEach
	public void captureTestName(TestInfo testInfo) {
		testName = testInfo.getTestMethod().get().getName();
	}

	/** The name of the running test method, as TestCase.getName() gave it. */
	protected String getName() {
		return testName;
	}

	private static final Pattern	QUOTED_P			= Pattern
			.compile("^([\"'])(.*)\\1$");
	private static final String FRAMEWORK_FACTORY = "/META-INF/services/org.osgi.framework.launch.FrameworkFactory";
	private static final String		CONNECT_FRAMEWORK_FACTORY	= "/META-INF/services/org.osgi.framework.connect.ConnectFrameworkFactory";
	private static final String	STORAGEROOT	= "org.osgi.test.cases.framework.launch.storageroot";

	static private class FrameworkClassLoader extends URLClassLoader {
		// All java.* and org.osgi.* packages need to be parent only delegation
		private static final String[] PARENT_ONLY_DELEGATION = {
				"java.", "org.osgi."
		};

		// HACK to make plurl work in the TCK
		private static final String[] PARENT_ONLY_DELEGATION_CONTAINS = {
				".plurl."
		};

		public FrameworkClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
			assertThat(parent)
					.as("Cannot have null parent.")
					.isNotNull();
		}

		@Override
		protected Class< ? > loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			Class< ? > clazz = findLoadedClass(name);
			if (clazz != null)
				return clazz;

			if (childFirst(name)) {
				try {
					clazz = findClass(name);
				} catch (ClassNotFoundException e) {
					// continue to parent
				}
			}
			if (clazz == null) {
				try {
					clazz = getParent().loadClass(name);
				} catch (ClassNotFoundException e) {
					// it may be a class from an extension; need to look locally
					if (!childFirst(name)) {
						clazz = findClass(name);
					}
				}
			}

			if (resolve) {
				resolveClass(clazz);
			}
			return clazz;
		}

		private static boolean childFirst(String name) {
			for (int i = PARENT_ONLY_DELEGATION.length - 1; i >= 0; i--) {
				if (name.startsWith(PARENT_ONLY_DELEGATION[i])) {
					return false;
				}
			}
			for (int i = PARENT_ONLY_DELEGATION_CONTAINS.length - 1; i >= 0; i--) {
				if (name.contains(PARENT_ONLY_DELEGATION_CONTAINS[i])) {
					return false;
				}
			}
			// all other packages are considered child first
			return true;

		}
	}

	static protected class LaunchFrameworkFactory
			implements FrameworkFactory, ConnectFrameworkFactory {
		private final String			frameworkFactoryClassName;
		private final String			connectFrameworkFactoryClassName;
		private FrameworkClassLoader	frameworkClassLoader;
		private FrameworkFactory		frameworkFactory;
		private ConnectFrameworkFactory	connectFrameworkFactory;

		public LaunchFrameworkFactory(String frameworkFactoryClassName,
				String connectFrameworkFactoryClassName) {
			this.frameworkFactoryClassName = frameworkFactoryClassName;
			this.connectFrameworkFactoryClassName = connectFrameworkFactoryClassName;
		}
		@Override
		public Framework newFramework(Map<String,String> configuration) {
			return getFrameworkFactory().newFramework(configuration);
		}

		@Override
		public Framework newFramework(Map<String,String> configuration,
				ModuleConnector connectFramework) {
			ConnectFrameworkFactory factory = getConnectFrameworkFactory();
			if (factory == null) {
				// TODO will need to move the connect tests to another project
				// if we make it optional
				fail("No ConnectFrameworkFactory");
			}
			return factory.newFramework(configuration, connectFramework);
		}
		public void close() {
			if (frameworkClassLoader != null) {
				try {
					frameworkClassLoader.close();
				} catch (IOException e) {
					System.out.println("Error closing framework class loader: "
									+ e.getMessage());
					e.printStackTrace();
				}
				frameworkClassLoader = null;
				frameworkFactory = null;
			}
		}

		private FrameworkFactory getFrameworkFactory() {
			if (frameworkFactory != null) {
				return frameworkFactory;
			}
			try {
				if (frameworkClassLoader == null) {
					frameworkClassLoader = createFrameworkClassLoader();
				}
				@SuppressWarnings("unchecked")
				Class<FrameworkFactory> clazz = (Class<FrameworkFactory>) frameworkClassLoader
						.loadClass(frameworkFactoryClassName);
				frameworkFactory = clazz.getConstructor().newInstance();
			} catch (Exception e) {
				fail("Failed to get the framework constructor", e);
			}
			return frameworkFactory;
		}

		private ConnectFrameworkFactory getConnectFrameworkFactory() {
			if (connectFrameworkFactoryClassName == null) {
				return null;
			}
			if (connectFrameworkFactory != null) {
				return connectFrameworkFactory;
			}
			try {
				if (frameworkClassLoader == null) {
					frameworkClassLoader = createFrameworkClassLoader();
				}
				@SuppressWarnings("unchecked")
				Class<ConnectFrameworkFactory> clazz = (Class<ConnectFrameworkFactory>) frameworkClassLoader
						.loadClass(connectFrameworkFactoryClassName);
				connectFrameworkFactory = clazz.getConstructor().newInstance();
			} catch (Exception e) {
				fail("Failed to get the framework constructor", e);
			}
			return connectFrameworkFactory;
		}
	}

	protected LaunchFrameworkFactory	frameworkFactory;
	private List<String>			rootBundles	= new LinkedList<String>();
	protected String				rootStorageArea;

	/** System property with a default: the launch tests run without a hosting framework, as OSGiTestCase.getProperty fell back to. */
	protected String getProperty(String key, String defaultValue) {
		String value = System.getProperty(key);
		return (value == null) ? defaultValue : value;
	}

	@BeforeEach
	public void setUp() throws Exception {
		rootStorageArea = getStorageAreaRoot();
		assertThat(rootStorageArea)
				.as("No storage area root found")
				.isNotNull();
		File rootFile = new File(rootStorageArea);
		assertThat(rootFile.exists() && !rootFile.isDirectory())
				.as("Root storage area is not a directory: " + rootFile.getPath())
				.isFalse();
		if (!rootFile.isDirectory())
			assertThat(rootFile.mkdirs())
					.as("Could not create root directory: " + rootFile.getPath())
					.isTrue();
		String frameworkFactoryClassName = getFrameworkFactoryClassName();
		assertThat(frameworkFactoryClassName)
				.as("Could not find framework factory class")
				.isNotNull();
		String connectFrameworkFactoryClassName = getConnectFrameworkFactoryClassName(); // allow
																							// null
		frameworkFactory = new LaunchFrameworkFactory(
				frameworkFactoryClassName, connectFrameworkFactoryClassName);
		StringTokenizer st = new StringTokenizer(getProperty(
				"org.osgi.test.cases.framework.launch.bundles", ""), ",");
		rootBundles.clear();
		while (st.hasMoreTokens()) {
			String bundle = st.nextToken();
			assertThat(bundle).isNotNull();
			rootBundles.add(bundle);
		}
	}

	static FrameworkClassLoader createFrameworkClassLoader()
			throws MalformedURLException {

		// Since bnd 4.3.0 the framework is on the launcher.runpath system
		// property.
		// TODO Remove the old approach one day.
		String pathProp = System.getProperty("launcher.runpath",
				System.getProperty("java.class.path"));

		Matcher matcher = QUOTED_P.matcher(pathProp);
		if (matcher.matches()) {
			pathProp = matcher.group(2);
		}

		String[] classpaths = pathProp
				.split("\\s*[," + File.pathSeparator + "]\\s*");

		List<URL> frameworkImpl = new ArrayList<>();
		for (String classpath : classpaths) {
			File file = new File(classpath);
			if (!file.getName().startsWith("biz.aQute")) {
				frameworkImpl.add(file.toURI().toURL());
			}
		}
		return new FrameworkClassLoader(frameworkImpl.toArray(new URL[0]),
				LaunchTest.class.getClassLoader());
	}

	@AfterEach
	public void tearDown() throws Exception {
		frameworkFactory.close();
	}

	protected Framework createFramework(Map<String, String> configuration) {
		Framework framework = null;
		try {
			framework = frameworkFactory.newFramework(configuration);
		}
		catch (Exception e) {
			fail("Failed to construct the framework", e);
		}
		assertThat(framework)
				.as("Wrong state for newly constructed framework").isInState(Bundle.INSTALLED);
		return framework;
	}
	
	protected URL getBundleInput(String bundle) {
		return getClass().getResource(bundle);
	}
	
	protected void initFramework(Framework framework,
			FrameworkListener... listeners) {
		boolean unintialized = (framework.getState() & (Framework.INSTALLED | Framework.RESOLVED)) != 0;
		try {
			if (listeners == null || listeners.length == 0) {
				framework.init();
			} else {
				framework.init(listeners);
			}
			assertThat(framework.getBundleContext())
					.as("BundleContext is null after init")
					.isNotNull();
		}
		catch (BundleException e) {
			fail("Unexpected BundleException initializing", e);
		}
		if (unintialized) {
			installRootBundles(framework);
		}
		assertThat(framework)
				.as("Wrong framework state after init").isInState(Bundle.STARTING);
	}
	
	protected Bundle installBundle(Framework framework, String bundle) throws BundleException, IOException {
		return installBundle(framework, bundle, bundle);
	}
	
	protected Bundle installBundle(Framework framework, String bundle, String location) throws BundleException, IOException {
		BundleContext fwkContext = framework.getBundleContext();
		assertThat(fwkContext)
				.as("Framework context is null")
				.isNotNull();
		URL input = getBundleInput(bundle);
		assertThat(input)
				.as("Cannot find resource: " + bundle)
				.isNotNull();
		return fwkContext.installBundle(location, input.openStream());
	}
	
	protected void startFramework(Framework framework) {
		boolean unintialized = (framework.getState() & (Framework.INSTALLED | Framework.RESOLVED)) != 0;
		try {
			framework.start();
			assertThat(framework.getBundleContext())
					.as("BundleContext is null after start")
					.isNotNull();
		}
		catch (BundleException e) {
			fail("Unexpected BundleException initializing", e);
		}
		if (unintialized) {
			installRootBundles(framework);
		}
		assertThat(framework)
				.as("Wrong framework state after init").isInState(Bundle.ACTIVE);

	}
	
	protected void stopFramework(Framework framework) {
		int previousState = framework.getState();
		try {
            framework.stop();
			FrameworkEvent event = framework.waitForStop(10000);
			assertThat(event)
					.as("FrameworkEvent is null")
					.isNotNull();
			assertThat(event.getType())
					.as("Wrong event type")
					.isEqualTo(FrameworkEvent.STOPPED);
			assertThat(framework.getBundleContext())
					.as("BundleContext is not null after stop")
					.isNull();
		}
		catch (BundleException e) {
			fail("Unexpected BundleException stopping", e);
		}
		catch (InterruptedException e) {
			fail("Unexpected InterruptedException waiting for stop", e);
		}
		// if the framework was not STARTING STOPPING or ACTIVE then we assume the waitForStop returned immediately with a FrameworkEvent.STOPPED
		// and does not change the state of the framework
		int expectedState = (previousState & (Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING)) != 0 ? Bundle.RESOLVED : previousState;
		assertThat(framework.getState())
				.as("Wrong framework state after stop")
				.isEqualTo(expectedState);
	}
	
	private String getClassName(URL factoryService) throws IOException {
		InputStream in = factoryService.openStream();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(in));
			for (String line = br.readLine(); line != null; line=br.readLine()) {
				int pound = line.indexOf('#');
				if (pound >= 0)
					line = line.substring(0, pound);
				line.trim();
				if (!"".equals(line))
					return line;
			}
		} finally {
			try {
				if (br != null)
					br.close();
			}
			catch (IOException e) {
				// did our best; just ignore
			}
		}
		return null;
	}
	
	private String getFrameworkFactoryClassName() throws IOException {
		URL factoryService = getClass().getResource(FRAMEWORK_FACTORY);
		assertThat(factoryService)
				.as("Could not locate: " + FRAMEWORK_FACTORY)
				.isNotNull();
		return getClassName(factoryService);
	}

	private String getConnectFrameworkFactoryClassName() throws IOException {
		URL factoryService = getClass().getResource(CONNECT_FRAMEWORK_FACTORY);
		assertThat(factoryService)
				.as("Could not locate: " + CONNECT_FRAMEWORK_FACTORY)
				.isNotNull();
		return getClassName(factoryService);
	}

	private void installRootBundles(Framework framework) {
		List<Bundle> bundles = new LinkedList<Bundle>();

		BundleContext fwkContext = framework.getBundleContext();
		assertThat(fwkContext)
				.as("Framework context is null")
				.isNotNull();
		for (String bundle : rootBundles) {
			try {
				Bundle b = fwkContext.installBundle("file:" + bundle);
				assertThat(b)
						.as("Cannot install bundle: " + bundle)
						.isNotNull();
				System.out.println("installed bundle " + b.getSymbolicName()
						+ " " + b.getVersion());
				bundles.add(b);
			}
			catch (BundleException e) {
				e.printStackTrace();
				fail("Unexpected BundleException installing root ", e);
			}
		}

		for (Bundle b : bundles) {
			if (b.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				try {
					b.start();
				}
				catch (BundleException e) {
					fail("Unexpected BundleException starting root ", e);
				}
				System.out.println("started bundle " + b.getSymbolicName());
			}
		}
	}

	private String getStorageAreaRoot() {
		String storageroot = System.getProperty(STORAGEROOT);
			assertThat(storageroot)
					.as("Must set property: " + STORAGEROOT)
					.isNotNull();
			return storageroot;
	}

	protected File getStorageArea(String testName, boolean delete) {
		File storageArea = new File(rootStorageArea, testName);
		if (delete) {
			assertThat(delete(storageArea))
					.as("Could not clean up storage area: " + storageArea.getPath())
					.isTrue();
			assertThat(storageArea.mkdirs())
					.as("Could not create storage area directory: " + storageArea.getPath())
					.isTrue();
		}
		return storageArea;
	}

	protected boolean delete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				String list[] = file.list();
				if (list != null) {
					int len = list.length;
					for (int i = 0; i < len; i++)
						if (!delete(new File(file, list[i])))
							return false;
				}
			}
	
			return file.delete();
		}
		return (true);
	}

	protected Map<String, String> getConfiguration(String testName) {
		return getConfiguration(testName, true);
	}

	protected Map<String, String> getConfiguration(String testName, boolean delete) {
		Map<String, String> configuration = new HashMap<String, String>();
		if (testName != null)
			configuration.put(Constants.FRAMEWORK_STORAGE, getStorageArea(testName, delete).getAbsolutePath());
		return configuration;
	}
}
