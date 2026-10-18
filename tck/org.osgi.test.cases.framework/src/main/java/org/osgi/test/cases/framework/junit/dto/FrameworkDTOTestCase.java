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
import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.framework.wiring.dto.BundleWireDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO;
import org.osgi.framework.wiring.dto.FrameworkWiringDTO;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.CapabilityRefDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.resource.dto.RequirementRefDTO;
import org.osgi.resource.dto.WireDTO;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;
import org.osgi.test.common.wiring.FrameworkWirings;

public class FrameworkDTOTestCase {

	@InjectBundleContext
	BundleContext bundleContext;

	@InjectBundleInstaller
	BundleInstaller bundleInstaller;

	private String testName;

	/** The name of the running test method, as TestCase.getName() gave it. */
	private String getName() {
		return testName;
	}

	/** InputStream to an entry within the test bundle. */
	private InputStream entryStream(String entry) throws IOException {
		URL url = bundleContext.getBundle().getEntry(entry);
		assertThat(url)
				.as("Can not find resource: " + entry)
				.isNotNull();
		return url.openStream();
	}

    private Bundle        tb1;
	private Bundle							tb2;
	private Set<BundleRevisionDTO>			resources;
	private Set<BundleRevisionDTO>			resourcesUntested;
	private Set<BundleWiringDTO.NodeDTO>	nodes;
	private Set<BundleWiringDTO.NodeDTO>	nodesUntested;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		testName = testInfo.getTestMethod().get().getName();
		resources = null;
		resourcesUntested = null;
		nodes = null;
		nodesUntested = null;
        tb1 = bundleInstaller.installBundle("dto.tb1.jar", false);
		tb2 = bundleInstaller.installBundle("dto.tb2.jar", false);
    }

	@AfterEach
	public void tearDown() throws Exception {
        if ((tb1.getState() & Bundle.UNINSTALLED) != Bundle.UNINSTALLED) {
            tb1.uninstall();
        }
		if ((tb2.getState() & Bundle.UNINSTALLED) != Bundle.UNINSTALLED) {
			tb2.uninstall();
		}
		FrameworkWirings.refreshBundles(bundleContext, tb1, tb2);
    }

	@Test
	public void testBundleDTO() throws Exception {
        BundleDTO dto = tb1.adapt(BundleDTO.class);
        assertBundleDTO(tb1, dto);
        tb1.start();
        assertThat(tb1.getState() == dto.state)
        		.as("Bundle state matches")
				.isFalse();
        dto = tb1.adapt(BundleDTO.class);
        assertBundleDTO(tb1, dto);
    }

    private void assertBundleDTO(Bundle bundle, BundleDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
        assertThat(dto.id)
        		.as("Bundle id does not match")
				.isEqualTo(bundle.getBundleId());
        assertThat(dto.lastModified)
        		.as("Bundle lastModified does not match")
				.isEqualTo(bundle.getLastModified());
        assertThat(dto.state)
        		.as("Bundle state does not match")
				.isEqualTo(bundle.getState());
        assertThat(dto.symbolicName)
        		.as("Bundle symbolicName does not match")
				.isEqualTo(bundle.getSymbolicName());
		assertThat(Version.valueOf(dto.version))
				.as("Bundle versionField does not match")
				.isEqualTo(bundle.getVersion());
    }

	@Test
	public void testBundleStartLevelDTO() throws Exception {
        BundleStartLevel bsl = tb1.adapt(BundleStartLevel.class);
        BundleStartLevelDTO dto = tb1.adapt(BundleStartLevelDTO.class);
		assertBundleStartLevelDTO(tb1, bsl, dto);
        bsl.setStartLevel(10);
        tb1.start(Bundle.START_ACTIVATION_POLICY);
        assertThat(bsl.getStartLevel() == dto.startLevel)
        		.as("Bundle startLevel matches")
				.isFalse();
        assertThat(bsl.isActivationPolicyUsed() == dto.activationPolicyUsed)
        		.as("Bundle activationPolicyUsed matches")
				.isFalse();
        assertThat(bsl.isPersistentlyStarted() == dto.persistentlyStarted)
        		.as("Bundle persistentlyStarted matches")
				.isFalse();
        dto = tb1.adapt(BundleStartLevelDTO.class);
		assertBundleStartLevelDTO(tb1, bsl, dto);
    }

	private void assertBundleStartLevelDTO(Bundle b, BundleStartLevel bsl, BundleStartLevelDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		assertThat(dto.bundle)
				.as("Bundle id does not match")
				.isEqualTo(b.getBundleId());
        assertThat(dto.startLevel)
        		.as("Bundle startLevel does not match")
				.isEqualTo(bsl.getStartLevel());
        assertThat(dto.activationPolicyUsed)
        		.as("Bundle activationPolicyUsed does not match")
				.isEqualTo(bsl.isActivationPolicyUsed());
        assertThat(dto.persistentlyStarted)
        		.as("Bundle persistentlyStarted does not match")
				.isEqualTo(bsl.isPersistentlyStarted());
    }

	@Test
	public void testBundleRevisionDTO() throws Exception {
        tb1.start();
        BundleRevision revision = tb1.adapt(BundleRevision.class);
        BundleRevisionDTO dto = tb1.adapt(BundleRevisionDTO.class);
		assertTopBundleRevisionDTO(revision, dto);
		tb1.uninstall();
		assertThat(tb1).isInState(Bundle.UNINSTALLED);
		dto = tb1.adapt(BundleRevisionDTO.class);
		assertThat(dto)
				.as("Current BundleRevisionDTO for uninstalled bundle is not null")
				.isNull();
	}

	private void assertTopBundleRevisionDTO(BundleRevision revision, BundleRevisionDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		resources = new HashSet<BundleRevisionDTO>();
		resources.add(dto);
		resources = Collections.unmodifiableSet(resources);
		resourcesUntested = new HashSet<BundleRevisionDTO>(resources);
		nodes = Collections.unmodifiableSet(new HashSet<BundleWiringDTO.NodeDTO>());
		nodesUntested = new HashSet<BundleWiringDTO.NodeDTO>(nodes);
        assertBundleRevisionDTO(revision, dto);
		assertThat(resourcesUntested)
				.as("Too many resources")
				.isEqualTo(Collections.EMPTY_SET);
		assertThat(nodesUntested)
				.as("Too many wiring nodes")
				.isEqualTo(Collections.EMPTY_SET);
    }

    private BundleRevisionDTO getBundleRevisionDTObyId(int id) {
		assertThat(resources)
				.as("resources not set")
				.isNotNull();
		for (BundleRevisionDTO dto : resources) {
			if (dto.id == id) {
				return dto;
			}
		}
		fail("resource id not found: " + id);
		return null;
	}

	private void assertBundleRevisionDTO(BundleRevision revision, BundleRevisionDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		if (!resourcesUntested.remove(dto)) {
			return;
		}
		assertThat(dto.bundle)
				.as("Bundle id does not match")
				.isEqualTo(revision.getBundle().getBundleId());
        assertThat(dto.symbolicName)
        		.as("BundleRevision symbolicName does not match")
				.isEqualTo(revision.getSymbolicName());
		assertThat(Version.valueOf(dto.version))
				.as("BundleRevision versionField does not match")
				.isEqualTo(revision.getVersion());
        assertThat(dto.type)
        		.as("BundleRevision type does not match")
				.isEqualTo(revision.getTypes());
		assertListCapabilityDTO(revision.getDeclaredCapabilities(null), dto.capabilities);
		assertListRequirementDTO(revision.getDeclaredRequirements(null), dto.requirements);
    }

	private void assertListCapabilityDTO(List<BundleCapability> caps, List<CapabilityDTO> dtos) throws Exception {
		assertThat(dtos)
        		.as("BundleCapability count does not match").hasSize(caps.size());
        for (int i = 0; i < caps.size(); i++) {
            BundleCapability cap = caps.get(i);
            CapabilityDTO dto = dtos.get(i);
			assertCapabilityDTO(cap, dto);
        }
    }

	private void assertCapabilityDTO(BundleCapability cap, CapabilityDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		assertBundleRevisionDTO(cap.getRevision(), getBundleRevisionDTObyId(dto.resource));
	    assertThat(dto.namespace)
	    		.as("BundleCapability namespace does not match")
				.isEqualTo(cap.getNamespace());
		assertAttributesDTO("BundleCapability attributes does not match", cap.getAttributes(), dto.attributes);
	    assertThat(dto.directives)
	    		.as("BundleCapability directives does not match")
				.isEqualTo(cap.getDirectives());
	}

	private void assertListCapabilityRefDTO(List<BundleCapability> caps, List<CapabilityRefDTO> dtos) throws Exception {
		assertThat(dtos.size())
				.as("BundleCapability count does not match")
				.isEqualTo(caps.size());
		for (int i = 0; i < caps.size(); i++) {
			BundleCapability cap = caps.get(i);
			CapabilityRefDTO dto = dtos.get(i);
			assertCapabilityRefDTO(cap, dto);
		}
	}

	private void assertCapabilityRefDTO(BundleCapability cap, CapabilityRefDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		BundleRevisionDTO revisionDTO = getBundleRevisionDTObyId(dto.resource);
		assertBundleRevisionDTO(cap.getRevision(), revisionDTO);
		for (CapabilityDTO capDTO : revisionDTO.capabilities) {
			if (capDTO.id == dto.capability) {
				assertCapabilityDTO(cap, capDTO);
				return;
			}
		}
		fail("capability id not found: " + dto.capability);
	}

	private void assertAttributesDTO(String message, Map<String, Object> attributes, Map<String, Object> dto) throws Exception {
		assertThat(dto.size())
				.as(message)
				.isEqualTo(attributes.size());
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Version) {
				value = String.valueOf(value);
			} else
				if (value instanceof List) {
					List<Object> newList = new ArrayList<Object>((List<?>) value);
					for (ListIterator<Object> iter = newList.listIterator(); iter.hasNext();) {
						Object element = iter.next();
						if (element instanceof Version) {
							iter.set(String.valueOf(element));
						}
					}
					value = newList;
				}
			assertThat(dto.get(entry.getKey()))
					.as(message)
				.isEqualTo(value);
		}
	}

	private void assertListRequirementDTO(List<BundleRequirement> reqs, List<RequirementDTO> dtos) throws Exception {
		assertThat(dtos)
        		.as("BundleRequirements count does not match").hasSize(reqs.size());
        for (int i = 0; i < reqs.size(); i++) {
            BundleRequirement req = reqs.get(i);
            RequirementDTO dto = dtos.get(i);
			assertRequirementDTO(req, dto);
        }
    }

	private void assertRequirementDTO(BundleRequirement req, RequirementDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		assertBundleRevisionDTO(req.getRevision(), getBundleRevisionDTObyId(dto.resource));
	    assertThat(dto.namespace)
	    		.as("BundleRequirement namespace does not match")
				.isEqualTo(req.getNamespace());
		assertAttributesDTO("BundleRequirement attributes does not match", req.getAttributes(), dto.attributes);
	    assertThat(dto.directives)
	    		.as("BundleRequirement directives does not match")
				.isEqualTo(req.getDirectives());
	}

	private void assertListRequirementRefDTO(List<BundleRequirement> reqs, List<RequirementRefDTO> dtos) throws Exception {
		assertThat(dtos.size())
				.as("BundleRequirements count does not match")
				.isEqualTo(reqs.size());
		for (int i = 0; i < reqs.size(); i++) {
			BundleRequirement req = reqs.get(i);
			RequirementRefDTO dto = dtos.get(i);
			assertRequirementRefDTO(req, dto);
		}
	}

	private void assertRequirementRefDTO(BundleRequirement req, RequirementRefDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		BundleRevisionDTO revisionDTO = getBundleRevisionDTObyId(dto.resource);
		assertBundleRevisionDTO(req.getRevision(), revisionDTO);
		for (RequirementDTO reqDTO : revisionDTO.requirements) {
			if (reqDTO.id == dto.requirement) {
				assertRequirementDTO(req, reqDTO);
				return;
			}
		}
		fail("requirement id not found: " + dto.requirement);
	}

	@Test
	public void testArrayBundleRevisionDTO() throws Exception {
        tb1.start();
		tb1.update(entryStream("dto.tb1.jar"));
        BundleRevisions revisions = tb1.adapt(BundleRevisions.class);
		BundleRevisionDTO[] dtos = tb1.adapt(BundleRevisionDTO[].class);
		assertThat(dtos)
				.as("dtos is null")
				.isNotNull();
		List<BundleRevision> revs = revisions.getRevisions();
		final int size = revs.size();
		assertThat(dtos.length)
				.as("Revision count does not match")
				.isEqualTo(size);
		for (int i = 0; i < size; i++) {
			assertTopBundleRevisionDTO(revs.get(i), dtos[i]);
		}
        tb1.uninstall();
        assertThat(tb1).isInState(Bundle.UNINSTALLED);
		dtos = tb1.adapt(BundleRevisionDTO[].class);
		assertThat(dtos)
				.as("Current BundleRevisionDTO[] for uninstalled bundle is not null")
				.isNull();
    }

	@Test
	public void testBundleWiringDTO() throws Exception {
        tb1.start();
        BundleWiring wiring = tb1.adapt(BundleWiring.class);
        BundleWiringDTO dto = tb1.adapt(BundleWiringDTO.class);
        assertBundleWiringDTO(wiring, dto);
        tb1.uninstall();
        assertThat(tb1).isInState(Bundle.UNINSTALLED);
        dto = tb1.adapt(BundleWiringDTO.class);
        assertThat(dto)
        		.as("Current BundleWiringDTO for uninstalled bundle is not null")
				.isNull();
    }

    private void assertBundleWiringDTO(BundleWiring wiring, BundleWiringDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		assertThat(dto.bundle)
				.as("Bundle id does not match")
				.isEqualTo(wiring.getBundle().getBundleId());
		resources = Collections.unmodifiableSet(new HashSet<BundleRevisionDTO>(dto.resources));
		resourcesUntested = new HashSet<BundleRevisionDTO>(resources);
		nodes = Collections.unmodifiableSet(new HashSet<BundleWiringDTO.NodeDTO>(dto.nodes));
		nodesUntested = new HashSet<BundleWiringDTO.NodeDTO>(nodes);
		assertBundleWiringNodeDTO(wiring, getBundleWiringNodeDTObyId(dto.root));
		assertThat(resourcesUntested)
				.as("Too many resources")
				.isEqualTo(Collections.EMPTY_SET);
		assertThat(nodesUntested)
				.as("Too many wiring nodes")
				.isEqualTo(Collections.EMPTY_SET);
	}

	private void assertFrameworkWiringDTO(Set<BundleWiring> allWirings,
			FrameworkWiringDTO dto, Map<BundleRevision,Integer> revisionIds)
			throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		assertThat(dto.wirings)
				.as("Wrong number of dto nodes").hasSize(allWirings.size());

		resources = Collections.unmodifiableSet(new HashSet<>(dto.resources));
		resourcesUntested = new HashSet<>(resources);
		nodes = Collections.unmodifiableSet(new HashSet<>(dto.wirings));
		nodesUntested = new HashSet<>(nodes);

		for (BundleWiringDTO.NodeDTO node : nodes) {
			BundleWiring wiring = findWiring(node, allWirings, revisionIds);
			assertBundleWiringNodeDTO(wiring, node);
		}
		assertThat(resourcesUntested)
				.as("Too many resources")
				.isEqualTo(Collections.EMPTY_SET);
		assertThat(nodesUntested)
				.as("Too many wiring nodes")
				.isEqualTo(Collections.EMPTY_SET);
    }

	private BundleWiring findWiring(BundleWiringDTO.NodeDTO node,
			Collection<BundleWiring> wirings,
			Map<BundleRevision,Integer> revisionIds) throws Exception {
		for (BundleWiring wiring : wirings) {
			Integer wiringRevisionId = revisionIds.get(wiring.getRevision());
			assertThat(wiringRevisionId)
					.as("Could not find revision id for wiring: "
					+ wiring.getRevision())
					.isNotNull();
			if (node.resource == wiringRevisionId.intValue()) {
				return wiring;
			}
		}
		fail("No wiring found for node: " + node);
		return null;
	}

	private BundleWiringDTO.NodeDTO getBundleWiringNodeDTObyId(int id) {
		assertThat(nodes)
				.as("wiring nodes not set")
				.isNotNull();
		for (BundleWiringDTO.NodeDTO dto : nodes) {
			if (dto.id == id) {
				return dto;
			}
		}
		fail("wiring node id not found: " + id);
		return null;
	}

	private void assertBundleWiringNodeDTO(BundleWiring wiring, BundleWiringDTO.NodeDTO dto) throws Exception {
		assertThat(dto)
				.as("wiring node is null")
				.isNotNull();
		if (!nodesUntested.remove(dto)) {
			return;
		}
        assertThat(dto.current)
        		.as("BundleWiring isCurrent does not match")
				.isEqualTo(wiring.isCurrent());
		assertThat(dto.inUse)
				.as("BundleWiring isUse does not match")
				.isEqualTo(wiring.isInUse());
		assertBundleRevisionDTO(wiring.getRevision(), getBundleRevisionDTObyId(dto.resource));
		assertListCapabilityRefDTO(wiring.getCapabilities(null), dto.capabilities);
		assertListRequirementRefDTO(wiring.getRequirements(null), dto.requirements);
        assertListBundleWireDTO(wiring.getProvidedWires(null), dto.providedWires);
        assertListBundleWireDTO(wiring.getRequiredWires(null), dto.requiredWires);
    }

	private void assertListBundleWireDTO(List<BundleWire> wires, List<WireDTO> dtos) throws Exception {
		assertThat(dtos)
        		.as("BundleWire count does not match").hasSize(wires.size());
        for (int i = 0; i < wires.size(); i++) {
            BundleWire wire = wires.get(i);
            BundleWireDTO dto = (BundleWireDTO) dtos.get(i);
            assertBundleWireDTO(wire, dto);
        }
    }

    private void assertBundleWireDTO(BundleWire wire, BundleWireDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
		assertBundleWiringNodeDTO(wire.getProviderWiring(), getBundleWiringNodeDTObyId(dto.providerWiring));
		assertBundleWiringNodeDTO(wire.getRequirerWiring(), getBundleWiringNodeDTObyId(dto.requirerWiring));
		assertCapabilityRefDTO(wire.getCapability(), dto.capability);
		assertRequirementRefDTO(wire.getRequirement(), dto.requirement);
		assertBundleRevisionDTO(wire.getProvider(), getBundleRevisionDTObyId(dto.provider));
		assertBundleRevisionDTO(wire.getRequirer(), getBundleRevisionDTObyId(dto.requirer));
    }

	@Test
	public void testArrayBundleWiringDTO() throws Exception {
        tb1.start();
		tb1.update(entryStream("dto.tb1.jar"));
        BundleRevisions revisions = tb1.adapt(BundleRevisions.class);
        List<BundleRevision> revs = revisions.getRevisions();
        final int size = revs.size();
        List<BundleWiring> wirings = new ArrayList<BundleWiring>(size);
        for (int i = 0; i < size; i++) {
            wirings.add(revs.get(i).getWiring());
        }
		BundleWiringDTO[] dtos = tb1.adapt(BundleWiringDTO[].class);
		assertBundleWiringDTOs(wirings, dtos);
        tb1.uninstall();
        assertThat(tb1).isInState(Bundle.UNINSTALLED);
		dtos = tb1.adapt(BundleWiringDTO[].class);
		assertThat(dtos)
				.as("Current BundleWiringDTO[] for uninstalled bundle is not null")
				.isNull();
    }

	private void assertBundleWiringDTOs(List<BundleWiring> wirings, BundleWiringDTO[] dtos) throws Exception {
		assertThat(dtos)
				.as("dtos is null")
				.isNotNull();
        final int size = wirings.size();
		assertThat(dtos.length)
				.as("Wiring count does not match")
				.isEqualTo(size);
        for (int i = 0; i < size; i++) {
			assertBundleWiringDTO(wirings.get(i), dtos[i]);
        }
    }

	@Test
	public void testArrayServiceReferenceDTO() throws Exception {
        tb1.start();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("dtotest", getName());
        Version versionProp = new Version("1.2.3.Q");
        properties.put("testVersion", versionProp);
        properties.put("testNumber", Double.valueOf(3.14));
        properties.put("testBoolean", Boolean.TRUE);
        properties.put("testCharacter", Character.valueOf('#'));
        TestDTO dtoProp = new TestDTO();
        dtoProp.field = "prop";
        properties.put("testDTO", dtoProp);
        Map<String, Object> mapProp = new HashMap<String, Object>();
        mapProp.put("mapProp", dtoProp);
        properties.put("testMap", mapProp);
        List<Object> listProp = new ArrayList<Object>();
        listProp.add(dtoProp);
        properties.put("testList", listProp);
        Set<Object> setProp = new HashSet<Object>();
        setProp.add(dtoProp);
        properties.put("testSet", setProp);
        Object[] objectArrayProp = new Object[] {dtoProp, versionProp};
        properties.put("testObjectArray", objectArrayProp);
        double[] doubleArrayProp = new double[] {3.14};
        properties.put("testdoubleArray", doubleArrayProp);
        Version[] versionArray = new Version[] {versionProp};
        properties.put("testVersionArray", versionArray);

        tb1.getBundleContext().registerService(DTO.class, new TestDTO(), properties);
        Collection<ServiceReference<DTO>> refs = bundleContext.getServiceReferences(DTO.class, "(dtotest=" + getName() + ")");
		assertThat(refs)
        		.as("Wrong number of ServiceReferences").hasSize(1);
        ServiceReference<DTO> ref = refs.iterator().next();
        bundleContext.getService(ref);
        assertThat(ref)
        		.as("Null ServiceReference")
				.isNotNull();
        ServiceReferenceDTO[] dtos = tb1.adapt(ServiceReferenceDTO[].class);
        assertThat(dtos)
        		.as("ServiceReferenceDTO[] is null")
				.isNotNull();
		assertThat(dtos.length)
				.as("ServiceReferenceDTO[] wrong length").isOne();
        ServiceReferenceDTO dto = dtos[0];
        assertThat(dto.bundle)
        		.as("ServiceReferenceDTO has wrong bundle")
				.isEqualTo(tb1.getBundleId());
        assertServiceReferenceDTO(ref, dto);
        tb1.stop();
        dtos = tb1.adapt(ServiceReferenceDTO[].class);
		assertThat(dtos)
				.as("ServiceReferenceDTO[] for stopped bundle is not null")
				.isNull();
        tb1.uninstall();
        dtos = tb1.adapt(ServiceReferenceDTO[].class);
        assertThat(dtos)
        		.as("ServiceReferenceDTO[] for uninstalled bundle is not null")
				.isNull();
    }

	@Test
	public void testServiceReferenceDTO() throws Exception {
		tb1.start();
		Dictionary<String,Object> properties = new Hashtable<String,Object>();
		properties.put("dtotest", getName());
		Version versionProp = new Version("1.2.3.Q");
		properties.put("testVersion", versionProp);
		properties.put("testNumber", Double.valueOf(3.14));
		properties.put("testBoolean", Boolean.TRUE);
		properties.put("testCharacter", Character.valueOf('#'));
		TestDTO dtoProp = new TestDTO();
		dtoProp.field = "prop";
		properties.put("testDTO", dtoProp);
		Map<String,Object> mapProp = new HashMap<String,Object>();
		mapProp.put("mapProp", dtoProp);
		properties.put("testMap", mapProp);
		List<Object> listProp = new ArrayList<Object>();
		listProp.add(dtoProp);
		properties.put("testList", listProp);
		Set<Object> setProp = new HashSet<Object>();
		setProp.add(dtoProp);
		properties.put("testSet", setProp);
		Object[] objectArrayProp = new Object[] {
				dtoProp, versionProp
		};
		properties.put("testObjectArray", objectArrayProp);
		double[] doubleArrayProp = new double[] {
				3.14
		};
		properties.put("testdoubleArray", doubleArrayProp);
		Version[] versionArray = new Version[] {
				versionProp
		};
		properties.put("testVersionArray", versionArray);

		tb1.getBundleContext()
				.registerService(DTO.class, new TestDTO(), properties);
		Collection<ServiceReference<DTO>> refs = bundleContext
				.getServiceReferences(DTO.class, "(dtotest=" + getName() + ")");
		assertThat(refs)
				.as("Wrong number of ServiceReferences").hasSize(1);
		ServiceReference<DTO> ref = refs.iterator().next();
		bundleContext.getService(ref);
		assertThat(ref)
				.as("Null ServiceReference")
				.isNotNull();
		ServiceReferenceDTO dto = ref.adapt(ServiceReferenceDTO.class);
		assertThat(dto.bundle)
				.as("ServiceReferenceDTO has wrong bundle")
				.isEqualTo(tb1.getBundleId());
		assertServiceReferenceDTO(ref, dto);
		tb1.stop();
	}

	@Test
	public void testFrameworkDTO() throws Exception {
        tb1.start();
        FrameworkDTO dto = tb1.adapt(FrameworkDTO.class);
        assertThat(dto)
        		.as("FrameworkDTO is not null")
				.isNull();
        Bundle systemBundle = bundleContext.getBundle(0);
        dto = systemBundle.adapt(FrameworkDTO.class);
        assertThat(dto)
        		.as("FrameworkDTO is null")
				.isNotNull();
        assertThat(dto.bundles)
        		.as("FrameworkDTO bundles is null")
				.isNotNull();
        Bundle[] bundlesArray = systemBundle.getBundleContext().getBundles();
        Map<Long, Bundle> bundlesMap = new HashMap<Long, Bundle>(bundlesArray.length);
        for (Bundle b : bundlesArray) {
            bundlesMap.put(Long.valueOf(b.getBundleId()), b);
        }
        for (BundleDTO b : dto.bundles) {
            Bundle bundle = bundlesMap.remove(Long.valueOf(b.id));
            assertThat(bundle)
            		.as("FrameworkDTO bundles has wrong bundle")
				.isNotNull();
            assertBundleDTO(bundle, b);
        }
        assertThat(dto.properties)
        		.as("FrameworkDTO has null properties")
				.isNotNull();
        for (Map.Entry<String, Object> entry : dto.properties.entrySet()) {
            assertThat(entry.getValue())
            		.as("FrameworkDTO wrong property")
				.isEqualTo(systemBundle.getBundleContext().getProperty(entry.getKey()));
        }
        assertThat(dto.services)
        		.as("FrameworkDTO services is null")
				.isNotNull();
        ServiceReference<?>[] services = systemBundle.getBundleContext().getAllServiceReferences(null, null);
        if (services == null) {
			assertThat(dto.services)
            		.as("FrameworkDTO services not empty").hasSize(0);
            return;
        }
		assertThat(dto.services)
        		.as("FrameworkDTO services wrong size").hasSize(services.length);
        Map<Long, ServiceReference<?>> servicesMap = new HashMap<Long, ServiceReference<?>>(services.length);
        for (ServiceReference<?> s : services) {
            servicesMap.put((Long) s.getProperty(Constants.SERVICE_ID), s);
        }
        for (ServiceReferenceDTO s : dto.services) {
            ServiceReference<?> service = servicesMap.remove(s.properties.get(Constants.SERVICE_ID));
            assertThat(service)
            		.as("FrameworkDTO services wrong")
				.isNotNull();
            assertServiceReferenceDTO(service, s);
        }
    }

    private void assertServiceReferenceDTO(ServiceReference<?> ref, ServiceReferenceDTO dto) throws Exception {
		assertThat(dto)
				.as("dto is null")
				.isNotNull();
        assertThat(dto.bundle)
        		.as("ServiceReferenceDTO has wrong bundle")
				.isEqualTo(ref.getBundle().getBundleId());
        assertThat(dto.properties)
        		.as("ServiceReferenceDTO has null properties")
				.isNotNull();
        String[] keys = ref.getPropertyKeys();
        assertThat(dto.properties.size())
        		.as("ServiceReferenceDTO properties the wrong size")
				.isEqualTo(keys.length);
        for (String k : keys) {
			Object v = ref.getProperty(k);
			if (Constants.SERVICE_ID.equals(k)) {
				assertThat(dto.id)
						.as("service.id does not match")
				.isEqualTo(((Long) v).longValue());
			}
			assertValueEquals("ServiceReferenceDTO has wrong property", v, dto.properties.get(k));
        }
        assertThat(dto.usingBundles)
        		.as("ServiceReferenceDTO has null usingBundles")
				.isNotNull();
        Bundle[] using = ref.getUsingBundles();
        if (using == null) {
			assertThat(dto.usingBundles.length)
					.as("ServiceReferenceDTO usingBundles not empty").isZero();
            return;
        }
        assertThat(dto.usingBundles.length)
        		.as("ServiceReferenceDTO usingBundles wrong size")
				.isEqualTo(using.length);
        Map<Long, Bundle> bundlesMap = new HashMap<Long, Bundle>(using.length);
        for (Bundle b : using) {
            bundlesMap.put(Long.valueOf(b.getBundleId()), b);
        }
        for (long id : dto.usingBundles) {
            Bundle bundle = bundlesMap.remove(Long.valueOf(id));
            assertThat(bundle)
            		.as("ServiceReferenceDTO usingBundles has wrong bundle")
				.isNotNull();
        }
    }

    private void assertValueEquals(String message, Object expected, Object actual) throws Exception {
        if (expected.getClass().isArray()) {
            assertThat(actual.getClass().isArray())
            		.as(message)
				.isTrue();
            final int length = Array.getLength(expected);
            assertThat(Array.getLength(actual))
            		.as(message)
				.isEqualTo(length);
            for (int i = 0; i < length; i++) {
                assertValueEquals(message, Array.get(expected, i), Array.get(actual, i));
            }
            return;
        }
        assertThat(actual.getClass().isArray())
        		.as(message)
				.isFalse();
        if (actual instanceof String && (!(expected instanceof String))) {
            expected = String.valueOf(expected);
        }
        assertThat(actual)
        		.as(message)
				.isEqualTo(expected);
    }

	@Test
	public void testFrameworkStartLevelDTO() throws Exception {
        tb1.start();
        FrameworkStartLevelDTO dto = tb1.adapt(FrameworkStartLevelDTO.class);
        assertThat(dto)
        		.as("FrameworkStartLevelDTO is not null")
				.isNull();
        Bundle systemBundle = bundleContext.getBundle(0);
        dto = systemBundle.adapt(FrameworkStartLevelDTO.class);
        assertThat(dto)
        		.as("FrameworkStartLevelDTO is null")
				.isNotNull();
        FrameworkStartLevel fsl = systemBundle.adapt(FrameworkStartLevel.class);
        assertThat(fsl)
        		.as("FrameworkStartLevel is null")
				.isNotNull();
        assertThat(dto.initialBundleStartLevel)
        		.as("FrameworkStartLevelDTO initialBundleStartLevel wrong")
				.isEqualTo(fsl.getInitialBundleStartLevel());
        assertThat(dto.startLevel)
        		.as("FrameworkStartLevelDTO startLevel wrong")
				.isEqualTo(fsl.getStartLevel());
    }

    public static class TestDTO extends DTO {
        public String field;
    }

	private Set<BundleWiring> getAllWirings() {
		Set<Bundle> allBundles = new HashSet<>(
				Arrays.asList(bundleContext.getBundles()));
		FrameworkWiring fwkWiring = bundleContext
				.getBundle(Constants.SYSTEM_BUNDLE_LOCATION)
				.adapt(FrameworkWiring.class);
		allBundles.addAll(fwkWiring.getRemovalPendingBundles());

		Set<BundleWiring> allWirings = new HashSet<>();
		for (Bundle bundle : allBundles) {
			BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
			for (BundleRevision revision : revisions.getRevisions()) {
				BundleWiring wiring = revision.getWiring();
				if (wiring != null) {
					allWirings.add(wiring);
				}
			}
		}
		return allWirings;
	}

	@Test
	public void testFrameworkWiringDTO() throws Exception {
		Bundle systemBundle = bundleContext.getBundle(0);

		tb1.start();
		tb2.start();
		int initialWiringCount = getAllWirings().size();

		Map<BundleRevision,Integer> revisionIds = getRevisionIDs(
				new HashMap<BundleRevision,Integer>());
		FrameworkWiringDTO dto = systemBundle.adapt(FrameworkWiringDTO.class);
		assertThat(dto.wirings)
				.as("Wrong number of wirings").hasSize(initialWiringCount);
		assertFrameworkWiringDTO(getAllWirings(), dto, revisionIds);

		tb1.update(entryStream("dto.tb1.jar"));

		// get the latest revision ids now to pick up the new revision
		revisionIds = getRevisionIDs(revisionIds);

		dto = systemBundle.adapt(FrameworkWiringDTO.class);
		assertThat(dto.wirings)
				.as("Wrong number of wirings").hasSize(initialWiringCount + 1);
		assertFrameworkWiringDTO(getAllWirings(), dto, revisionIds);

		FrameworkWirings.refreshBundles(bundleContext, tb1);

		dto = systemBundle.adapt(FrameworkWiringDTO.class);
		assertThat(dto.wirings)
				.as("Wrong number of wirings").hasSize(initialWiringCount);
		assertFrameworkWiringDTO(getAllWirings(), dto, revisionIds);

		tb1.uninstall();

		assertThat(tb1).isInState(Bundle.UNINSTALLED);

		dto = systemBundle.adapt(FrameworkWiringDTO.class);
		assertThat(dto.wirings)
				.as("Wrong number of wirings").hasSize(initialWiringCount);
		assertFrameworkWiringDTO(getAllWirings(), dto, revisionIds);

		// Refreshing will remove the removal pending wiring
		// The tb2 wiring will not exist because it cannot resolve
		FrameworkWirings.refreshBundles(bundleContext, tb1);
		dto = systemBundle.adapt(FrameworkWiringDTO.class);
		assertThat(dto.wirings)
				.as("Wrong number of wirings").hasSize(initialWiringCount - 2);
		assertFrameworkWiringDTO(getAllWirings(), dto, revisionIds);
	}

	private Map<BundleRevision,Integer> getRevisionIDs(
			Map<BundleRevision,Integer> revisionIDs) {
		// Get every revision's DTO ID so we can easily look it up later
		// This must to be done while the bundles are installed
		for (Bundle bundle : bundleContext.getBundles()) {
			BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
			List<BundleRevision> revisionList = revisions.getRevisions();
			BundleRevisionDTO[] revisionDTOs = bundle
					.adapt(BundleRevisionDTO[].class);
			assertThat(revisionDTOs.length)
					.as("Unexpected number of dtos")
				.isEqualTo(revisions.getRevisions().size());
			for (int i = 0; i < revisionDTOs.length; i++) {
				revisionIDs.put(revisionList.get(i),
						Integer.valueOf(revisionDTOs[i].id));
			}
		}
		return revisionIDs;
	}
}
