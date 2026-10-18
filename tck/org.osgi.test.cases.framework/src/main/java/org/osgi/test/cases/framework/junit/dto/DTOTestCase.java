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

package org.osgi.test.cases.framework.junit.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.dto.DTO;
import org.osgi.framework.Version;

public class DTOTestCase {
	private TestDTO dto;

	@BeforeEach
	public void setUp() {
		dto = new TestDTO();
		dto.stringField = "stringValue";
		dto.versionField = Version.valueOf("1.2.3.qual");
		dto.enumField = TestEnum.TWO;
	}

	@Test
	public void testDTOtoString() throws Exception {
        String result = dto.toString();
		System.out.println(result);
		assertThat(result.indexOf("stringField") >= 0)
				.as("toString does not include stringField name")
				.isTrue();
		assertThat(result.indexOf("\"stringValue\"") >= 0)
				.as("toString does not include stringField value")
				.isTrue();
		assertThat(result.indexOf("versionField") >= 0)
				.as("toString does not include versionField name")
				.isTrue();
		assertThat(result.indexOf("\"1.2.3.qual\"") >= 0)
				.as("toString does not include versionField value")
				.isTrue();
		assertThat(result.indexOf("enumField") >= 0)
				.as("toString does not include enumField name")
				.isTrue();
		assertThat(result.indexOf("\"TWO\"") >= 0)
				.as("toString does not include enumField value")
				.isTrue();
	}

	@Test
	public void testDTOEnum() throws Exception {
		for (Field field : dto.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Class< ? > c = field.getType();
			if (!Enum.class.isAssignableFrom(c)) {
				continue;
			}
			System.out.printf("%s is an enum of type %s\n", field.getName(), c);
			Object o = field.get(dto);
			Method m = c.getMethod("valueOf", String.class);
			assertThat(m.invoke(null, ((Enum< ? >) o).name())).isSameAs(o);
		}
    }

	@Test
	public void testDTOVersion() throws Exception {
		for (Field field : dto.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			Class< ? > c = field.getType();
			if (!"org.osgi.framework.Version".equals(c.getName())) {
				continue;
			}
			System.out.printf("%s is a version of type %s\n", field.getName(),
					c);
			Object o = field.get(dto);
			Method m = c.getMethod("valueOf", String.class);
			assertThat(m.invoke(null, o.toString())).isEqualTo(o);
		}
	}

    public enum TestEnum {
		ONE, TWO, THREE
	}
    public static class TestDTO extends DTO {
		public String	stringField;
		public Version	versionField;
		public TestEnum	enumField;
    }
}
