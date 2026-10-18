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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.connect.ConnectContent.ConnectEntry;
import org.osgi.framework.connect.ConnectModule;
import org.osgi.framework.connect.ModuleConnector;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.condition.Condition;
import org.osgi.test.cases.framework.launch.junit.connect.test.classes.Activator;

public class ConnectTests extends LaunchTest {

	private File storageArea;

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		storageArea = new File(rootStorageArea, getName());
		storageArea.mkdirs();
	}

	@AfterEach
	public void tearDown() throws Exception {
		super.tearDown();
		if (storageArea != null) {
			assertThat(delete(storageArea))
					.as("Could not clean up storage area: " + storageArea.getPath())
					.isTrue();
		}
	}

	void doTestConnect(ModuleConnector connectFramework,
			Map<String,String> fwkConfig,
			Consumer<Framework> test) {
		fwkConfig.put(Constants.FRAMEWORK_STORAGE, storageArea.getAbsolutePath());
		Framework framework = frameworkFactory
				.newFramework(fwkConfig,
				connectFramework);
		if (framework == null) {
			return;
		}
		boolean passed = false;
		try {
			test.accept(framework);
			passed = true;
		} finally {
			try {
				framework.stop();
				framework.waitForStop(10000);
			} catch (Exception e) {
				if (passed) {
					sneakyThrow(e);
				}
			}
		}
	}

	public static class TestCountingConnectFramework implements ModuleConnector {
		private final AtomicInteger				initializeCalled			= new AtomicInteger();
		private final Queue<String>				getModuleCalled				= new ConcurrentLinkedQueue<>();
		private final AtomicInteger				createBundleActivatorCalled	= new AtomicInteger();
		private final Map<String,ConnectModule>	modules						= new ConcurrentHashMap<String,ConnectModule>();

		@Override
		public void initialize(
				File storage,
				Map<String,String> configuration) {
			initializeCalled.getAndIncrement();
		}

		@Override
		public Optional<ConnectModule> connect(String location)
				throws BundleException {
			getModuleCalled.add(location);
			ConnectModule m = modules.get(location);
			if (m == BUNDLE_EXCEPTION) {
				throw new BundleException(
						"Test throw BundleException from getModule");
			}
			return Optional.ofNullable(m);
		}

		@Override
		public Optional<BundleActivator> newBundleActivator() {
			createBundleActivatorCalled.getAndIncrement();
			return Optional.empty();
		}

		int getInitializeCnt() {
			return initializeCalled.get();
		}

		List<String> getModuleLocations() {
			return new ArrayList<>(getModuleCalled);
		}

		int getCreateBundleActivatorCnt() {
			return createBundleActivatorCalled.get();
		}

		void setModule(String location, ConnectModule module) {
			if (module == null) {
				modules.remove(location);
			} else {
				modules.put(location, module);
			}
		}
	}

	public static class TestConnectModule implements ConnectModule {
		private volatile TestConnectContent content;

		public TestConnectModule(TestConnectContent content) {
			this.content = content;
		}

		@Override
		public TestConnectContent getContent() {
			return content;
		}

		void setContent(TestConnectContent updatedContent) {
			this.content = updatedContent;
		}
	}

	public static class TestConnectContent implements ConnectContent {
		private final Map<String,String>		headers;
		private final Map<String,ConnectEntry>	entries	= new LinkedHashMap<>();
		private final ClassLoader				loader;
		private final AtomicBoolean				isOpen	= new AtomicBoolean();

		public TestConnectContent(Map<String,String> headers,
				ClassLoader loader) {
			this.headers = headers;
			this.loader = loader;
		}

		@Override
		public Optional<Map<String,String>> getHeaders() {
			checkOpen();
			return Optional.ofNullable(headers);
		}

		@Override
		public Iterable<String> getEntries() throws IOException {
			checkOpen();
			return entries.keySet();
		}

		@Override
		public Optional<ConnectEntry> getEntry(String name) {
			checkOpen();
			return Optional.ofNullable(entries.get(name));
		}

		@Override
		public Optional<ClassLoader> getClassLoader() {
			checkOpen();
			return Optional.ofNullable(loader);
		}

		@Override
		public void open() throws IOException {
			if (!isOpen.compareAndSet(false, true)) {
				throw new IllegalStateException("Already Opened.");
			}
		}

		@Override
		public void close() throws IOException {
			if (!isOpen.compareAndSet(true, false)) {
				throw new IllegalStateException("Already Closed.");
			}
		}

		void addEntry(String path, ConnectEntry entry) {
			entries.put(path, entry);
		}

		private void checkOpen() {
			if (!isOpen.get()) {
				throw new IllegalStateException("Not Opened.");
			}
		}

		boolean isOpen() {
			return isOpen.get();
		}
	}

	public static class TestConnectEntryBytes implements ConnectEntry {
		private final String	name;
		private final byte[]	bytes;

		public TestConnectEntryBytes(String name, byte[] bytes) {
			this.name = name;
			this.bytes = bytes;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public long getContentLength() {
			return bytes.length;
		}

		@Override
		public long getLastModified() {
			return 0;
		}

		@Override
		public byte[] getBytes() {
			return bytes.clone();
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(bytes);
		}

	}

	public static class TestConnectEntryURL implements ConnectEntry {
		private final String	name;
		private final URL		content;

		public TestConnectEntryURL(String name, URL content) {
			this.name = name;
			this.content = content;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public long getContentLength() {
			try {
				return content.openConnection().getContentLengthLong();
			} catch (IOException e) {
				return 0;
			}
		}

		@Override
		public long getLastModified() {
			try {
				return content.openConnection().getLastModified();
			} catch (IOException e) {
				return 0;
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return content.openStream();
		}

		public URL getContentURL() {
			return content;
		}

	}

	static final TestConnectModule BUNDLE_EXCEPTION = new TestConnectModule(
			null);

	@Test
	public void testConnectFactoryNoModules() {
		TestCountingConnectFramework connectFactory = new TestCountingConnectFramework();

		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.start();
				f.stop();
				f.waitForStop(5000);
				f.start();
				f.stop();
				f.waitForStop(5000);
			} catch (Throwable t) {
				sneakyThrow(t);
			}
		});
		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.start();
				f.stop();
			} catch (BundleException e) {
				sneakyThrow(e);
			}
		});

		assertThat(connectFactory.getInitializeCnt())
				.as("Wrong number of init called.")
				.isEqualTo(2);
		assertThat(connectFactory.getCreateBundleActivatorCnt())
				.as("Wrong number of create activator called.")
				.isEqualTo(3);
	}

	@Test
	public void testConnectActivator() {
		final AtomicInteger bundleActvatorStartCalled = new AtomicInteger();
		final AtomicInteger bundleActvatorStopCalled = new AtomicInteger();
		ModuleConnector activatorFactory = new TestCountingConnectFramework() {
			@Override
			public Optional<BundleActivator> newBundleActivator() {
				super.newBundleActivator();
				return Optional.of(new BundleActivator() {

					@Override
					public void start(BundleContext context) throws Exception {
						bundleActvatorStartCalled.getAndIncrement();
					}

					@Override
					public void stop(BundleContext context) throws Exception {
						bundleActvatorStopCalled.getAndIncrement();
					}
				});
			}
		};

		doTestConnect(activatorFactory, new HashMap<>(), (f) -> {
			try {
				f.start();
				f.stop();
				f.waitForStop(5000);
				f.start();
				f.stop();
				f.waitForStop(5000);
			} catch (Exception e) {
				sneakyThrow(e);
			}
		});
		assertThat(bundleActvatorStartCalled.get())
				.as("Wrong number of start called.")
				.isEqualTo(2);
		assertThat(bundleActvatorStopCalled.get())
				.as("Wrong number of stop called.")
				.isEqualTo(2);
	}

	@Test
	public void testConditionTrue() {
		final AtomicReference<ServiceReference<Condition>> trueConditionStart = new AtomicReference<>();
		final AtomicReference<ServiceReference<Condition>> trueConditionStop = new AtomicReference<>();
		ModuleConnector activatorFactory = new TestCountingConnectFramework() {
			@Override
			public Optional<BundleActivator> newBundleActivator() {
				super.newBundleActivator();
				return Optional.of(new BundleActivator() {
					@Override
					public void start(BundleContext context) throws Exception {
						trueConditionStart.set(context.getServiceReferences(
								Condition.class,
								'(' + Condition.CONDITION_ID + '='
										+ Condition.CONDITION_ID_TRUE + ')')
								.iterator()
								.next());
					}

					@Override
					public void stop(BundleContext context) throws Exception {
						trueConditionStop.set(context.getServiceReferences(
								Condition.class,
								'(' + Condition.CONDITION_ID + '='
										+ Condition.CONDITION_ID_TRUE + ')')
								.iterator()
								.next());
					}
				});
			}
		};

		doTestConnect(activatorFactory, new HashMap<>(), (f) -> {
			try {
				f.init();
				ServiceReference<Condition> trueCondition = trueConditionStart
						.get();
				assertThat(trueCondition)
						.as("No true condition found.")
						.isNotNull();
				assertThat(trueCondition.getBundle())
						.as("Wrong bundle.")
						.isEqualTo(f.getBundleContext().getBundle());
				f.stop();
				f.waitForStop(5000);

				assertThat(trueConditionStop.get())
						.as("Different true condition found on start and stop.")
						.isEqualTo(trueCondition);
				assertThat(trueCondition.getBundle())
						.as("True condition should be unregistered on stop.")
						.isNull();
			} catch (Exception e) {
				sneakyThrow(e);
			}
		});

	}

	@Test
	public void testConnectInit() {
		final AtomicReference<File> initFile = new AtomicReference<>();
		final AtomicReference<File> storeFile = new AtomicReference<>();
		final AtomicReference<Map<String,String>> initConfig = new AtomicReference<>();
		ModuleConnector activatorFactory = new TestCountingConnectFramework() {
			@Override
			public void initialize(File storage,
					Map<String,String> config) {
				super.initialize(storage, config);
				initFile.set(storage);
				initConfig.set(config);
			}
		};

		Map<String,String> config = new HashMap<>();
		config.put("k1", "v1");
		config.put("k2", "v2");

		doTestConnect(activatorFactory, config, (f) -> {
			try {
				f.init();
				BundleContext bc = f.getBundleContext();
				storeFile.set(
						new File(bc.getProperty(Constants.FRAMEWORK_STORAGE)));
			} catch (Exception e) {
				sneakyThrow(e);
			}
		});
		assertThat(initFile.get())
				.as("Wrong init store file.")
				.isEqualTo(storeFile.get());
		assertThat(initConfig.get().entrySet().containsAll(config.entrySet()))
				.as("Did not find all init configs: " + initConfig.get())
				.isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.as("Expected unmodifiable map")
				.isThrownBy(() -> initConfig.get().put("k3", "v3"));
	}

	@Test
	public void testConnectContentHeaders() throws IOException {
		doTestConnectContentSimple(false);
	}

	@Test
	public void testConnectContentManifest() throws IOException {
		doTestConnectContentSimple(true);
	}

	void doTestConnectContentSimple(boolean withManifest) throws IOException {
		TestCountingConnectFramework connectFactory = new TestCountingConnectFramework();
		AtomicInteger initialBundleCount = new AtomicInteger(0);
		final List<String> locations = Arrays.asList("b.1", "b.2", "b.3",
				"b.4");
		for (String l : locations) {
			connectFactory.setModule(l,
					withManifest ? createSimpleManifestModule(l)
							: createSimpleHeadersModule(l));
		}

		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.init();
				initialBundleCount
						.set(f.getBundleContext().getBundles().length);
				for (String l : locations) {
					Bundle b = f.getBundleContext().installBundle(l);
					assertThat(b.getSymbolicName())
							.as("Wrong symbolic name.")
							.isEqualTo(l);
				}
			} catch (Throwable t) {
				sneakyThrow(t);
			}
		});

		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.init();
				Bundle[] bundles = f.getBundleContext().getBundles();
				assertThat(bundles.length)
						.as("Wrong number of bundles from cache.")
						.isEqualTo(locations.size() + initialBundleCount.get());
				for (String l : locations) {
					Bundle b = f.getBundleContext().getBundle(l);
					assertThat(b)
							.as("No bundle at location: " + l)
							.isNotNull();
					assertThat(b.getSymbolicName())
							.as("Wrong symbolic name.")
							.isEqualTo(l);
				}
			} catch (BundleException e) {
				sneakyThrow(e);
			}
		});
		
		// Nulling out b.2 location so that the factory returns an empty Optional for that location.
		// Setting b.3 location to throw an IllegalStateException to make sure the framework handles that case.
		// The result is that we should see two less bundles below.
		connectFactory.setModule("b.2", null);
		connectFactory.setModule("b.3", BUNDLE_EXCEPTION);
		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.init();
				Bundle[] bundles = f.getBundleContext().getBundles();
				assertThat(bundles.length)
						.as("Wrong number of bundles from cache.")
						.isEqualTo(locations.size() + initialBundleCount.get() - 2);
				for (String l : locations) {
					Bundle b = f.getBundleContext().getBundle(l);
					if ("b.2".equals(l) || "b.3".equals(l)) {
						assertThat(b)
								.as("Found unexpected bundle.")
								.isNull();
					} else {
						assertThat(b)
								.as("No bundle at location: " + l)
								.isNotNull();
						assertThat(b.getSymbolicName())
								.as("Wrong symbolic name.")
								.isEqualTo(l);
					}
				}
			} catch (BundleException e) {
				sneakyThrow(e);
			}
		});
	}

	@Test
	public void testConnectContentActivatorsWithFrameworkLoaders() {
		doTestConnectContentActivators(false);
	}

	@Test
	public void testConnectContentActivatorsWithProvidedLoaders() {
		doTestConnectContentActivators(true);
	}

	void doTestConnectContentActivators(boolean provideLoader) {
		TestCountingConnectFramework connectFactory = new TestCountingConnectFramework();
		final List<Integer> ids = Arrays.asList(1, 2, 3);
		for (Integer id : ids) {
			connectFactory.setModule(id.toString(),
					createAdvancedModule(id, provideLoader));
		}

		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.start();
				for (Integer id : ids) {
					Bundle b = f.getBundleContext()
							.installBundle(id.toString());
					assertThat(b.getSymbolicName())
							.as("Wrong symbolic name.")
							.isEqualTo(id.toString());
					b.start();
					ServiceReference< ? >[] registered = b
							.getRegisteredServices();
					assertThat(registered)
							.as("No services found.")
							.isNotNull();
					assertThat(registered.length)
							.as("Wrong number of services.").isOne();
					assertThat((String) registered[0].getProperty("activator"))
							.as("Wrong service property.")
							.isEqualTo(Activator.class.getSimpleName() + id);
					if (provideLoader) {
						assertThat(Activator.class)
								.as("Expected the same classes.").isEqualTo(b
								.loadClass(Activator.class.getName()));
					} else {
						assertThat(Activator.class.equals(b
										.loadClass(Activator.class.getName())))
								.as("Expected different classes.")
								.isFalse();
					}
				}
			} catch (Throwable t) {
				sneakyThrow(t);
			}
		});
	}

	@Test
	public void testConnectContentEntriesWithFrameworkLoaders() {
		doTestConnectContentEntries(false);
	}

	@Test
	public void testConnectContentEntriesWithProvidedLoaders() {
		doTestConnectContentEntries(true);
	}

	void doTestConnectContentEntries(boolean provideLoader) {
		TestCountingConnectFramework connectFactory = new TestCountingConnectFramework();
		final List<Integer> ids = Arrays.asList(1, 2, 3);
		final Map<Integer,TestConnectModule> modules = new HashMap<>();
		for (Integer id : ids) {
			TestConnectModule m = createAdvancedModule(id, provideLoader);
			modules.put(id, m);
			connectFactory.setModule(id.toString(), m);
		}

		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.start();
				for (Integer id : ids) {
					Bundle b = f.getBundleContext()
							.installBundle(id.toString());
					assertThat(b.getSymbolicName())
							.as("Wrong symbolic name.")
							.isEqualTo(id.toString());
					TestConnectModule m = modules.get(id);
					List<String> entries = new ArrayList<>();
					for (String entry : m.getContent().getEntries()) {
						entries.add(entry);
					}

					Set<String> bundleEntryUrls = new HashSet<>();
					for (Enumeration<URL> eUrls = b.findEntries("/", "*",
							true); eUrls.hasMoreElements();) {
						// URL paths always begin with '/', remove it
						bundleEntryUrls.add(
								eUrls.nextElement().getPath().substring(1));
					}
					assertThat(bundleEntryUrls.size())
							.as("Wrong number of bundle entry URLs.")
							.isEqualTo(entries.size());
					assertThat(entries.containsAll(bundleEntryUrls))
							.as("Wrong bundle entry URLs: " + bundleEntryUrls)
							.isTrue();

					List<String> bundleEntryPaths = new ArrayList<>();
					for (Enumeration<String> ePaths = b.getEntryPaths(
							"org/osgi/test/cases/framework/launch/junit/connect/test/resources/"); ePaths
									.hasMoreElements();) {
						bundleEntryPaths.add(ePaths.nextElement());
					}
					assertThat(bundleEntryPaths)
							.as("Wrong number of bundle entry paths from root.").hasSize(1);
					assertThat(bundleEntryPaths.get(0))
							.as("Wrong bundle entry found at root.")
							.isEqualTo("org/osgi/test/cases/framework/launch/junit/connect/test/resources/"
									+ id
									+ ".txt");

					BundleWiring wiring = b.adapt(BundleWiring.class);
					assertThat(wiring)
							.as("No wiring.")
							.isNotNull();
					Collection<String> wiringResourcePaths = wiring
							.listResources("/", "*",
									BundleWiring.LISTRESOURCES_LOCAL
											| BundleWiring.LISTRESOURCES_RECURSE);
					assertThat(wiringResourcePaths.size())
							.as("Wrong number of resource paths.")
							.isEqualTo(entries.size());
					assertThat(entries.containsAll(wiringResourcePaths))
							.as("Wrong resource paths: " + wiringResourcePaths)
							.isTrue();

					Set<String> wiringEntryUrls = new HashSet<>();
					for (URL url : wiring.findEntries("/", "*",
							BundleWiring.FINDENTRIES_RECURSE)) {
						// URL paths always begin with '/', remove it
						wiringEntryUrls.add(url.getPath().substring(1));
					}
					assertThat(wiringEntryUrls.size())
							.as("Wrong number of wiring entry URLs.")
							.isEqualTo(entries.size());
					assertThat(entries.containsAll(wiringEntryUrls))
							.as("Wrong wiring entry URLs: " + wiringEntryUrls)
							.isTrue();

					String txtPathDir = "org/osgi/test/cases/framework/launch/junit/connect/test/resources/";
					String txtPath = txtPathDir + id + ".txt";
					Optional<ConnectEntry> txtConnectEntry = m.getContent()
							.getEntry(txtPath);
					assertThat(txtConnectEntry)
							.as("Could not find text entry.").isPresent();

					doCheckEntry(provideLoader, id, b, txtPathDir, txtPath,
							txtConnectEntry);

					// now try with leading '/'
					String slashTxtPath = '/' + txtPath;
					doCheckEntry(provideLoader, id, b, '/' + txtPathDir,
							slashTxtPath,
							txtConnectEntry);
				}
			} catch (Throwable t) {
				sneakyThrow(t);
			}
		});
	}

	private void doCheckEntry(boolean provideLoader, Integer id, Bundle b,
			String txtPathDir, String txtPath, Optional<ConnectEntry> txtConnectEntry) throws IOException {
		checkEntry(txtConnectEntry.get(), b.getEntry(txtPath),
				id, false);

		if (provideLoader) {
			checkEntry(txtConnectEntry.get(),
					b.getResource(txtPath), id, true);
		} else {
			checkEntry(txtConnectEntry.get(),
					b.getResource(txtPath), id, false);
		}
		Enumeration<URL> found = b.findEntries(txtPathDir, "*.txt",
				false);
		checkEntry(txtConnectEntry.get(), found.nextElement(), id,
				false);
		assertThat(found.hasMoreElements())
				.as("More entries found.")
				.isFalse();
	}

	@Test
	public void testOpenCloseUpdateConnectContent() {
		final String NAME1 = "testUpdate.1";
		final String NAME2 = "testUpdate.2";
		TestCountingConnectFramework connectFactory = new TestCountingConnectFramework();
		TestConnectModule m = createSimpleHeadersModule(NAME1);
		connectFactory.setModule(NAME1, m);

		doTestConnect(connectFactory, new HashMap<>(), (f) -> {
			try {
				f.start();
				Bundle b = f.getBundleContext().installBundle(NAME1);
				assertThat(b.getSymbolicName())
						.as("Wrong name.")
						.isEqualTo(NAME1);
				// make sure to open the bundle file
				assertThat(b.getEntry("doesNotExist.txt")).isNull();
				TestConnectContent original = m.getContent();
				assertThat(original.isOpen())
						.as("Original content is not open.")
						.isTrue();

				// set the new content but don't update
				m.setContent(createSimpleHeadersContent(NAME2));

				FrameworkWiring fwkWiring = f.adapt(FrameworkWiring.class);
				CountDownLatch refreshDone = new CountDownLatch(1);
				fwkWiring.refreshBundles(Collections.singletonList(b),
						(e) -> refreshDone.countDown());
				refreshDone.await();

				// should still be NAME1
				assertThat(b.getSymbolicName())
						.as("Wrong name.")
						.isEqualTo(NAME1);
				assertThat(original.isOpen())
						.as("Original content is not open.")
						.isTrue();

				// now update should stage in the new content
				b.update();
				assertThat(b.getSymbolicName())
						.as("Wrong name.")
						.isEqualTo(NAME2);
				// make sure to open the bundle file
				assertThat(b.getEntry("doesNotExist.txt")).isNull();
				TestConnectContent newContent = m.getContent();
				assertThat(newContent.isOpen())
						.as("New content is not open.")
						.isTrue();
				assertThat(original.isOpen())
						.as("Original content is open.")
						.isFalse();
			} catch (Throwable t) {
				sneakyThrow(t);
			}
		});
	}

	void checkEntry(ConnectEntry expected, URL actual, Integer id,
			boolean matchUrl)
			throws IOException {

		assertThat(actual)
				.as("No entry found.")
				.isNotNull();
		if (matchUrl) {
			assertThat(actual)
					.as("Wrong path.")
					.isEqualTo(((TestConnectEntryURL) expected).getContentURL());
		} else {
			assertThat(actual.getPath().substring(1))
					.as("Wrong path.")
					.isEqualTo(expected.getName());
		}

		URLConnection connection = actual.openConnection();
		assertThat(connection.getLastModified())
				.as("Wrong last modified.")
				.isEqualTo(expected.getLastModified());
		assertThat(connection.getContentLengthLong())
				.as("Wrong content length.")
				.isEqualTo(expected.getContentLength());
		byte[] expectedBytes = getBytes(expected.getInputStream());
		byte[] actualBytes = getBytes(connection.getInputStream());
		assertThat(actualBytes.length)
				.as("Wrong input steam size.")
				.isEqualTo(expectedBytes.length);
		for (int i = 0; i < expectedBytes.length; i++) {
			assertThat(actualBytes[i])
					.as("Wrong byte at: " + i)
					.isEqualTo(expectedBytes[i]);
		}
		String actualString = new String(actualBytes);
		assertThat(actualString)
				.as("Wrong entry string.")
				.isEqualTo(id.toString());
	}

	TestConnectModule createSimpleHeadersModule(String name) {
		return new TestConnectModule(createSimpleHeadersContent(name));
	}

	TestConnectContent createSimpleHeadersContent(String name) {
		Map<String,String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, name);
		headers.put(Constants.IMPORT_PACKAGE, "org.osgi.framework");
		return new TestConnectContent(headers, null);
	}

	TestConnectModule createSimpleManifestModule(String name)
			throws IOException {
		Manifest manifest = new Manifest();
		Attributes headers = manifest.getMainAttributes();
		headers.putValue("Manifest-Version", "1");
		headers.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.putValue(Constants.BUNDLE_SYMBOLICNAME, name);
		headers.putValue(Constants.IMPORT_PACKAGE, "org.osgi.framework");
		ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
		manifest.write(manifestBytes);
		TestConnectContent c = new TestConnectContent(null, null);
		addEntry("META-INF/MANIFEST.MF", manifestBytes.toByteArray(), c);
		return new TestConnectModule(c);
	}

	TestConnectModule createAdvancedModule(Integer id, boolean provideLoader) {
		Map<String,String> headers = new HashMap<>();
		headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		headers.put(Constants.BUNDLE_SYMBOLICNAME, id.toString());
		headers.put(Constants.IMPORT_PACKAGE, "org.osgi.framework");
		headers.put(Constants.BUNDLE_ACTIVATOR, Activator.class.getName() + id);
		TestConnectContent c = new TestConnectContent(headers,
				provideLoader ? getClass().getClassLoader() : null);
		addEntry("org/", c);
		addEntry("org/osgi/", c);
		addEntry("org/osgi/test/", c);
		addEntry("org/osgi/test/cases/", c);
		addEntry("org/osgi/test/cases/framework/", c);
		addEntry("org/osgi/test/cases/framework/launch/", c);
		addEntry("org/osgi/test/cases/framework/launch/junit/", c);
		addEntry("org/osgi/test/cases/framework/launch/junit/connect/", c);
		addEntry("org/osgi/test/cases/framework/launch/junit/connect/test/", c);
		addEntry(
				"org/osgi/test/cases/framework/launch/junit/connect/test/classes/",
				c);
		addEntry(
				"org/osgi/test/cases/framework/launch/junit/connect/test/classes/Activator.class",
				c);
		addEntry(
				"org/osgi/test/cases/framework/launch/junit/connect/test/classes/Activator"
						+ id
				+ ".class", c);
		addEntry(
				"org/osgi/test/cases/framework/launch/junit/connect/test/resources/",
				c);
		addEntry(
				"org/osgi/test/cases/framework/launch/junit/connect/test/resources/"
				+ id + ".txt", c);
		return new TestConnectModule(c);
	}

	void addEntry(String name, TestConnectContent content) {
		content.addEntry(name, new TestConnectEntryURL(name,
				getClass().getResource("/" + name)));
	}

	void addEntry(String name, byte[] bytes, TestConnectContent content) {
		content.addEntry(name, new TestConnectEntryBytes(name, bytes));
	}

	static byte[] getBytes(InputStream in) throws IOException {
		byte[] classbytes;
		int bytesread = 0;
		int readcount;
		try {
			int length = 1024;
			classbytes = new byte[length];
			readloop: while (true) {
				for (; bytesread < length; bytesread += readcount) {
					readcount = in.read(classbytes, bytesread,
							length - bytesread);
					if (readcount <= 0) /* if we didn't read anything */
						break readloop; /* leave the loop */
				}
				byte[] oldbytes = classbytes;
				length += 1024;
				classbytes = new byte[length];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}

			if (classbytes.length > bytesread) {
				byte[] oldbytes = classbytes;
				classbytes = new byte[bytesread];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}
		} finally {
			try {
				in.close();
			} catch (IOException ee) {
				// nothing to do here
			}
		}
		return classbytes;
	}

	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
