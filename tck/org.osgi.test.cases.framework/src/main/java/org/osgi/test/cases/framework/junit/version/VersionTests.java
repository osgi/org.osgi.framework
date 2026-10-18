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
package org.osgi.test.cases.framework.junit.version;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

/**
 * Tests for the Version class.
 */
public class VersionTests {
	/**
	 * Test the Version constructor with legal parameters
	 *
	 * @osgi.spec Version.<init>(int,int,int)
	 */
	@Test
	public void testConstructors() {
		/**
		 * Test the Version constructor with legal parameters
		 *
		 * @osgi.spec Version.<init>(int,int,int)
		 */
		new Version(0, 0, 0);

		new Version(0, 0, 0,
				"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-0123456789");

		new Version("0.0.0");
		Version.parseVersion("0.0.0");
		Version.valueOf("0.0.0");
		new Version(
				"0.0.0.ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-0123456789");
		Version.parseVersion("0.0.0.ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-0123456789");
		Version.valueOf("0.0.0.ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_-0123456789");

	}

	@Test
	public void testConstructorsBadArguments() {
		/**
		 * Test the Version constructor with illegal parameters
		 *
		 * @osgi.spec Version.<init>(int,int,int)
		 */
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(-1, 2, 3));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(1, -2, 3));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(1, 2, -3));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(1, 2, 3, "4.5"));

		assertThatExceptionOfType(RuntimeException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(null));

		assertThatExceptionOfType(RuntimeException.class)
				.as("Version created with null argument")
				.isThrownBy(() -> Version.valueOf(null));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(""));

		testConstructorsBadArguments("-1.2.3");
		testConstructorsBadArguments("1.-2.3");
		testConstructorsBadArguments("1.2.-3");
		testConstructorsBadArguments("1.2.3.4.5");

		testConstructorsBadArguments("1.2.3.");
		testConstructorsBadArguments("1.2.3-");
		testConstructorsBadArguments("1.2.3-4.5");
		testConstructorsBadArguments("a.2.3");
		testConstructorsBadArguments("1.b.3");
		testConstructorsBadArguments("1.2.c");
		testConstructorsBadArguments("1.");
		testConstructorsBadArguments("1.2.");
	}

	@Test
	private void testConstructorsBadArguments(String arg) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> new Version(arg));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> Version.parseVersion(arg));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("Version created with illegal arguments")
				.isThrownBy(() -> Version.valueOf(arg));
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEquals() {
		Version version1;
		Version version2;

		version1 = new Version(0, 0, 0);
		version2 = new Version(0, 0, 0);

		assertThat(version2)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version1);

		/**
		 * Test the method equals() with the same versions
		 *
		 * @osgi.spec Version.equals(Object)
		 */

		version1 = new Version(0, 0, 0, "a");
		version2 = new Version(0, 0, 0, "a");

		assertThat(version1)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version1);
		assertThat(version2)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version2);
		assertThat(version2)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version1);
		assertThat(version1)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version2);

		/**
		 * Test the method equals() with different versions
		 *
		 * @osgi.spec Version.equals(Object)
		 */

		version1 = new Version(0, 0, 0);
		version2 = new Version(1, 0, 0);

		assertThat(version1.equals(version2))
				.as("Testing the method equals() with different versions")
				.isFalse();
		assertThat(version2.equals(version1))
				.as("Testing the method equals() with different versions")
				.isFalse();

		/**
		 * Test the method equals() with different versions
		 *
		 * @osgi.spec Version.equals(Object)
		 */

		version1 = new Version(0, 0, 0);
		version2 = new Version(0, 1, 0);

		assertThat(version1.equals(version2))
				.as("Testing the method equals() with different versions")
				.isFalse();
		assertThat(version2.equals(version1))
				.as("Testing the method equals() with different versions")
				.isFalse();

		/**
		 * Test the method equals() with different versions
		 *
		 * @osgi.spec Version.equals(Object)
		 */

		version1 = new Version(0, 0, 0);
		version2 = new Version(0, 0, 1);

		assertThat(version1.equals(version2))
				.as("Testing the method equals() with different versions")
				.isFalse();
		assertThat(version2.equals(version1))
				.as("Testing the method equals() with different versions")
				.isFalse();

		/**
		 * Test the method equals() with different versions
		 *
		 * @osgi.spec Version.equals(Object)
		 */

		version1 = new Version(0, 0, 0, "a");
		version2 = new Version(0, 0, 0, "b");

		assertThat(version1.equals(version2))
				.as("Testing the method equals() with different versions")
				.isFalse();
		assertThat(version2.equals(version1))
				.as("Testing the method equals() with different versions")
				.isFalse();

		/**
		 * Test the method equals() with different versions
		 *
		 * @osgi.spec Version.equals(Object)
		 */

		version1 = new Version(0, 0, 0, "a");
		version2 = new Version(1, 1, 1, "b");

		assertThat(version1.equals(version2))
				.as("Testing the method equals() with different versions")
				.isFalse();
		assertThat(version2.equals(version1))
				.as("Testing the method equals() with different versions")
				.isFalse();

		version1 = new Version("0.0.0.a");
		version2 = Version.valueOf(" 0.0.0.a  ");
		assertThat(version2)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version1);
		assertThat(version1)
				.as("Testing the method equals() with the same versions")
				.isEqualTo(version2);

		version1 = new Version("0.0.0.a");
		version2 = null;
		assertThat(version1.equals(version2))
				.as("Testing the method equals() with null")
				.isFalse();
		assertThat(version1.equals(this))
				.as("Testing the method equals() with null")
				.isFalse();

	}

	/**
	 * Test the method hashCode() when the equals() returns true
	 *
	 * @osgi.spec Version.hashCode();
	 */
	@Test
	public void testHashCode() throws Exception {
		Version version1;
		Version version2;

		version1 = new Version(0, 0, 0, "a");
		version2 = new Version(0, 0, 0, "a");

		assertThat(version2.hashCode())
				.as("The method hashCode() has different return values when the method equals() returns true for two Version instances")
				.isEqualTo(version1.hashCode());

		version1 = new Version(0, 0, 0);
		version2 = new Version(1, 0, 0);

		assertThat(version1.hashCode() == version2.hashCode())
				.as("The method hashCode() has the same return value when the method equals() returns false for two Version instances")
				.isFalse();

	}

	@Test
	public void testGetMajor() {
		Version version;

		version = new Version(1, 2, 3);

		assertThat(version.getMajor())
				.as("Testing the method getMajor() using the constructor Version(int,int,int)").isOne();

		version = new Version("1.2.3");

		assertThat(version.getMajor())
				.as("Testing the method getMajor() using the constructor Version(String)").isOne();
	}

	@Test
	public void testGetMinor() {
		Version version;

		version = new Version(1, 2, 3);

		assertThat(version.getMinor())
				.as("Testing the method getMinor() using the constructor Version(int,int,int)")
				.isEqualTo(2);

		version = new Version("1.2.3");

		assertThat(version.getMinor())
				.as("Testing the method getMinor() using the constructor Version(String)")
				.isEqualTo(2);
	}

	@Test
	public void testGetMicro() {
		Version version;

		version = new Version(1, 2, 3);

		assertThat(version.getMicro())
				.as("Testing the method getMicro() using the constructor Version(int,int,int)")
				.isEqualTo(3);

		version = new Version("1.2.3");

		assertThat(version.getMicro())
				.as("Testing the method getMicro() using the constructor Version(String)")
				.isEqualTo(3);
	}

	/**
	 * Test the method getQualifier() using the constructor
	 * Version(int,int,int,String)
	 *
	 * @osgi.spec Version.getQualifier()
	 */
	@Test
	public void testGetQualifier() throws Exception {
		Version version;

		version = new Version(1, 1, 1, "a");

		assertThat(version.getQualifier())
				.as("Testing the method getQualifier() using the constructor Version(int,int,int,String)")
				.isEqualTo("a");

		version = new Version(1, 1, 1, null);

		assertThat(version.getQualifier())
				.as("Testing the method getQualifier() using the constructor Version(int,int,int,String)").isEmpty();

		version = new Version("1.1.1.a");

		assertThat(version.getQualifier())
				.as("Testing the method getQualifier() using the constructor Version(String)")
				.isEqualTo("a");

		version = new Version("1.1.1");

		assertThat(version.getQualifier())
				.as("Testing the method getQualifier() using the constructor Version(String)").isEmpty();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testCompareTo() {
		/**
		 * Test the method compareTo() with first version number less than
		 * second version number
		 *
		 * @osgi.spec Version.compareTo(Version);
		 */
		Version version1;
		Version version2;

		version1 = new Version(1, 1, 1);
		version2 = new Version(2, 1, 1);

		assertThat(version1.compareTo(version2) < 0)
				.as("Testing the method compareTo() with first version number less than second version number")
				.isTrue();
		assertThat(version2.compareTo(version1) > 0)
				.as("Testing the method compareTo() with first version number less than second version number")
				.isTrue();

		/**
		 * Test the method compareTo() with first version number greater than
		 * second version number
		 *
		 * @osgi.spec Version.compareTo(Version);
		 */

		version1 = new Version(2, 1, 1);
		version2 = new Version(1, 1, 1);

		assertThat(version1.compareTo(version2) > 0)
				.as("Testing the method compareTo() with first version number greater than second version number")
				.isTrue();
		assertThat(version2.compareTo(version1) < 0)
				.as("Testing the method compareTo() with first version number greater than second version number")
				.isTrue();

		/**
		 * Test the method compareTo() with same version numbers
		 *
		 * @osgi.spec Version.compareTo(Version);
		 */

		version1 = new Version(1, 1, 1);
		version2 = version1;

		assertThat(version1.compareTo(version2) == 0)
				.as("Testing the method compareTo() with same version numbers")
				.isTrue();
		assertThat(version2.compareTo(version1) == 0)
				.as("Testing the method compareTo() with same version numbers")
				.isTrue();

		version1 = new Version(1, 1, 1);
		version2 = new Version(1, 1, 1);

		assertThat(version1.compareTo(version2) == 0)
				.as("Testing the method compareTo() with same version numbers")
				.isTrue();
		assertThat(version2.compareTo(version1) == 0)
				.as("Testing the method compareTo() with same version numbers")
				.isTrue();

		version1 = new Version(1, 1, 1, "a");
		version2 = new Version(1, 1, 1, "a");

		assertThat(version1.compareTo(version2) == 0)
				.as("Testing the method compareTo() with same version numbers")
				.isTrue();
		assertThat(version2.compareTo(version1) == 0)
				.as("Testing the method compareTo() with same version numbers")
				.isTrue();

		version1 = new Version(1, 1, 1, "");
		version2 = new Version(1, 1, 1, "b");

		assertThat(version1.compareTo(version2) < 0)
				.as("Testing the method compareTo() with different qualifiers")
				.isTrue();
		assertThat(version2.compareTo(version1) > 0)
				.as("Testing the method compareTo() with different qualifiers")
				.isTrue();

		version1 = new Version(1, 1, 1, null);
		version2 = new Version(1, 1, 1, "a");

		assertThat(version1.compareTo(version2) < 0)
				.as("Testing the method compareTo() with different qualifiers")
				.isTrue();
		assertThat(version2.compareTo(version1) > 0)
				.as("Testing the method compareTo() with different qualifiers")
				.isTrue();

		/**
		 * Test the method compareTo() with an incorrect object
		 *
		 * @osgi.spec Version.compareTo(Version);
		 */
		Object incorrect = "";
		@SuppressWarnings("rawtypes")
		Comparable lastVersion = new Version(1, 1, 1);
		assertThatExceptionOfType(ClassCastException.class)
				.as("Testing the method compareTo() with an incorrect object")
				.isThrownBy(() -> lastVersion.compareTo(incorrect));
	}

	@Test
	public void testConstantsValues() {
		assertThat(Version.emptyVersion)
				.as("emptyVersion not equal to 0.0.0")
				.isEqualTo(new Version(0, 0, 0));
		assertThat(Version.emptyVersion.hashCode())
				.as("emptyVersion has different hashCode than 0.0.0")
				.isEqualTo(new Version(0, 0, 0).hashCode());
		assertThat(Version.emptyVersion)
				.as("not emptyVersion")
				.isSameAs(Version.parseVersion(null));
		assertThat(Version.emptyVersion)
				.as("not emptyVersion")
				.isSameAs(Version.parseVersion(""));
		assertThat(Version.emptyVersion)
				.as("not emptyVersion")
				.isSameAs(Version.parseVersion("	"));
		assertThat(Version.emptyVersion)
				.as("not emptyVersion")
				.isSameAs(Version.valueOf(""));
		assertThat(Version.emptyVersion)
				.as("not emptyVersion")
				.isSameAs(Version.valueOf("	"));
	}

	@Test
	public void testToString() {
		Version version;

		version = new Version(1, 2, 3);
		assertThat(version.toString())
				.as("Wrong toString result")
				.isEqualTo("1.2.3");

		version = new Version(1, 2, 3, "a");
		assertThat(version.toString())
				.as("Wrong toString result")
				.isEqualTo("1.2.3.a");

		version = new Version(1, 2, 3, null);
		assertThat(version.toString())
				.as("Wrong toString result")
				.isEqualTo("1.2.3");

		version = new Version("1.2.3");
		assertThat(version.toString())
				.as("Wrong toString result")
				.isEqualTo("1.2.3");

		version = new Version("1.2.3.a");
		assertThat(version.toString())
				.as("Wrong toString result")
				.isEqualTo("1.2.3.a");
	}
}
