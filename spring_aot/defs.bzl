"""Rule for executing Spring Application AOT Processor"""

load("@rules_java//java:defs.bzl", "JavaInfo")
load("@rules_java//java:java_library.bzl", "java_library")

def _spring_aot_process_impl(ctx):
    java_runtime = ctx.toolchains["@bazel_tools//tools/jdk:runtime_toolchain_type"].java_runtime

    target_info = ctx.attr.target[JavaInfo]
    classpath_jars = target_info.transitive_runtime_jars

    compiled_jar = ctx.actions.declare_file(ctx.label.name + ".jar")

    aot_and_compile_script = ctx.actions.declare_file(ctx.label.name + "_aot_and_compile.sh")
    ctx.actions.write(
        output = aot_and_compile_script,
        content = """#!/bin/bash
set -e

SOURCE_OUT=$(mktemp -d)
RESOURCE_OUT=$(mktemp -d)
CLASS_OUT=$(mktemp -d)

mkdir -p "$SOURCE_OUT" "$RESOURCE_OUT" "$CLASS_OUT"

trap 'echo "Cleaning up..."; rm -rf "$SOURCE_OUT" "$RESOURCE_OUT" "$CLASS_OUT"' EXIT SIGINT SIGTERM

echo "Running Spring AOT processor..."
{java} -cp "{classpath}" org.springframework.boot.SpringApplicationAotProcessor \
    {main_class} "$SOURCE_OUT" "$RESOURCE_OUT" "$CLASS_OUT" \
    {group_id} {artifact_id} {args}

echo "AOT processing complete. Checking generated files..."
echo "Source files:"
find "$SOURCE_OUT" -name "*.java" -type f | head -20

echo "Generated application context initializer:"
if [ -f "$SOURCE_OUT/com/example/DemoApplication__ApplicationContextInitializer.java" ]; then
    echo "Found ApplicationContextInitializer"
else
    echo "ApplicationContextInitializer NOT FOUND!"
fi

if find "$SOURCE_OUT" -name "*.java" -type f | head -1 | grep -q .; then
    echo "Compiling generated AOT sources..."
    TEMP_CLASSES=$(mktemp -d)
    trap 'rm -rf "$TEMP_CLASSES"' EXIT SIGINT SIGTERM

    find "$SOURCE_OUT" -name "*.java" -type f > sources.txt
    {javac} -cp "{classpath}:$CLASS_OUT" -d $TEMP_CLASSES @sources.txt

    if [ -d "$CLASS_OUT" ] && [ "$(ls -A "$CLASS_OUT")" ]; then
        echo "Copying CGLIB proxy classes..."
        cp -r "$CLASS_OUT"/* $TEMP_CLASSES/ 2>/dev/null || true
    fi

    if [ -d "$RESOURCE_OUT" ] && [ "$(ls -A "$RESOURCE_OUT")" ]; then
        echo "Copying resources..."
        cp -r "$RESOURCE_OUT"/* $TEMP_CLASSES/ 2>/dev/null || true
    fi

    {jar} cf {output_jar} -C $TEMP_CLASSES .

    echo "JAR created successfully"
else
    echo "No generated sources found, creating empty JAR"
    exit 1
fi
""".format(
            java = java_runtime.java_executable_exec_path,
            javac = java_runtime.java_home + "/bin/javac",
            jar = java_runtime.java_home + "/bin/jar",
            classpath = ":".join([jar.path for jar in classpath_jars.to_list()]),
            main_class = ctx.attr.main_class,
            group_id = ctx.attr.group_id,
            artifact_id = ctx.attr.artifact_id,
            args = " ".join(ctx.attr.args),
            output_jar = compiled_jar.path,
        ),
        is_executable = True,
    )

    ctx.actions.run(
        outputs = [compiled_jar],
        inputs = depset(
            direct = [aot_and_compile_script],
            transitive = [classpath_jars, java_runtime.files],
        ),
        executable = aot_and_compile_script,
        mnemonic = "SpringAotProcessAndCompile",
        progress_message = "Running Spring AOT processor and compiling for %s" % ctx.label,
    )

    java_info = JavaInfo(
        output_jar = compiled_jar,
        compile_jar = compiled_jar,
        runtime_deps = [ctx.attr.target[JavaInfo]],
    )

    return [
        DefaultInfo(files = depset([compiled_jar])),
        java_info,
    ]

spring_aot_process = rule(
    implementation = _spring_aot_process_impl,
    attrs = {
        "main_class": attr.string(
            mandatory = True,
            doc = "The main class of the Spring Boot application to process",
        ),
        "group_id": attr.string(
            mandatory = True,
            doc = "Maven group ID for the generated AOT artifacts",
        ),
        "artifact_id": attr.string(
            mandatory = True,
            doc = "Maven artifact ID for the generated AOT artifacts",
        ),
        "target": attr.label(
            mandatory = True,
            doc = "Java library target to process with AOT",
            providers = [JavaInfo],
        ),
        "args": attr.string_list(
            doc = "Additional arguments to pass to the AOT processor",
        ),
    },
    provides = [JavaInfo],
    toolchains = [
        "@bazel_tools//tools/jdk:runtime_toolchain_type",
    ],
    doc = """Executes Spring Application AOT Processor.

This rule runs the org.springframework.boot.SpringApplicationAotProcessor
to generate AOT-optimized classes and resources for a Spring Boot application.
""",
)

def _spring_aot_library_impl(name, artifact_id, group_id, main_class, target, **kwargs):
    processed_target_name = name + "_aot_process"
    spring_aot_process(
        name = processed_target_name,
        artifact_id = artifact_id,
        group_id = group_id,
        main_class = main_class,
        target = target,
    )

    java_library(
        name = name,
        runtime_deps = [":" + processed_target_name],
        **kwargs
    )

spring_aot_library = macro(
    attrs = {
        "artifact_id": attr.string(
            mandatory = True,
            doc = "Maven artifact ID for the generated AOT artifacts",
        ),
        "group_id": attr.string(
            mandatory = True,
            doc = "Maven group ID for the generated AOT artifacts",
        ),
        "main_class": attr.string(
            mandatory = True,
            doc = "The main class of the Spring Boot application",
        ),
        "target": attr.label(
            mandatory = True,
            doc = "Java library target to process with AOT",
            providers = [JavaInfo],
        ),
    },
    doc = """Creates a Java library with Spring AOT processing.

    This macro takes a regular Java library and processes it through Spring's AOT
    processor to generate optimized classes and resources.
    """,
    implementation = _spring_aot_library_impl,
)
