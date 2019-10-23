package gov.va.plugin.maven.wsdl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SchemaFromWsdlMojoTest {

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

  /** Test the plugin correctly fails when wsdl directory does not exist. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void directoryDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlDirectory(new File("")).build();
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails as expected when a wsdl with multiple schema is encountered. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void extractInvalidWsdlFile() {
    final File sourceDestDir = temporaryFolder.newFolder("extractInvalidWsdlFile");
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(
                singletonList(
                    INVALID_WSDL_RESOURCES_PATH.resolve(WSDL_MULTIPLE_SCHEMA_NAME).toString()))
            .sourceDestDir(sourceDestDir)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  /** Test nominal case of correctly extracted schema from directory. */
  @Test
  @SneakyThrows
  public void extractWsdlDirectory() {
    final File sourceDestDir = temporaryFolder.newFolder("extractWsdlDirectory");
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlDirectory(VALID_WSDL_RESOURCES_PATH.toFile())
            .sourceDestDir(sourceDestDir)
            .versionProvider(versionProvider)
            .build();
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
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList(VALID_WSDL_RESOURCES_PATH.resolve(VALID_WSDL_NAME).toString()))
            .sourceDestDir(sourceDestDir)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
    String actual =
        new String(Files.readAllBytes(sourceDestDir.toPath().resolve(VALID_SCHEMA_NAME)));
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }

  /** Test the plugin fails when a schema is not found. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void noWsdlFound() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlDirectory(TEST_WSDL_RESOURCES_PATH.toFile()).build();
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a destination directory is not specified. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void nullSourceDestDir() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList(VALID_WSDL_RESOURCES_PATH.toString()))
            .sourceDestDir(null)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a specified wsdl does not exist. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlFiles(singletonList("test.wsdl")).build();
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a specified wsdl has no schema. */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlHasNoSchema() {
    final File sourceDestDir = temporaryFolder.newFolder("wsdlHasNoSchema");
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(
                singletonList(INVALID_WSDL_RESOURCES_PATH.resolve(WSDL_NO_SCHEMA_NAME).toString()))
            .sourceDestDir(sourceDestDir)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  /** Test the plugin fails when a specified wsdl is invalid (can not be parsed). */
  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlInvalid() {
    final File sourceDestDir = temporaryFolder.newFolder("wsdlInvalid");
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(
                singletonList(INVALID_WSDL_RESOURCES_PATH.resolve(INVALID_WSDL_NAME).toString()))
            .sourceDestDir(sourceDestDir)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }
}
