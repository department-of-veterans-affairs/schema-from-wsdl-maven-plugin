package gov.va.plugin.maven.wsdl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import lombok.SneakyThrows;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SchemaFromWsdlMojoTest {

  /** Test wsdl dependency group id. */
  private static final String WSDL_DEPENDENCY_GROUP_ID = "groupId";

  /** Test wsdl dependency artifact id. */
  private static final String WSDL_DEPENDENCY_ARTIFACT_ID = "artifactId";

  /** Test wsdl dependency. */
  private static final String WSDL_DEPENDENCY =
      WSDL_DEPENDENCY_GROUP_ID + ":" + WSDL_DEPENDENCY_ARTIFACT_ID;

  /** Test valid wsdl dependency filename. */
  private static final String VALID_JAR_NAME = "valid.jar";

  /** Expected schema filename. */
  private static final String VALID_SCHEMA_NAME = "valid.xsd";

  /** Test wsdl filenames. */
  private static final String VALID_WSDL_NAME = "valid.wsdl";

  private static final String WSDL_MULTIPLE_SCHEMA_NAME = "multiple-schemas.wsdl";

  private static final String WSDL_NO_SCHEMA_NAME = "no-schema.wsdl";

  private static final String INVALID_WSDL_NAME = "invalid.wsdl";

  /** Base test resources path. */
  private static final Path TEST_RESOURCES = Paths.get("src", "test", "resources");

  /** Base wsdl resources path. */
  private static final Path TEST_WSDL_RESOURCES_PATH = TEST_RESOURCES.resolve("wsdl");

  /** Path containing valid wsdl. */
  private static final Path VALID_WSDL_RESOURCES_PATH = TEST_WSDL_RESOURCES_PATH.resolve("valid");

  /** Path containing invalid wsdl. */
  private static final Path INVALID_WSDL_RESOURCES_PATH =
      TEST_WSDL_RESOURCES_PATH.resolve("invalid");

  /** Path for the expected schema to be extracted from the valid schema. */
  private static final Path EXPECTED_SCHEMA_RESOURCE_PATH =
      TEST_RESOURCES.resolve("xsd").resolve(VALID_SCHEMA_NAME);

  /**
   * The content of the expected schema for comparison to the schema extracted from the valid wsdl.
   */
  private static String expected;

  /** Provider the plugin will use during test execution. */
  private final SimpleEmbeddedSchemaFromWsdlProvider versionProvider =
      new SimpleEmbeddedSchemaFromWsdlProvider();

  /** Temporary folder for the plugin destination directory. */
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  /** Before class execution load the content of the expected schema. */
  @BeforeClass
  @SneakyThrows
  public static void loadExpectedSchemaString() {
    expected = new String(Files.readAllBytes(EXPECTED_SCHEMA_RESOURCE_PATH));
  }

  /**
   * Instantiate a Maven project that references the previously built temporary jar as a dependency
   * artifact.
   *
   * @return MavenProject.
   */
  private MavenProject buildMockMavenProject(final File jarDestFile) {
    final MavenProject project = new MavenProject();
    final Artifact artifact =
        new DefaultArtifact(
            WSDL_DEPENDENCY_GROUP_ID,
            WSDL_DEPENDENCY_ARTIFACT_ID,
            "1.0.0",
            "compile",
            "type",
            "classifier",
            null);
    artifact.setFile(jarDestFile);
    project.setArtifacts(Set.of(artifact));
    return project;
  }

  /**
   * Build a temporary dependency jar to test loading a wsdl from resource.
   *
   * @param jarDestDir Temporary jar directory.
   * @return File location of temporary jar.
   */
  @SneakyThrows
  private File buildTemporaryDependencyJar(final File jarDestDir) {
    final File jarDestFile = jarDestDir.toPath().resolve(VALID_JAR_NAME).toFile();
    try (FileOutputStream fos = new FileOutputStream(jarDestFile)) {
      try (JarOutputStream jos = new JarOutputStream(fos)) {
        try (BufferedOutputStream bos = new BufferedOutputStream(jos)) {
          BufferedReader br =
              new BufferedReader(
                  new FileReader(VALID_WSDL_RESOURCES_PATH.resolve(VALID_WSDL_NAME).toFile()));
          jos.putNextEntry(new JarEntry(VALID_WSDL_NAME));
          int c;
          while ((c = br.read()) != -1) {
            bos.write(c);
          }
        }
      }
    }
    return jarDestFile;
  }

  /** Test the plugin correctly fails when wsdl directory does not exist. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void directoryDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlDirectory(new File("invalid"));
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails as expected when a wsdl with multiple schema is encountered. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void extractInvalidWsdlFile() {
    final File sourceDestDir = temporaryFolder.newFolder("extractInvalidWsdlFile");
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlFiles(
        singletonList(INVALID_WSDL_RESOURCES_PATH.resolve(WSDL_MULTIPLE_SCHEMA_NAME).toString()));
    schemaFromWsdlMojo.sourceDestDir(sourceDestDir);
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
  }

  /** Test nominal case of correctly extracted schema from dependency wsdl. */
  @Test
  @SneakyThrows
  public void extractWsdlDependency() {
    // Build a maven project with test dependency that contains a wsdl.
    final File jarDestDir = temporaryFolder.newFolder("jar");
    final MavenProject project = buildMockMavenProject(buildTemporaryDependencyJar(jarDestDir));
    // Execute the mojo.
    final File sourceDestDir = temporaryFolder.newFolder("extractWsdlDependency");
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.project(project);
    schemaFromWsdlMojo.wsdlDependency(WSDL_DEPENDENCY);
    schemaFromWsdlMojo.wsdlFiles(singletonList(VALID_WSDL_NAME));
    schemaFromWsdlMojo.sourceDestDir(sourceDestDir);
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
    final String actual =
        new String(Files.readAllBytes(sourceDestDir.toPath().resolve(VALID_SCHEMA_NAME)));
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }

  /** Test nominal case of correctly extracted schema from directory. */
  @Test
  @SneakyThrows
  public void extractWsdlDirectory() {
    final File sourceDestDir = temporaryFolder.newFolder("extractWsdlDirectory");
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlDirectory(VALID_WSDL_RESOURCES_PATH.toFile());
    schemaFromWsdlMojo.sourceDestDir(sourceDestDir);
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
    String actual =
        new String(Files.readAllBytes(sourceDestDir.toPath().resolve(VALID_SCHEMA_NAME)));
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }

  /** Test nominal case of correctly extracted schema specified by file path. */
  @Test
  @SneakyThrows
  public void extractWsdlFile() {
    final File sourceDestDir = temporaryFolder.newFolder("extractWsdlFile");
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlFiles(
        singletonList(VALID_WSDL_RESOURCES_PATH.resolve(VALID_WSDL_NAME).toString()));
    schemaFromWsdlMojo.sourceDestDir(sourceDestDir);
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
    String actual =
        new String(Files.readAllBytes(sourceDestDir.toPath().resolve(VALID_SCHEMA_NAME)));
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }

  /** Test case where an invalid dependency is specified. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void invalidDependencySpecified() {
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlDependency("invalid");
    schemaFromWsdlMojo.wsdlFiles(singletonList(VALID_WSDL_NAME));
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a schema is not found. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void noWsdlFound() {
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlDirectory(TEST_WSDL_RESOURCES_PATH.toFile());
    schemaFromWsdlMojo.execute();
  }

  /** Test case where no wsdl is specified. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void noWsdlSpecified() {
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlDependency(WSDL_DEPENDENCY);
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a destination directory is not specified. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void nullSourceDestDir() {
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlFiles(singletonList(VALID_WSDL_RESOURCES_PATH.toString()));
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
  }

  /** Test case where specified dependency is not found in the project. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void unmatchedDependency() {
    // Build a maven project with test dependency that contains a wsdl.
    final File jarDestDir = temporaryFolder.newFolder("jar");
    final MavenProject project = buildMockMavenProject(buildTemporaryDependencyJar(jarDestDir));
    // Execute the mojo.
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.project(project);
    schemaFromWsdlMojo.wsdlDependency("no:match");
    schemaFromWsdlMojo.wsdlFiles(singletonList(VALID_WSDL_NAME));
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a specified wsdl does not exist. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlFiles(singletonList("test.wsdl"));
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a specified wsdl has no schema. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlHasNoSchema() {
    final File sourceDestDir = temporaryFolder.newFolder("wsdlHasNoSchema");
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlFiles(
        singletonList(INVALID_WSDL_RESOURCES_PATH.resolve(WSDL_NO_SCHEMA_NAME).toString()));
    schemaFromWsdlMojo.sourceDestDir(sourceDestDir);
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a specified wsdl is invalid (can not be parsed). */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlInvalid() {
    final File sourceDestDir = temporaryFolder.newFolder("wsdlInvalid");
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.wsdlFiles(
        singletonList(INVALID_WSDL_RESOURCES_PATH.resolve(INVALID_WSDL_NAME).toString()));
    schemaFromWsdlMojo.sourceDestDir(sourceDestDir);
    schemaFromWsdlMojo.versionProvider(versionProvider);
    schemaFromWsdlMojo.execute();
  }

  /** Test case where specified wsdl is not found in dependency. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlNotFound() {
    // Build a maven project with test dependency that contains a wsdl.
    final File jarDestDir = temporaryFolder.newFolder("jar");
    final MavenProject project = buildMockMavenProject(buildTemporaryDependencyJar(jarDestDir));
    // Execute the mojo.
    SchemaFromWsdlMojo schemaFromWsdlMojo = new SchemaFromWsdlMojo();
    schemaFromWsdlMojo.project(project);
    schemaFromWsdlMojo.wsdlDependency(WSDL_DEPENDENCY);
    schemaFromWsdlMojo.wsdlFiles(singletonList("invalid.wsdl"));
    schemaFromWsdlMojo.execute();
  }
}
