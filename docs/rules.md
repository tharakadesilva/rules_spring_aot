<!-- Generated with Stardoc: http://skydoc.bazel.build -->

Rule for executing Spring Application AOT Processor

<a id="spring_aot_process"></a>

## spring_aot_process

<pre>
load("@rules_spring_aot//spring_aot:defs.bzl", "spring_aot_process")

spring_aot_process(<a href="#spring_aot_process-name">name</a>, <a href="#spring_aot_process-args">args</a>, <a href="#spring_aot_process-artifact_id">artifact_id</a>, <a href="#spring_aot_process-group_id">group_id</a>, <a href="#spring_aot_process-main_class">main_class</a>, <a href="#spring_aot_process-target">target</a>)
</pre>

Executes Spring Application AOT Processor.

This rule runs the org.springframework.boot.SpringApplicationAotProcessor
to generate AOT-optimized classes and resources for a Spring Boot application.

**ATTRIBUTES**


| Name  | Description | Type | Mandatory | Default |
| :------------- | :------------- | :------------- | :------------- | :------------- |
| <a id="spring_aot_process-name"></a>name |  A unique name for this target.   | <a href="https://bazel.build/concepts/labels#target-names">Name</a> | required |  |
| <a id="spring_aot_process-args"></a>args |  Additional arguments to pass to the AOT processor   | List of strings | optional |  `[]`  |
| <a id="spring_aot_process-artifact_id"></a>artifact_id |  Maven artifact ID for the generated AOT artifacts   | String | required |  |
| <a id="spring_aot_process-group_id"></a>group_id |  Maven group ID for the generated AOT artifacts   | String | required |  |
| <a id="spring_aot_process-main_class"></a>main_class |  The main class of the Spring Boot application to process   | String | required |  |
| <a id="spring_aot_process-target"></a>target |  Java library target to process with AOT   | <a href="https://bazel.build/concepts/labels">Label</a> | required |  |


