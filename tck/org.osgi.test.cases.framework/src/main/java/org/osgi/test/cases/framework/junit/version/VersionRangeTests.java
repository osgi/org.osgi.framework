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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Tests for the VersionRange class.
 */
public class VersionRangeTests {
	@Test
	public void testConstructors() {
		Version version1 = new Version(1, 2, 3);
		Version version2 = new Version(2, 0, 0);
		new VersionRange('[', version1, version2, ')');
		new VersionRange('[', version1, version2, ']');
		new VersionRange('(', version1, version2, ')');
		new VersionRange('(', version1, version2, ']');

		new VersionRange('[', version1, null, ')');

		new VersionRange("[1,2)");
		new VersionRange("[1.2.3,2.0.0)");
		new VersionRange("(1.2.3,2.0.0]");
		new VersionRange(" [ 1.2.3 , 2.0.0 ) ");
		new VersionRange(" ( 1.2.3 , 2.0.0 ] ");
		new VersionRange("1.2.3");
		new VersionRange(" 1.2.3 ");
		VersionRange.valueOf("[1,2)");
		VersionRange.valueOf("[1.2.3,2.0.0)");
		VersionRange.valueOf("(1.2.3,2.0.0]");
		VersionRange.valueOf(" [ 1.2.3 , 2.0.0 ) ");
		VersionRange.valueOf(" ( 1.2.3 , 2.0.0 ] ");
		VersionRange.valueOf("1.2.3");
		VersionRange.valueOf(" 1.2.3 ");
	}

