package com.bazel.spring.aot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Process aot.factories files to generate GraalVM native-image configuration.
 * This implements the missing functionality in rules_spring_aot that Maven/Gradle provide automatically.
 */
public class SpringAotFactoriesProcessor {
    
    private static final String AOT_FACTORIES = "META-INF/spring/aot.factories";
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: SpringAotFactoriesProcessor <classpath> <output-dir>");
            System.exit(1);
        }
        
        String classpath = args[0];
        String outputDir = args[1];
        
        System.out.println("=== SpringAotFactoriesProcessor Starting ===");
        System.out.println("Output dir: " + outputDir);
        
        // Parse classpath and find aot.factories in JAR files
        Set<String> runtimeHintsRegistrars = new HashSet<>();
        Set<String> beanFactoryProcessors = new HashSet<>();
        Set<String> beanRegistrationProcessors = new HashSet<>();
        
        String[] jarPaths = classpath.split(":");
        System.out.println("Scanning " + jarPaths.length + " JAR files for aot.factories...");
        
        int foundCount = 0;
        for (String jarPath : jarPaths) {
            if (jarPath.isEmpty() || !jarPath.endsWith(".jar")) {
                continue;
            }
            
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                continue;
            }
            
            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(AOT_FACTORIES);
                if (entry != null) {
                    foundCount++;
                    System.out.println("Found aot.factories in: " + jarFile.getName());
                    
                    Properties props = new Properties();
                    try (InputStream is = jar.getInputStream(entry)) {
                        props.load(is);
                    }
                    
                    // Extract RuntimeHintsRegistrar implementations
                    String registrars = props.getProperty("org.springframework.aot.hint.RuntimeHintsRegistrar", "");
                    for (String registrar : registrars.split(",")) {
                        String trimmed = registrar.trim();
                        if (!trimmed.isEmpty()) {
                            // Remove backslash line continuations
                            trimmed = trimmed.replace("\\", "");
                            runtimeHintsRegistrars.add(trimmed);
                            System.out.println("  - RuntimeHintsRegistrar: " + trimmed);
                        }
                    }
                    
                    // Extract BeanFactoryInitializationAotProcessor implementations
                    String factoryProcessors = props.getProperty("org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor", "");
                    for (String processor : factoryProcessors.split(",")) {
                        String trimmed = processor.trim();
                        if (!trimmed.isEmpty()) {
                            trimmed = trimmed.replace("\\", "");
                            beanFactoryProcessors.add(trimmed);
                            System.out.println("  - BeanFactoryInitializationAotProcessor: " + trimmed);
                        }
                    }
                    
                    // Extract BeanRegistrationAotProcessor implementations
                    String regProcessors = props.getProperty("org.springframework.beans.factory.aot.BeanRegistrationAotProcessor", "");
                    for (String processor : regProcessors.split(",")) {
                        String trimmed = processor.trim();
                        if (!trimmed.isEmpty()) {
                            trimmed = trimmed.replace("\\", "");
                            beanRegistrationProcessors.add(trimmed);
                            System.out.println("  - BeanRegistrationAotProcessor: " + trimmed);
                        }
                    }
                    
                    // Extract BeanRegistrationExcludeFilter implementations
                    String excludeFilters = props.getProperty("org.springframework.beans.factory.aot.BeanRegistrationExcludeFilter", "");
                    for (String filter : excludeFilters.split(",")) {
                        String trimmed = filter.trim();
                        if (!trimmed.isEmpty()) {
                            trimmed = trimmed.replace("\\", "");
                            // Add exclude filters to reflection config as well
                            beanFactoryProcessors.add(trimmed);
                            System.out.println("  - BeanRegistrationExcludeFilter: " + trimmed);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading JAR file " + jarPath + ": " + e.getMessage());
            }
        }
        
        System.out.println("Found " + foundCount + " aot.factories files");
        System.out.println("Total RuntimeHintsRegistrars: " + runtimeHintsRegistrars.size());
        System.out.println("Total BeanFactoryProcessors: " + beanFactoryProcessors.size());
        System.out.println("Total BeanRegistrationProcessors: " + beanRegistrationProcessors.size());
        
        // Generate configuration files
        generateReflectConfig(runtimeHintsRegistrars, beanFactoryProcessors, beanRegistrationProcessors, outputDir);
        generateResourceConfig(outputDir);
        generateProxyConfig(outputDir);
        generateSerializationConfig(outputDir);
        
        System.out.println("=== AOT factories processing complete ===");
        System.out.println("Generated files in: " + outputDir);
    }
    
    private static void generateReflectConfig(Set<String> registrars, Set<String> factoryProcessors, 
                                               Set<String> regProcessors, String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir, "reflect-config.json");
        Files.createDirectories(outputPath.getParent());
        
        StringBuilder json = new StringBuilder("[\n");
        boolean first = true;
        
        // Add all discovered classes from aot.factories
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(registrars);
        allClasses.addAll(factoryProcessors);
        allClasses.addAll(regProcessors);
        
        // Add Spring Boot internal classes that are always needed
        // These are loaded dynamically by SpringFactoriesLoader
        allClasses.add("org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer");
        allClasses.add("org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer$SharedMetadataReaderFactoryBean");
        allClasses.add("org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer$CachingMetadataReaderFactoryPostProcessor");
        allClasses.add("org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer$ConfigurationClassPostProcessorCustomizingSupplier");
        allClasses.add("org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener");
        allClasses.add("org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener$ConditionEvaluationReportListener");
        allClasses.add("org.springframework.boot.autoconfigure.condition.ConditionEvaluationReportAutoConfigurationImportListener");
        allClasses.add("org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer");
        allClasses.add("org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer$ConfigurationWarningsPostProcessor");
        allClasses.add("org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer$ComponentScanPackageCheck");
        allClasses.add("org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer$Check");
        
        // Add template-related runtime hints if found
        for (String registrar : registrars) {
            if (registrar.contains("FreeMarker")) {
                allClasses.add("org.springframework.boot.autoconfigure.freemarker.FreeMarkerTemplateAvailabilityProvider");
            }
            if (registrar.contains("Groovy")) {
                allClasses.add("org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAvailabilityProvider");
            }
            if (registrar.contains("Jackson")) {
                allClasses.add("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration");
            }
        }
        
        for (String className : allClasses) {
            if (!first) json.append(",\n");
            first = false;
            
            json.append("  {\n");
            json.append("    \"name\": \"").append(className).append("\",\n");
            json.append("    \"allDeclaredConstructors\": true,\n");
            json.append("    \"allPublicConstructors\": true,\n");
            json.append("    \"allDeclaredMethods\": true,\n");
            json.append("    \"allPublicMethods\": true,\n");
            json.append("    \"allDeclaredFields\": true,\n");
            json.append("    \"allPublicFields\": true\n");
            json.append("  }");
        }
        
        json.append("\n]\n");
        
        Files.writeString(outputPath, json.toString());
        System.out.println("Generated: " + outputPath + " with " + allClasses.size() + " classes");
    }
    
    private static void generateResourceConfig(String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir, "resource-config.json");
        
        String json = """
        {
          "resources": {
            "includes": [
              {"pattern": "application.*\\\\.yml"},
              {"pattern": "application.*\\\\.yaml"},
              {"pattern": "application.*\\\\.properties"},
              {"pattern": "META-INF/spring\\\\.factories"},
              {"pattern": "META-INF/spring/.*\\\\.imports"},
              {"pattern": "META-INF/spring/aot\\\\.factories"},
              {"pattern": "META-INF/additional-spring-configuration-metadata\\\\.json"},
              {"pattern": "META-INF/spring-configuration-metadata\\\\.json"},
              {"pattern": "git\\\\.properties"},
              {"pattern": "logback.*\\\\.xml"},
              {"pattern": "log4j2.*\\\\.xml"}
            ]
          },
          "bundles": []
        }
        """;
        
        Files.writeString(outputPath, json);
        System.out.println("Generated: " + outputPath);
    }
    
    private static void generateProxyConfig(String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir, "proxy-config.json");
        
        // Add common Spring proxies
        String json = """
        [
          {
            "interfaces": [
              "org.springframework.aop.SpringProxy",
              "org.springframework.aop.framework.Advised",
              "org.springframework.core.DecoratingProxy"
            ]
          },
          {
            "interfaces": [
              "org.springframework.context.annotation.ConfigurationClassEnhancer$EnhancedConfiguration"
            ]
          }
        ]
        """;
        
        Files.writeString(outputPath, json);
        System.out.println("Generated: " + outputPath);
    }
    
    private static void generateSerializationConfig(String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir, "serialization-config.json");
        
        // Add common serializable classes
        String json = """
        {
          "types": [
          ],
          "lambdaCapturingTypes": [
          ]
        }
        """;
        
        Files.writeString(outputPath, json);
        System.out.println("Generated: " + outputPath);
    }
}