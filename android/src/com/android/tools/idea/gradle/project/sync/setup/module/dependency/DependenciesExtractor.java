/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.gradle.project.sync.setup.module.dependency;

import com.android.builder.model.level2.Library;
import com.android.ide.common.gradle.model.IdeAndroidArtifact;
import com.android.ide.common.gradle.model.IdeBaseArtifact;
import com.android.ide.common.gradle.model.IdeVariant;
import com.android.ide.common.gradle.model.level2.IdeDependencies;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.repository.GradleVersion;
import com.android.tools.idea.gradle.project.sync.setup.module.ModuleFinder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import org.jetbrains.annotations.NotNull;

import static com.android.tools.idea.gradle.project.sync.Modules.createUniqueModuleId;
import static com.intellij.openapi.roots.DependencyScope.COMPILE;
import static com.intellij.openapi.roots.DependencyScope.TEST;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.util.text.StringUtil.trimLeading;

/**
 * Creates {@link DependencySet} from variant or artifact.
 */
public class DependenciesExtractor {
  @NotNull
  public static DependenciesExtractor getInstance() {
    return ServiceManager.getService(DependenciesExtractor.class);
  }

  /**
   * Get a {@link DependencySet} contains merged dependencies from main artifact and test artifacts.
   *
   * @param variant             the variant to extract dependencies from.
   * @param moduleFinder
   * @return Instance of {@link DependencySet} retrieved from given variant.
   */
  @NotNull
  public DependencySet extractFrom(@NotNull IdeVariant variant, @NotNull ModuleFinder moduleFinder) {
    DependencySet dependencies = new DependencySet();

    for (IdeBaseArtifact testArtifact : variant.getTestArtifacts()) {
      populate(dependencies, testArtifact, moduleFinder, TEST);
    }

    IdeAndroidArtifact mainArtifact = variant.getMainArtifact();
    populate(dependencies, mainArtifact, moduleFinder, COMPILE);

    return dependencies;
  }

  /**
   * @param artifact the artifact to extract dependencies from.
   * @param scope    Scope of the dependencies, e.g. "compile" or "test".
   * @return Instance of {@link DependencySet} retrieved from given artifact.
   */
  @NotNull
  public DependencySet extractFrom(@NotNull IdeBaseArtifact artifact,
                                   @NotNull DependencyScope scope,
                                   @NotNull ModuleFinder moduleFinder) {
    DependencySet dependencies = new DependencySet();
    populate(dependencies, artifact, moduleFinder, scope);
    return dependencies;
  }

  private static void populate(@NotNull DependencySet dependencies,
                               @NotNull IdeBaseArtifact artifact,
                               @NotNull ModuleFinder moduleFinder,
                               @NotNull DependencyScope scope) {
    IdeDependencies artifactDependencies = artifact.getLevel2Dependencies();

    for (Library library : artifactDependencies.getJavaLibraries()) {
      LibraryDependency libraryDependency = new LibraryDependency(library.getArtifact(), library.getArtifactAddress(), scope);
      libraryDependency.addBinaryPath(library.getArtifact());
      dependencies.add(libraryDependency);
    }

    for (Library library : artifactDependencies.getAndroidLibraries()) {
      dependencies.add(createLibraryDependencyFromAndroidLibrary(library, scope));
    }

    for (Library library : artifactDependencies.getModuleDependencies()) {
      String gradlePath = library.getProjectPath();
      if (isNotEmpty(gradlePath)) {
        Module module = null;
        String moduleId = gradlePath;
        String projectFolderPath = library.getBuildId();
        if (isNotEmpty(projectFolderPath)) {
          moduleId = createUniqueModuleId(projectFolderPath, gradlePath);
          module = moduleFinder.findModuleByModuleId(moduleId);
        }
        if (module == null) {
          moduleId = gradlePath;
          module = moduleFinder.findModuleByGradlePath(moduleId);
        }
        ModuleDependency dependency = new ModuleDependency(moduleId, scope, module);
        dependencies.add(dependency);
      }
    }
  }

  @NotNull
  private static LibraryDependency createLibraryDependencyFromAndroidLibrary(@NotNull Library library,
                                                                             @NotNull DependencyScope scope) {
    LibraryDependency dependency = new LibraryDependency(library.getArtifact(), library.getArtifactAddress(), scope);
    dependency.addBinaryPath(library.getCompileJarFile());
    dependency.addBinaryPath(library.getResFolder());

    for (String localJar : library.getLocalJars()) {
      dependency.addBinaryPath(localJar);
    }
    return dependency;
  }

  /**
   * Computes a library name intended for display purposes; names may not be unique
   * (and separator is always ":"). It will only show the artifact id, if that id contains slashes, otherwise
   * it will include the last component of the group id (unless identical to the artifact id).
   *
   * E.g.
   * com.android.support.test.espresso:espresso-core:3.0.1@aar -> espresso-core:3.0.1
   * android.arch.lifecycle:extensions:1.0.0-beta1@aar -> lifecycle:extensions:1.0.0-beta1
   * com.google.guava:guava:11.0.2@jar -> guava:11.0.2
   */
  @NotNull
  public static String getDependencyDisplayName(@NotNull Library library) {
    String artifactAddress = library.getArtifactAddress();
    GradleCoordinate coordinates = GradleCoordinate.parseCoordinateString(artifactAddress);
    if (coordinates != null) {
      String name = coordinates.getArtifactId();
      if (name == null) {
        name = "?";
      }

      // For something like android.arch.lifecycle:runtime, instead of just showing "runtime",
      // we show "lifecycle:runtime"
      if (!name.contains("-")) {
        String groupId = coordinates.getGroupId();
        if (groupId != null) {
          int index = groupId.lastIndexOf('.'); // okay if it doesn't exist
          String groupSuffix = groupId.substring(index + 1);
          if (!groupSuffix.equals(name)) { // e.g. for com.google.guava:guava we'd end up with "guava:guava"
            name = groupSuffix + ":" + name;
          }
        }
      }

      GradleVersion version = coordinates.getVersion();
      if (version != null && !"unspecified".equals(version.toString())) {
        name += ":" + version;
      }
      return name;
    }
    return trimLeading(artifactAddress, ':');
  }
}
