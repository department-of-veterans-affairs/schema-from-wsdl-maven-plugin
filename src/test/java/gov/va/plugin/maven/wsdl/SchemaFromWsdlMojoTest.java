package gov.va.plugin.maven.wsdl;

import static java.util.Collections.singletonList;

import java.io.File;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

public class SchemaFromWsdlMojoTest {

  String wsdlDirectory = "src/test/resources/wsdl/valid";

  String sourceDestDir = "src/test/resources/xsd";

  SimpleEmbeddedSchemaFromWsdlProvider versionProvider = new SimpleEmbeddedSchemaFromWsdlProvider();

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
            .wsdlFiles(singletonList("src/test/resources/wsdl/invalid/multiple-schemas.wsdl"))
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
            .wsdlDirectory(new File(wsdlDirectory))
            .sourceDestDir(new File(sourceDestDir))
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  @Test
  @SneakyThrows
  public void extractWsdlFile() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList("src/test/resources/wsdl/valid/valid.wsdl"))
            .sourceDestDir(new File(sourceDestDir))
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
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
  public void test() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList(wsdlDirectory))
            .sourceDestDir(null)
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void wsdlDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList("src/test/resources/xsd/test.wsdl"))
            .build();
    schemaFromWsdlMojo.execute();
  }
}
