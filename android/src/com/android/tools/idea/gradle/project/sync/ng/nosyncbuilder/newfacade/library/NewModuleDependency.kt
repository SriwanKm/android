/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.newfacade.library

import com.android.ide.common.gradle.model.level2.IdeModuleLibrary
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.interfaces.library.ModuleDependency
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.misc.OldJavaDependency
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.proto.LibraryProto

data class NewModuleDependency(
  override val buildId: String,
  override val projectPath: String,
  override val variant: String?, // not null only for Android modules
  override val artifactAddress: String
) : ModuleDependency {
  constructor(library: IdeModuleLibrary) : this(
    library.buildId!!,
    library.projectPath!!,
    library.variant!!,
    library.artifactAddress
  )

  constructor(library: OldJavaDependency) : this(
    library.buildId!!,
    library.project!!,
    null,
    library.name
  )

  constructor(proto: LibraryProto.ModuleDependency) : this(
    proto.buildId,
    proto.projectPath,
    if (proto.hasVariant()) proto.variant else null,
    proto.library.artifactAddress
  )
}
