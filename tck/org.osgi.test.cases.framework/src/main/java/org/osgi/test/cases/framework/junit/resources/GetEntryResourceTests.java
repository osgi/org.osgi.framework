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
package org.osgi.test.cases.framework.junit.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

public class GetEntryResourceTests {

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	private String testName;

	/** The name of the running test method, as TestCase.getName() gave it. */
	private String getName() {
		return testName;
	}
	Bundle testBundle;
	Bundle testFragment;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		testName = testInfo.getTestMethod().get().getName();
		if (getName().startsWith("testFragment"))
			testFragment = bundleInstaller.installBundle("resources.tb2.jar", false);
		testBundle = bundleInstaller.installBundle("resources.tb1.jar", false);
	}

	@Test
	public void testGetEntryRoot() {
		URL root = testBundle.getEntry("/");
		assertThat(root).isNotNull();
	}

	@Test
	public void testGetEntry01() {
		URL datatxt = testBundle.getEntry("resources/data.txt");
		assertThat(datatxt).isNotNull();
		assertURL(datatxt);
		URL dataxml = testBundle.getEntry("resources/data.xml");
		assertURL(dataxml);

		URL doesNotExist = testBundle.getEntry("DoesNotExist");
		assertThat(doesNotExist)
				.as("Found unexpected resource: " + doesNotExist)
				.isNull();
	}

	@Test
	public void testGetEntry02() {
		URL base = testBundle.getEntry("resources/data.txt");
		assertThat(base).isNotNull();
		// test using a file as a relative URL
		URL dataxml = newURL(base, "data.xml");
		assertURL(dataxml);

		URL dir1 = newURL(base, "dir1/");
		assertThat(dir1.getPath())
				.as("Wrong path for dir1")
				.isEqualTo("/resources/dir1/");

		// test using the root URL as a base
		URL root = testBundle.getEntry("/");
		assertThat(root).isNotNull();
		URL datatxt = newURL(root, "resources/data.txt");
		assertURL(datatxt);
		dataxml = newURL(root, "resources/data.xml");
		assertURL(dataxml);

		// test directory entries
		datatxt = newURL(dir1, "../data.txt");
		assertURL(datatxt);
		datatxt = newURL(dir1, "../dir1/../data.txt");
		assertURL(datatxt);
	}

	@Test
	public void testFindEntries01() {
		doTestFindEntries(1);
	}

	@Test
	public void testFragmentFindEntries01() {
		doTestFindEntries(2);
	}

	private void doTestFindEntries(int factor) {
		assertFindEntries(testBundle, "resources", null, false, 4 * factor);
		assertFindEntries(testBundle, "resources", "*", false, 4 * factor);
		assertFindEntries(testBundle, "resources", null, true, 14 * factor);
		assertFindEntries(testBundle, "resources", "*", true, 14 * factor);
		assertFindEntries(testBundle, "resources", "*.xml", true, 5 * factor);
		assertFindEntries(testBundle, "resources", "data*.xml", true, 5 * factor);
		assertFindEntries(testBundle, "resources", "*.xml", false, 1 * factor);
		assertFindEntries(testBundle, "resources", "data*.xml", false, 1 * factor);
		assertFindEntries(testBundle, "resources", "data.txt", false, 1 * factor);
		assertFindEntries(testBundle, "", "data.txt", true, 1 * factor);
		assertFindEntries(testBundle, "resources", "data*", true, 10 * factor);
		assertFindEntries(testBundle, "resources", "*d*ta*.*", true, 10 * factor);
		assertFindEntries(testBundle, "resources", "doesNotExist", true, 0 * factor);
		assertFindEntries(testBundle, "resources", "doesNotExist", false, 0 * factor);
		assertFindEntries(testBundle, "resources", "does*Not*Exist", true, 0 * factor);
		assertFindEntries(testBundle, "resources", "dir*", false, 2 * factor);
		assertFindEntries(testBundle, "resources", "dir*", true, 4 * factor);
	}

	@Test
	public void testGetEntryPaths01() {
		assertEntryPaths(testBundle, "resources", new String[] {"resources/dir1/", "resources/dir2/", "resources/data.txt", "resources/data.xml"});
		assertEntryPaths(testBundle, "resources/dir1", new String[] {"resources/dir1/dir1a/", "resources/dir1/data1.txt", "resources/dir1/data1.xml"});

	}

	private void assertEntryPaths(Bundle bundle, String path, String[] expected) {
		Enumeration<String> paths = bundle.getEntryPaths(path);
		if (expected != null)
			assertThat(paths).isNotNull();
		else {
			assertThat(paths).isNull();
			return;
		}

		ArrayList<String> expectedList = new ArrayList<>(expected.length);
		for (int i = 0; i < expected.length; i++)
			expectedList.add(expected[i]);

		int numFound = 0;
		while (paths.hasMoreElements()) {
			Object element = paths.nextElement();
			if (!expectedList.contains(element))
				fail("Unexpected entry found: " + element);
			numFound++;
		}
		assertThat(numFound)
				.as("Unexpected number of entries")
				.isEqualTo(expected.length);
	}

	private void assertFindEntries(Bundle bundle, String path, String wildCard, boolean recurse, int expectedNum) {
		assertBundleFindEntries(bundle, path, wildCard, recurse, expectedNum);
		assertWiringFindEntries(bundle, path, wildCard, recurse, expectedNum);
	}
	private void assertBundleFindEntries(Bundle bundle, String path, String wildCard, boolean recurse, int expectedNum) {
		Enumeration<URL> entries = bundle.findEntries(path, wildCard, recurse);
		if (expectedNum > 0)
			assertThat(entries).isNotNull();
		else {
			assertThat(entries).isNull();
			return;
		}

		int numFound = 0;
		while (entries.hasMoreElements()) {
			assertURL(entries.nextElement());
			numFound++;
		}
		assertThat(numFound)
				.as("Unexpected number of entries")
				.isEqualTo(expectedNum);
	}

	private void assertWiringFindEntries(Bundle bundle, String path, String wildCard, boolean recurse, int expectedNum) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		List<URL> entries = wiring.findEntries(path, wildCard,
				recurse ? BundleWiring.FINDENTRIES_RECURSE : 0);
		assertThat(entries)
				.as("Entries should not be null.")
				.isNotNull();

		Iterator<URL> iEntries = entries.iterator();
		int numFound = 0;
		while (iEntries.hasNext()) {
			assertURL(iEntries.next());
			numFound++;
		}
		assertThat(numFound)
				.as("Unexpected number of entries")
				.isEqualTo(expectedNum);
	}

	@AfterEach
	public void tearDown() throws Exception {
		testBundle.uninstall();
		if (testFragment != null)
			testFragment.uninstall();
	}

	private URL newURL(URL base, String path) {
		try {
			return new URL(base, path);
		} catch (MalformedURLException e) {
			fail("Failed to create URL: " + base + " " + path, e);
		}
		return null;
	}

	private void assertURL(URL url) {
		assertThat(url)
				.as("Unexpected null url")
				.isNotNull();
		String expected = url.getPath();
		int lastSlash = expected.lastIndexOf('/');
		assertThat(lastSlash).isNotNegative();
		if (lastSlash == expected.length() - 1)
			return;
		expected = expected.substring(lastSlash + 1);
		InputStream ins = null;
		try {
			ins = url.openStream();
			assertThat(ins)
					.as("Expecting to open stream to resource")
				.isNotNull();
			BufferedReader bufr = new BufferedReader(new InputStreamReader(ins));
			String line = bufr.readLine();
			assertThat(line)
					.as("Incorrect content in: " + url)
				.isEqualTo(expected);
		} catch (IOException e) {
			fail("Failed to read resource: " + url, e);
		}
		finally {
			if (ins != null)
				try {
					ins.close();
				} catch (IOException e) {
					// nothing
				}
		}
	}
}
