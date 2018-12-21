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

import java.util.List;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.guvnor.structure.pom.AddPomDependencyEvent;
import org.guvnor.structure.pom.DependencyType;
import org.guvnor.structure.pom.DynamicPomDependency;
import org.guvnor.structure.pom.PomDependencyExperimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.vfs.Path;
import org.uberfire.experimental.service.backend.BackendExperimentalFeaturesRegistryService;

/**
 * Editor to receive PomDependency Event
 */
@ApplicationScoped
public class PomStructureEditor {

    private final Logger logger = LoggerFactory.getLogger(PomStructureEditor.class);
    boolean enabledPomDependenciesFeature;
    private PomEditor pomEditor;
    private DynamicDependencyTypeConfigurationMap configurationMap;
    private BackendExperimentalFeaturesRegistryService experimentalServiceRegistry;

    // for test
    public PomStructureEditor(DynamicDependencyTypeConfigurationMap map) {
        configurationMap = map;
        pomEditor = new PomEditorDefault(configurationMap);
        enabledPomDependenciesFeature = true;
    }

    @Inject
    public PomStructureEditor(BackendExperimentalFeaturesRegistryService experimentalServiceRegistry, DynamicDependencyTypeConfigurationMap configurationMap) {
        this.configurationMap = configurationMap;
        pomEditor = new PomEditorDefault(configurationMap);
        this.experimentalServiceRegistry = experimentalServiceRegistry;
    }

    public void onNewDynamicDependency(final @Observes AddPomDependencyEvent event) {
        if (experimentalServiceRegistry != null) {
            enabledPomDependenciesFeature = experimentalServiceRegistry.getExperimentalFeaturesSession().getFeaturesRegistry().isFeatureEnabled(PomDependencyExperimental.class.getName());
        }
        if (enabledPomDependenciesFeature) {
            final Path projectPath = event.getProjectPath();
            final Set<DependencyType> dependencyTypes = event.getDependencyTypes();
            addDependenciesToPom(projectPath,
                                 dependencyTypes,
                                 configurationMap);
        }
    }

    private void addDependenciesToPom(Path projectPath,
                                      Set<DependencyType> dependencyTypes,
                                      DynamicDependencyTypeConfigurationMap map) {
        List<DynamicPomDependency> deps = map.getDependencies(dependencyTypes);
        if (!pomEditor.addDependencies(dependencyTypes,
                                       projectPath)) {
            logger.warn("Failed to add dependencies {} to pom.xml located in {}",
                        deps,
                        projectPath);
        }
    }
}
