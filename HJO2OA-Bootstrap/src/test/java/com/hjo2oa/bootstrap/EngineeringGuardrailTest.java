package com.hjo2oa.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EngineeringGuardrailTest {

    private static final Pattern FLYWAY_VERSION = Pattern.compile("^V(\\d+)__.*\\.sql$");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+(?:dbo\\.)?([A-Za-z0-9_]+)"
    );
    private static final Pattern REQUEST_MAPPING_LINE = Pattern.compile("@RequestMapping\\(([^\\n]+)");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern LEGACY_FRONTEND_PATH = Pattern.compile(
            "['\"]/(api/(auth|org-perm|v1/(org-perm|data-services|workflow|wf|admin/wf))|auth|org-perm"
                    + "|data-services|workflow|v1/(org-perm|data-services|workflow|wf|admin/wf))"
    );
    private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private static final Pattern INTERNAL_DEPENDENCY = Pattern.compile(
            "(?s)<dependency>\\s*<groupId>com\\.hjo2oa</groupId>\\s*<artifactId>([^<]+)</artifactId>.*?</dependency>"
    );
    private static final Pattern IMPORT_LINE = Pattern.compile("(?m)^import\\s+(com\\.hjo2oa\\.[^;]+);");
    private static final List<String> VERSIONED_API_EXCEPTIONS = List.of("/api/open/**");
    private static final Path PROJECT_ROOT = findProjectRoot();

    @Test
    void flywayMigrationsMustHaveUniqueVersionsAndObjects() throws IOException {
        Path migrationDir = projectRoot().resolve("HJO2OA-Bootstrap/src/main/resources/db/migration/sqlserver");
        Map<String, Path> versions = new HashMap<>();
        Map<String, Path> tables = new HashMap<>();
        List<String> violations = new ArrayList<>();

        try (var stream = Files.list(migrationDir)) {
            for (Path file : stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList()) {
                String fileName = file.getFileName().toString();
                Matcher versionMatcher = FLYWAY_VERSION.matcher(fileName);
                if (versionMatcher.matches()) {
                    String version = versionMatcher.group(1);
                    Path existing = versions.putIfAbsent(version, file);
                    if (existing != null) {
                        violations.add("Duplicate Flyway version V" + version + ": " + existing.getFileName()
                                + " and " + fileName);
                    }
                }

                Matcher tableMatcher = CREATE_TABLE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (tableMatcher.find()) {
                    String tableName = tableMatcher.group(1).toLowerCase(Locale.ROOT);
                    Path existing = tables.putIfAbsent(tableName, file);
                    if (existing != null) {
                        violations.add("Duplicate table " + tableName + ": " + existing.getFileName()
                                + " and " + fileName);
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void controllerPrimaryRequestMappingsMustUseVersionedApiContract() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : mainJavaFiles()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (!content.contains("@RestController")) {
                continue;
            }
            Matcher mappingMatcher = REQUEST_MAPPING_LINE.matcher(content);
            while (mappingMatcher.find()) {
                Matcher literalMatcher = STRING_LITERAL.matcher(mappingMatcher.group(1));
                if (!literalMatcher.find()) {
                    continue;
                }
                String primaryPath = literalMatcher.group(1);
                if (primaryPath.startsWith("/api/") && !primaryPath.startsWith("/api/v1/")
                        && !VERSIONED_API_EXCEPTIONS.contains(primaryPath)) {
                    violations.add(projectRoot().relativize(file) + " primary mapping is " + primaryPath);
                }
            }
        }

        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void frontendServicesMustNotUseLegacyApiGatewayPaths() throws IOException {
        Path servicesRoot = projectRoot().resolve("frontend/apps/portal-web/src");
        List<String> violations = new ArrayList<>();

        try (var stream = Files.walk(servicesRoot)) {
            for (Path file : stream
                    .filter(path -> path.getFileName().toString().endsWith(".ts")
                            || path.getFileName().toString().endsWith(".tsx"))
                    .filter(path -> normalizedPath(path).contains("/services/"))
                    .toList()) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                Matcher matcher = LEGACY_FRONTEND_PATH.matcher(content);
                while (matcher.find()) {
                    violations.add(projectRoot().relativize(file) + " contains legacy API path near "
                            + matcher.group());
                }
            }
        }

        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void moduleDependenciesMustNotReverseCoreOrDirectlyAccessOtherDomainImplementations() throws IOException {
        List<String> violations = new ArrayList<>();

        try (var stream = Files.walk(projectRoot())) {
            for (Path pom : stream
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !normalizedPath(path).contains("/target/"))
                    .toList()) {
                String content = Files.readString(pom, StandardCharsets.UTF_8);
                String artifactId = projectArtifactId(content);
                if (artifactId == null || "hjo2oa".equals(artifactId)) {
                    continue;
                }
                String moduleRoot = moduleRootOf(pom);
                Matcher dependencyMatcher = INTERNAL_DEPENDENCY.matcher(content);
                while (dependencyMatcher.find()) {
                    String dependency = dependencyMatcher.group(1);
                    if ("HJO2OA-Bootstrap".equals(dependency)) {
                        violations.add(projectRoot().relativize(pom) + " depends on Bootstrap");
                    }
                    if ("HJO2OA-Shared".equals(artifactId) && dependency.startsWith("HJO2OA-")) {
                        violations.add(projectRoot().relativize(pom) + " makes Shared depend on " + dependency);
                    }
                }
            }
        }

        for (Path file : mainJavaFiles()) {
            String sourceRoot = moduleRootOf(file);
            Matcher importMatcher = IMPORT_LINE.matcher(Files.readString(file, StandardCharsets.UTF_8));
            while (importMatcher.find()) {
                String importedClass = importMatcher.group(1);
                if (!directlyImportsImplementationLayer(importedClass)) {
                    continue;
                }
                String targetRoot = moduleRootForPackage(importedClass);
                if (targetRoot != null && !sourceRoot.equals(targetRoot)) {
                    violations.add(projectRoot().relativize(file)
                            + " directly imports cross-domain implementation/interface " + importedClass);
                }
            }
        }

        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    private static List<Path> mainJavaFiles() throws IOException {
        try (var stream = Files.walk(projectRoot())) {
            return stream
                    .filter(path -> normalizedPath(path).contains("/src/main/java/"))
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }

    private static Path projectRoot() {
        return PROJECT_ROOT;
    }

    private static Path findProjectRoot() {
        Path current = Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("frontend/apps/portal-web/src"))
                    && Files.exists(current.resolve("HJO2OA-Bootstrap/src/main/resources/db/migration/sqlserver"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of(System.getProperty("maven.multiModuleProjectDirectory", ".")).toAbsolutePath().normalize();
    }

    private static String normalizedPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String firstMatch(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String projectArtifactId(String content) {
        return firstMatch(ARTIFACT_ID, content.replaceFirst("(?s)<parent>.*?</parent>", ""));
    }

    private static String moduleRootOf(Path pom) {
        Path relative = projectRoot().relativize(pom);
        return relative.getNameCount() == 0 ? "" : relative.getName(0).toString();
    }

    private static boolean directlyImportsImplementationLayer(String importedClass) {
        return importedClass.contains(".infrastructure.") || importedClass.contains(".interfaces.");
    }

    private static String moduleRootForPackage(String importedClass) {
        if (importedClass.startsWith("com.hjo2oa.org.")) {
            return "HJO2OA-OrgPerm";
        }
        if (importedClass.startsWith("com.hjo2oa.wf.")) {
            return "HJO2OA-WorkflowForm";
        }
        if (importedClass.startsWith("com.hjo2oa.content.")) {
            return "HJO2OA-Content";
        }
        if (importedClass.startsWith("com.hjo2oa.portal.")) {
            return "HJO2OA-Portal";
        }
        if (importedClass.startsWith("com.hjo2oa.collaboration.")) {
            return "HJO2OA-Collaboration";
        }
        if (importedClass.startsWith("com.hjo2oa.msg.")) {
            return "HJO2OA-Messaging";
        }
        if (importedClass.startsWith("com.hjo2oa.data.")) {
            return "HJO2OA-DataServices";
        }
        return null;
    }
}