	@Test
	public void testConstructorsBadArguments() {
		Version version1 = new Version(1, 2, 3);
		Version version2 = new Version(2, 0, 0);
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> new VersionRange('x', version1, version2, ')'));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> new VersionRange('[', version1, version2, 'x'));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> new VersionRange('[', null, version2, ')'));

		assertThatExceptionOfType(RuntimeException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> new VersionRange(null));
		assertThatExceptionOfType(RuntimeException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> VersionRange.valueOf(null));
		testConstructorsBadArguments("");
		testConstructorsBadArguments("x");
		testConstructorsBadArguments(" 1.2.3 x");
		testConstructorsBadArguments(" 1.2.3 [");
		testConstructorsBadArguments(" 1.2.3 ( ");
		testConstructorsBadArguments(" x 1.2.3 ");
		testConstructorsBadArguments("[");
		testConstructorsBadArguments("(1");
		testConstructorsBadArguments("[1,");
		testConstructorsBadArguments("[1,2");
		testConstructorsBadArguments("[1,2x");
		testConstructorsBadArguments("	[1,2)	x");
		testConstructorsBadArguments("x	[1,2)");
		testConstructorsBadArguments("	[1,2)	)");
		testConstructorsBadArguments("	[1,,2)	");
		testConstructorsBadArguments("	[1,2))	");
		testConstructorsBadArguments("[[1,2)");
		testConstructorsBadArguments("	[,2)	");
		testConstructorsBadArguments("[2)");
	}

	@Test
	private void testConstructorsBadArguments(String arg) {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> new VersionRange(arg));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("VersionRange created with illegal arguments")
				.isThrownBy(() -> VersionRange.valueOf(arg));

	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEquals() {
		Version version1 = new Version(1, 2, 3);
		Version version2 = new Version(2, 4, 6);
		VersionRange range1;
		VersionRange range2;

		range1 = new VersionRange('[', version1, version2, ')');
		range2 = new VersionRange("[1.2.3,2.4.6)");
		assertThat(range1)
				.as("equal objects")
				.isEqualTo(range1);
		assertThat(range2)
				.as("equal objects")
				.isEqualTo(range2);
		assertThat(range2)
				.as("equal objects")
				.isEqualTo(range1);
		assertThat(range1)
				.as("equal objects")
				.isEqualTo(range2);

		range1 = new VersionRange('[', version1, version2, ')');
		range2 = new VersionRange('[', version1, version2, ')');
		assertThat(range2)
				.as("equal objects")
				.isEqualTo(range1);
		assertThat(range1)
				.as("equal objects")
				.isEqualTo(range2);

		range1 = new VersionRange('[', version1, version2, ')');
		range2 = new VersionRange('[', version1, version2, ']');
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();
		range2 = new VersionRange("[1.2.3,2.4.6]");
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();

		range1 = new VersionRange('(', version1, version2, ']');
		range2 = new VersionRange('[', version1, version2, ']');
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();
		range2 = new VersionRange("[1.2.3,2.4.6]");
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();

		range1 = new VersionRange('[', version1, version2, ']');
		range2 = new VersionRange('[', version1, version1, ']');
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();

		range1 = new VersionRange('[', version1, version1, ')');
		range2 = new VersionRange('[', version1, version1, ']');
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();

		// both are empty; so equal
		range1 = new VersionRange('[', version2, version1, ')');
		range2 = new VersionRange('[', version1, version1, ')');
		assertThat(range2)
				.as("equal objects")
				.isEqualTo(range1);
		assertThat(range1)
				.as("equal objects")
				.isEqualTo(range2);

		range1 = new VersionRange('[', version1, null, ')');
		range2 = new VersionRange('[', version1, null, ']');
		assertThat(range2)
				.as("equal objects")
				.isEqualTo(range1);
		assertThat(range1)
				.as("equal objects")
				.isEqualTo(range2);

		range1 = new VersionRange('[', version1, version2, ')');
		range2 = new VersionRange('[', version1, null, ')');
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();

		range1 = new VersionRange('[', version1, null, ')');
		range2 = new VersionRange('[', version1, version2, ')');
		assertThat(range1.equals(range2))
				.as("unequal objects")
				.isFalse();
		assertThat(range2.equals(range1))
				.as("unequal objects")
				.isFalse();

		range1 = new VersionRange('[', version1, version2, ')');
		assertThat(range1.equals(null))
				.as("unequal objects")
				.isFalse();
		assertThat(range1.equals(this))
				.as("unequal objects")
				.isFalse();

	}

	@Test
	public void testHashCode() throws Exception {
		Version version1 = new Version(1, 2, 3);
		Version version2 = new Version(2, 4, 6);
		VersionRange range1;
		VersionRange range2;

		range1 = new VersionRange('[', version1, version2, ')');
		range2 = new VersionRange('[', version1, version2, ')');
		assertThat(range2.hashCode())
				.as("equal objects have different hashCode")
				.isEqualTo(range1.hashCode());

		range1 = new VersionRange('[', version1, version2, ')');
		range2 = new VersionRange('[', version1, version2, ']');
		assertThat(range1.hashCode() == range2.hashCode())
				.as("unequal objects have same hashCode")
				.isFalse();

		range1 = new VersionRange('(', version1, version2, ']');
		range2 = new VersionRange('[', version1, version2, ']');
		assertThat(range1.hashCode() == range2.hashCode())
				.as("unequal objects have same hashCode")
				.isFalse();

		range1 = new VersionRange('[', version1, version2, ']');
		range2 = new VersionRange('[', version1, version1, ']');
		assertThat(range1.hashCode() == range2.hashCode())
				.as("unequal objects have same hashCode")
				.isFalse();

		range1 = new VersionRange('[', version1, version1, ')');
		range2 = new VersionRange('[', version1, version1, ']');
		assertThat(range1.hashCode() == range2.hashCode())
				.as("unequal objects have same hashCode")
				.isFalse();

		// both are empty; so same hashCode
		range1 = new VersionRange('[', version2, version1, ')');
		range2 = new VersionRange('[', version1, version1, ')');
		assertThat(range2.hashCode())
				.as("equal objects have different hashCode")
				.isEqualTo(range1.hashCode());

		range1 = new VersionRange('[', version1, null, ')');
		range2 = new VersionRange('[', version1, null, ']');
		assertThat(range2.hashCode())
				.as("equal objects have different hashCode")
				.isEqualTo(range1.hashCode());

	}

	@Test
	public void testGetLeftType() {
		Version version11 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7);
		VersionRange range;

		range = new VersionRange('[', version11, version21, ')');
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_CLOSED);

		range = new VersionRange('[', version11, version21, ']');
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_CLOSED);

		range = new VersionRange('(', version11, version21, ')');
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_OPEN);

		range = new VersionRange('(', version11, version21, ']');
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_OPEN);

		range = new VersionRange('[', version11, null, ')');
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_CLOSED);

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_CLOSED);

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_CLOSED);

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_OPEN);

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_OPEN);

		range = new VersionRange("2.3.4");
		assertThat(range.getLeftType())
				.as("Wrong type")
				.isEqualTo(VersionRange.LEFT_CLOSED);

	}

	@Test
	public void testGetLeft() {
		Version version11 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7);
		VersionRange range;

		range = new VersionRange('[', version11, version21, ')');
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange('[', version11, version21, ']');
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange('(', version11, version21, ')');
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange('(', version11, version21, ']');
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange('[', version11, null, ')');
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

		range = new VersionRange("2.3.4");
		assertThat(range.getLeft())
				.as("Wrong version")
				.isEqualTo(version11);

	}

	@Test
	public void testGetRight() {
		Version version11 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7);
		VersionRange range;

		range = new VersionRange('[', version11, version21, ')');
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange('[', version11, version21, ']');
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange('(', version11, version21, ')');
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange('(', version11, version21, ']');
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange('[', version11, null, ')');
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(null);

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(version21);

		range = new VersionRange("2.3.4");
		assertThat(range.getRight())
				.as("Wrong version")
				.isEqualTo(null);
	}

	@Test
	public void testGetRightType() throws Exception {
		Version version11 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7);
		VersionRange range;

		range = new VersionRange('[', version11, version21, ')');
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_OPEN);

		range = new VersionRange('[', version11, version21, ']');
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_CLOSED);

		range = new VersionRange('(', version11, version21, ')');
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_OPEN);

		range = new VersionRange('(', version11, version21, ']');
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_CLOSED);

		range = new VersionRange('[', version11, null, ')');
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_OPEN);

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_OPEN);

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_CLOSED);

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_OPEN);

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_CLOSED);

		range = new VersionRange("2.3.4");
		assertThat(range.getRightType())
				.as("Wrong type")
				.isEqualTo(VersionRange.RIGHT_OPEN);

	}

	@Test
	public void testIsEmpty() {
		Version version1 = new Version(1, 0, 0);
		Version version2 = new Version(1, 0, 0, "-");
		VersionRange range;

		range = new VersionRange('[', version1, version2, ')');
		assertThat(range.isEmpty())
				.as("range is not empty")
				.isFalse();

		range = new VersionRange('(', version1, version2, ']');
		assertThat(range.isEmpty())
				.as("range is not empty")
				.isFalse();

		range = new VersionRange('[', version1, version1, ']');
		assertThat(range.isEmpty())
				.as("range is not empty")
				.isFalse();

		range = new VersionRange('[', version1, version1, ')');
		assertThat(range.isEmpty())
				.as("range is empty")
				.isTrue();

		range = new VersionRange('(', version1, version1, ']');
		assertThat(range.isEmpty())
				.as("range is empty")
				.isTrue();

		range = new VersionRange('(', version1, version1, ')');
		assertThat(range.isEmpty())
				.as("range is empty")
				.isTrue();

		range = new VersionRange('[', version2, version1, ']');
		assertThat(range.isEmpty())
				.as("range is empty")
				.isTrue();

		range = new VersionRange('[', version2, null, ')');
		assertThat(range.isEmpty())
				.as("range is not empty")
				.isFalse();
	}

	@Test
	public void testIsExact() {
		Version version1 = new Version(1, 0, 0);
		Version version2 = new Version(1, 0, 0, "-");
		Version version3 = new Version(1, 0, 0, "--");
		VersionRange range;

		range = new VersionRange('[', version1, version1, ']');
		assertThat(range.isExact())
				.as("range is exact")
				.isTrue();

		range = new VersionRange('[', version1, version1, ')');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('(', version1, version1, ']');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('(', version1, version1, ')');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('[', version1, version2, ']');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('[', version2, version1, ']');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('[', version2, null, ')');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('[', version2, null, ']');
		assertThat(range.isExact())
				.as("range is not exact")
				.isFalse();

		range = new VersionRange('[', version1, version2, ')');
		assertThat(range.isExact())
				.as("range is exact")
				.isTrue();

		range = new VersionRange('(', version1, version2, ']');
		assertThat(range.isExact())
				.as("range is exact")
				.isTrue();

		range = new VersionRange('(', version1, version3, ')');
		assertThat(range.isExact())
				.as("range is exact")
				.isTrue();
	}

	@Test
	public void testToString() {
		Version version11 = new Version(2, 3, 4, "-");
		Version version12 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7, "-");
		Version version22 = new Version(5, 6, 7);
		VersionRange range;

		range = new VersionRange('[', version11, version21, ')');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("[2.3.4.-,5.6.7.-)");

		range = new VersionRange('[', version12, version22, ')');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("[2.3.4,5.6.7)");

		range = new VersionRange('[', version11, version21, ']');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("[2.3.4.-,5.6.7.-]");

		range = new VersionRange('[', version12, version22, ']');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("[2.3.4,5.6.7]");

		range = new VersionRange('(', version11, version21, ')');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("(2.3.4.-,5.6.7.-)");

		range = new VersionRange('(', version12, version22, ')');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("(2.3.4,5.6.7)");

		range = new VersionRange('(', version11, version21, ']');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("(2.3.4.-,5.6.7.-]");

		range = new VersionRange('(', version12, version22, ']');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("(2.3.4,5.6.7]");

		range = new VersionRange('[', version11, null, ')');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("2.3.4.-");

		range = new VersionRange('[', version12, null, ')');
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("2.3.4");

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("[2.3.4,5.6.7)");

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("[2.3.4,5.6.7]");

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("(2.3.4,5.6.7)");

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("(2.3.4,5.6.7]");

		range = new VersionRange("2.3.4");
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("2.3.4");

		range = new VersionRange("2.3.4.-");
		assertThat(range.toString())
				.as("Wrong toString result")
				.isEqualTo("2.3.4.-");
	}

	@Test
	public void testIncludes() {
		Version version11 = new Version(2, 3, 4, "-");
		Version version12 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7, "-");
		Version version22 = new Version(5, 6, 7);
		Version version3 = new Version(3, 4, 5);
		Version version4 = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE,
				Integer.MAX_VALUE);
		VersionRange range;

		range = new VersionRange('[', version11, version22, ']');
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange('[', version11, version22, ')');
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("included")
				.isFalse();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange('(', version11, version22, ']');
		assertThat(range.includes(version11))
				.as("included")
				.isFalse();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange('(', version11, version22, ')');
		assertThat(range.includes(version11))
				.as("included")
				.isFalse();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("included")
				.isFalse();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("included")
				.isFalse();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("included")
				.isFalse();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange("[2.3.4.-,5.6.7.-)");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("included")
				.isFalse();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange("(2.3.4.-,5.6.7.-]");
		assertThat(range.includes(version11))
				.as("included")
				.isFalse();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("included")
				.isFalse();

		range = new VersionRange('[', version11, null, ')');
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("not included")
				.isTrue();

		range = new VersionRange('(', version12, null, ')');
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("not included")
				.isTrue();

		range = new VersionRange('[', version12, null, ')');
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("not included")
				.isTrue();

		range = new VersionRange("2.3.4");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("not included")
				.isTrue();

		range = new VersionRange("2.3.4.-");
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("included")
				.isFalse();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("not included")
				.isTrue();

		range = new VersionRange('[', Version.emptyVersion, null, ')');
		assertThat(range.includes(version11))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version12))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version21))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version22))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version3))
				.as("not included")
				.isTrue();
		assertThat(range.includes(version4))
				.as("not included")
				.isTrue();
		assertThat(range.includes(Version.emptyVersion))
				.as("not included")
				.isTrue();

		VersionRange lastRange = range; // range is reassigned above, the lambda needs a final copy
		assertThatExceptionOfType(RuntimeException.class)
				.as("includes called with illegal argument")
				.isThrownBy(() -> lastRange.includes(null));
	}

	@Test
	public void testFilterString() throws Exception {
		Version version11 = new Version(2, 3, 4, "-");
		Version version12 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7, "-");
		Version version22 = new Version(5, 6, 7);
		Version version3 = new Version(3, 4, 5);
		Version version4 = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE,
				Integer.MAX_VALUE);
		VersionRange range;

		range = new VersionRange('[', version11, version22, ']');
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('[', version11, version22, ')');
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('(', version11, version22, ']');
		assertThat(includesByFilter(range, version11))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('(', version11, version22, ')');
		assertThat(includesByFilter(range, version11))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("[2.3.4,5.6.7)");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("[2.3.4,5.6.7]");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("(2.3.4,5.6.7]");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("(2.3.4,5.6.7)");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("[2.3.4.-,5.6.7.-)");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("(2.3.4.-,5.6.7.-]");
		assertThat(includesByFilter(range, version11))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('[', version11, null, ')');
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('(', version12, null, ')');
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('[', version12, null, ')');
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("2.3.4");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange("2.3.4.-");
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("included")
				.isFalse();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

		range = new VersionRange('[', Version.emptyVersion, null, ')');
		assertThat(includesByFilter(range, version11))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version12))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version21))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version22))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version3))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, version4))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, Version.emptyVersion))
				.as("not included")
				.isTrue();
		assertThat(includesByFilter(range, null))
				.as("included")
				.isFalse();

	}

	private boolean includesByFilter(VersionRange range, Version version)
			throws Exception {
		String attributeName = "includesByFilter";
		String filterString = range.toFilterString(attributeName);
		// System.out.println(range + " => " + filterString);
		Filter filter = FrameworkUtil.createFilter(filterString);
		Map<String, Version> map = new HashMap<String, Version>();
		if (version != null) {
			map.put(attributeName, version);
		}
		return filter.matches(map);
	}

	@Test
	public void testFilterStringBadArguments() {
		VersionRange range;

		range = new VersionRange('[', Version.emptyVersion,
				Version.emptyVersion, ']');

		// Bad characters '=', '>', '<', '~', '(' or ')'
		assertThatExceptionOfType(RuntimeException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString(null));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString(""));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString("b~d"));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString("ba)"));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString("(ad"));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString("b=d"));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString("<ad"));

		assertThatExceptionOfType(IllegalArgumentException.class)
				.as("filter string created with illegal arguments")
				.isThrownBy(() -> range.toFilterString("ba>"));

	}

	@Test
	public void testIntersection() {
		Version version11 = new Version(2, 3, 4, "-");
		Version version12 = new Version(2, 3, 4);
		Version version21 = new Version(5, 6, 7, "-");
		Version version22 = new Version(5, 6, 7);
		Version version3 = new Version(3, 4, 5);
		VersionRange range1;
		VersionRange range2;
		VersionRange range3;
		VersionRange expected;

		range1 = new VersionRange('[', version11, version22, ']');
		expected = range1;
		assertThat(range1.intersection((VersionRange[]) null))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ']');
		expected = range1;
		assertThat(range1.intersection())
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ']');
		range2 = range1;
		expected = range1;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ')');
		range2 = range1;
		expected = range1;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('(', version11, version22, ']');
		range2 = range1;
		expected = range1;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('(', version11, version22, ')');
		range2 = range1;
		expected = range1;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ']');
		range2 = new VersionRange('[', version11, version22, ')');
		expected = range2;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ']');
		range2 = new VersionRange('(', version11, version22, ']');
		expected = range2;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ']');
		range2 = new VersionRange('(', version11, version22, ')');
		expected = range2;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version11, version22, ']');
		range2 = new VersionRange('[', version12, version21, ']');
		expected = range1;
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version12, version22, ']');
		range2 = new VersionRange('[', version11, version21, ']');
		expected = new VersionRange('[', version11, version22, ']');
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version12, version22, ')');
		range2 = new VersionRange('[', version11, null, ')');
		expected = new VersionRange('[', version11, version22, ')');
		assertThat(range1.intersection(range2))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version22, version12, ')');
		range2 = new VersionRange('[', version11, null, ')');
		assertThat(range1.isEmpty())
				.as("range is not empty")
				.isTrue();
		assertThat(range1.intersection(range2).isEmpty())
				.as("range is not empty")
				.isTrue();
		assertThat(range2.intersection(range1).isEmpty())
				.as("range is not empty")
				.isTrue();

		range1 = new VersionRange('[', version22, version12, ')');
		range2 = new VersionRange('[', version11, version21, ')');
		range3 = new VersionRange('(', version12, version3, ']');
		assertThat(range1.isEmpty())
				.as("range is not empty")
				.isTrue();
		assertThat(range1.intersection(range2, range3)
				.isEmpty())
				.as("range is not empty")
				.isTrue();
		assertThat(range2.intersection(range1, range3)
				.isEmpty())
				.as("range is not empty")
				.isTrue();
		assertThat(range3.intersection(range1, range2)
				.isEmpty())
				.as("range is not empty")
				.isTrue();

		range1 = new VersionRange('[', version12, version22, ')');
		range2 = new VersionRange('[', version11, version21, ')');
		range3 = new VersionRange('(', version12, version3, ']');
		expected = new VersionRange('[', version11, version3, ']');
		assertThat(range1.intersection(range2, range3))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range2.intersection(range1, range3))
				.as("wrong intersection")
				.isEqualTo(expected);
		assertThat(range3.intersection(range1, range2))
				.as("wrong intersection")
				.isEqualTo(expected);

		range1 = new VersionRange('[', version12, version11, ']');
		range2 = new VersionRange('[', version22, version21, ']');
		assertThat(range1.isEmpty())
				.as("range is empty")
				.isFalse();
		assertThat(range2.isEmpty())
				.as("range is empty")
				.isFalse();
		assertThat(range1.intersection(range2).isEmpty())
				.as("range is not empty")
				.isTrue();
		assertThat(range2.intersection(range1).isEmpty())
				.as("range is not empty")
				.isTrue();

	}
}
