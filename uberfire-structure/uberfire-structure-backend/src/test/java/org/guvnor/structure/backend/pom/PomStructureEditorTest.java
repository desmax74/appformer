/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.guvnor.structure.backend.pom;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.guvnor.structure.backend.pom.types.FakeJPATypeDependency;
import org.guvnor.structure.pom.AddPomDependencyEvent;
import org.guvnor.structure.pom.DependencyType;
import org.junit.Before;
import org.junit.Test;
import org.uberfire.backend.vfs.PathFactory;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

public class PomStructureEditorTest {

    private final String POM = "pom.xml";
    private PomStructureEditor editor;
    private Path tmpRoot, tmp;
    private DynamicDependencyTypeConfigurationMap configurationMap;
    private String JPA_HIBERNATE_VERSION;

    @Before
    public void setUp() throws Exception {
        tmpRoot = Files.createTempDirectory("repo");
        tmp = TestUtil.createAndCopyToDirectory(tmpRoot,
                                                "dummy",
                                                "target/test-classes/dummy_empty_deps");
        configurationMap = new DynamicDependencyTypeConfigurationMap();
        DependencyType depType = new FakeJPATypeDependency();
        configurationMap.addDependencies(depType,
                                         depType.getDependencies());
        JPA_HIBERNATE_VERSION = configurationMap.getMapping().get(depType.getType()).get(0).getVersion();
    }

    @Test
    public void onNewDynamicDependencyEventTest() throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new ByteArrayInputStream(Files.readAllBytes(Paths.get(tmp.toAbsolutePath().toString() + File.separator + POM))));
        assertThat(model.getDependencies()).hasSize(0);

        editor = new PomStructureEditor(configurationMap);
        AddPomDependencyEvent event = new AddPomDependencyEvent(new HashSet<>(Arrays.asList(new FakeJPATypeDependency())),
                                                                PathFactory.newPath(tmp.getFileName().toString(),
                                                                                    tmp.toUri().toString() + File.separator + POM));
        editor.onNewDynamicDependency(event);

        model = reader.read(new ByteArrayInputStream(Files.readAllBytes(Paths.get(tmp.toAbsolutePath().toString() + File.separator + POM))));
        assertThat(model.getDependencies()).hasSize(1);
        Dependency dep = model.getDependencies().get(0);
        assertThat(dep.getGroupId()).containsOnlyOnce("org.hibernate.javax.persistence");
        assertThat(dep.getArtifactId()).containsOnlyOnce("hibernate-jpa-2.1-api");
        assertThat(dep.getVersion()).containsOnlyOnce(JPA_HIBERNATE_VERSION);
    }
}

