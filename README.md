# Bazel rules for Spring AOT

Bazel rules for building Spring applications with Ahead-of-Time (AOT) compilation support. These rules enable you to leverage Spring's AOT processing capabilities within your Bazel build, resulting in faster startup times and reduced memory footprint for your Spring applications.

## Features

- 🚀 **Faster Startup**: Pre-process Spring beans and configurations at build time
- 💾 **Reduced Memory**: Lower runtime memory usage through AOT optimizations
- 🔧 **Native Image Support**: Essential for GraalVM native image compilation
- 📦 **Seamless Integration**: Works with existing Bazel Java rules
- 🔄 **Reproducible Builds**: Consistent AOT processing across environments

## Installation

### Using Bazel Modules (Bzlmod)

Add to your `MODULE.bazel`:

```starlark
bazel_dep(name = "rules_spring_aot", version = "0.1.0")
```

### Using WORKSPACE

From the release you wish to use:
<https://github.com/tharakadesilva/rules_spring_aot/releases>
copy the WORKSPACE snippet into your `WORKSPACE` file.

To use a commit rather than a release, you can point at any SHA of the repo.

For example to use commit `abc123`:

1. Replace `url = "https://github.com/tharakadesilva/rules_spring_aot/releases/download/v0.1.0/rules_spring_aot-v0.1.0.tar.gz"` with a GitHub-provided source archive like `url = "https://github.com/tharakadesilva/rules_spring_aot/archive/abc123.tar.gz"`
2. Replace `strip_prefix = "rules_spring_aot-0.1.0"` with `strip_prefix = "rules_spring_aot-abc123"`
3. Update the `sha256`. The easiest way to do this is to comment out the line, then Bazel will
   print a message with the correct value.

## Quick Start

### 1. Basic Spring Boot Application with AOT

```starlark
load("@rules_spring_aot//spring_aot:defs.bzl", "spring_aot_library")
load("@rules_java//java:defs.bzl", "java_library", "java_binary")

# Define your Spring Boot application
java_library(
    name = "my-app-lib",
    srcs = glob(["src/main/java/**/*.java"]),
    runtime_deps = [
        "@maven//:org_springframework_boot_spring_boot_starter_web",
    ],
    deps = [
        # ... other dependencies
    ],
)

# Apply AOT processing
# Creates a java_library containing all AOT-generated classes and resources
spring_aot_library(
    name = "my-app-aot",
    target = ":my-app-lib",
    main_class = "com.example.MyApplication",
    group_id = "com.example",
    artifact_id = "my-app",
)

# Create executable JAR
# Produces a standard JAR with improved startup time (faster than regular Spring Boot, slower than native)
java_binary(
    name = "my-app",
    main_class = "com.example.MyApplication",
    runtime_deps = [":my-app-aot"],
    jvm_flags = ["-Dspring.aot.enabled=true"],
)
```

### 2. Native Image with GraalVM

See [rules_graalvm](https://github.com/sgammon/rules_graalvm) for setup instructions.

```starlark
load("@rules_spring_aot//spring_aot:defs.bzl", "spring_aot_library")
load("@rules_graalvm//graalvm:defs.bzl", "native_image")

# ... (previous definitions)

# Build native executable
native_image(
    name = "my-app-native",
    main_class = "com.example.MyApplication",
    native_image_tool = "@graalvm//:native-image",
    deps = [":my-app-aot"],
)
```

## Requirements

- Bazel 8.3 or higher
- Java 17 or higher
- Spring Boot 3.0 or higher (requires AOT support)
  - **Note**: `org.springframework.boot/spring-boot` must be present in the target java_library's classpath
- GraalVM (optional, for native image builds)
- **Platform**: Linux and macOS only (Windows is not supported)
