load("//tools/base/bazel:bazel.bzl", "iml_module")
load("//tools/base/bazel:maven.bzl", "maven_repo")
load("//tools/base/bazel:maven.bzl", "maven_java_library")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.resources-aar",
    srcs = ["src"],
    iml_files = ["intellij.android.resources-aar.iml"],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/base/common:studio.android.sdktools.common[module]",
        "//tools/base/sdk-common:studio.android.sdktools.sdk-common[module]",
        "//tools/idea/platform/util:intellij.platform.util[module]",
        "//tools/idea/platform/util-rt:intellij.platform.util.rt[module]",
        "//tools/idea/.idea/libraries:aapt-proto",
        "//tools/idea/.idea/libraries:kotlin-stdlib-jdk8",
        "//tools/idea/.idea/libraries:protobuf",
    ],
)

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.resources-aar.tests",
    iml_files = ["intellij.android.resources-aar.tests.iml"],
    test_class = "com.android.tools.idea.resources.aar.ResourcesAarTestSuite",
    test_coverage = True,
    # keep sorted
    test_data = [
        ":framework_res",
        "//prebuilts/studio/layoutlib:data/res",
        "//prebuilts/studio/sdk:platforms/latest",
        "//tools/adt/idea/android/testData",
        "//tools/idea:platform-build-info",
    ],
    # do not sort: must match IML order
    test_runtime_deps = [
        "//tools/idea/platform/tasks-platform-impl:intellij.platform.tasks.impl",
        "//tools/idea/java/compiler/instrumentation-util-8:intellij.java.compiler.instrumentationUtil.java8",
        "//tools/idea/jvm/jvm-analysis-impl:intellij.jvm.analysis.impl",
        "//tools/idea/plugins/maven/error-prone-compiler:intellij.maven.errorProne.compiler",
        "//tools/idea/plugins/IntelliLang/intellilang-jps-plugin:intellij.java.langInjection.jps",
        "//tools/adt/idea/databinding:intellij.android.databinding",
    ],
    test_shard_count = 8,
    test_srcs = ["testSrc"],
    test_tags = [
        "cpu:3",
        "block-network",
        "no_test_windows",
    ],
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//tools/base/layoutlib-api:studio.android.sdktools.layoutlib-api[module, test]",
        "//tools/base/sdk-common:studio.android.sdktools.sdk-common[module, test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/adt/idea/android:intellij.android.core[module, test]",
        "//tools/adt/idea/android:intellij.android.core.tests[module, test]",
        "//tools/adt/idea/resources-aar:intellij.android.resources-aar[module, test]",
        "//tools/adt/idea/android-test-framework:intellij.android.testFramework[module, test]",
        "//tools/idea:intellij.platform.jps.build_and_others[module, test]",
        "//tools/idea/platform/testFramework:intellij.platform.testFramework[module, test]",
        "//tools/idea/.idea/libraries:aapt-proto[test]",
        "//tools/idea/.idea/libraries:JUnit4[test]",
        "//tools/idea/.idea/libraries:protobuf[test]",
        "//tools/idea/.idea/libraries:truth[test]",
    ],
)

java_binary(
    name = "framework-res-jar-creator",
    main_class = "com.android.tools.idea.resources.aar.FrameworkResJarCreator",
    runtime_deps = [":intellij.android.resources-aar_runtime"],
)

genrule(
    name = "framework_res",
    srcs = [
        "//prebuilts/studio/layoutlib:data/res",
        "//prebuilts/studio/layoutlib:data/res/version",
    ],
    outs = ["framework_res.jar"],
    cmd = "$(JAVA) -classpath $(location :framework-res-jar-creator_deploy.jar) " + "com.android.tools.idea.resources.aar.FrameworkResJarCreator " + "$(location //prebuilts/studio/layoutlib:data/res/version)/.. $(OUTS)",
    toolchains = ["//prebuilts/studio/jdk:jdk_runtime"],
    tools = [
        ":framework-res-jar-creator",
        ":framework-res-jar-creator_deploy.jar",
    ],
    visibility = ["//visibility:public"],
)
