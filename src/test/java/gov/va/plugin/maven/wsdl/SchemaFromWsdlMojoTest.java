package gov.va.plugin.maven.wsdl;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.After;
import org.junit.Test;

public class SchemaFromWsdlMojoTest {

  String validWsdlDirectory = "src/test/resources/wsdl/valid/";

  String invalidWsdlDirectory = "src/test/resources/wsdl/invalid/";

  String sourceDestDir = "src/test/resources/xsd/";

  SimpleEmbeddedSchemaFromWsdlProvider versionProvider = new SimpleEmbeddedSchemaFromWsdlProvider();

  @After
  public void _cleanup() {
    if (Paths.get(sourceDestDir + "valid.xsd").toFile().exists()) {
      final boolean deleted = Paths.get(sourceDestDir + "valid.xsd").toFile().delete();
      assertThat(deleted).isTrue();
    }
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void directoryDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlDirectory(new File("")).build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void extractInvalidWsdlFile() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList(invalidWsdlDirectory + "multiple-schemas.wsdl"))
            .sourceDestDir(new File(sourceDestDir))
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  @Test
  @SneakyThrows
  public void extractWsdlDirectory() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlDirectory(new File(validWsdlDirectory))
            .sourceDestDir(new File(sourceDestDir))
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
    String actual = new String(Files.readAllBytes(Paths.get(sourceDestDir + "valid.xsd")));
    String expected = new String(Files.readAllBytes(Paths.get(sourceDestDir + "expected.xsd")));
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }

  @Test
  @SneakyThrows
  public void extractWsdlFile() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList(validWsdlDirectory + "valid.wsdl"))
            .sourceDestDir(new File(sourceDestDir))
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
    String actual = new String(Files.readAllBytes(Paths.get(sourceDestDir + "valid.xsd")));
    String expected = new String(Files.readAllBytes(Paths.get(sourceDestDir + "expected.xsd")));
    assertThat(actual).isEqualToIgnoringWhitespace(expected);
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void noWsdlFound() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlDirectory(new File("src/test/resources/wsdl")).build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void nullSourceDestDir() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList(validWsdlDirectory))
            .sourceDestDir(null)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlFiles(singletonList("test.wsdl")).build();
    schemaFromWsdlMojo.execute();
  }
}
