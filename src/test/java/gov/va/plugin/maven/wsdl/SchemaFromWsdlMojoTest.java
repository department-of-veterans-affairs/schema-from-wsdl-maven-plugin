package gov.va.plugin.maven.wsdl;

import static java.util.Collections.singletonList;

import java.io.File;
import java.net.MalformedURLException;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Ignore;
import org.junit.Test;

public class SchemaFromWsdlMojoTest {

  String wsdlDirectory = "src/test/resources/wsdl";

  String sourceDestDir = "src/test/resources/xsd";

  SimpleEmbeddedSchemaFromWsdlProvider versionProvider = new SimpleEmbeddedSchemaFromWsdlProvider();

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void directoryDoesNotExist() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlDirectory(new File("")).build();
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
  public void extractWsdlFileName() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList("src/test/resources/wsdl/example.wsdl"))
            .sourceDestDir(new File(sourceDestDir))
            .versionProvider(versionProvider)
            .build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MalformedURLException.class)
  @SneakyThrows
  @Ignore
  public void malformedPathUrl() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList("src/test/resources/xsd/example.wsdl"))
            .build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void noWsdlFound() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder().wsdlDirectory(new File("src/test/resources/xsd")).build();
    schemaFromWsdlMojo.execute();
  }

  @Test(expected = MojoExecutionException.class)
  @SneakyThrows
  public void nullDestinationDirectory() {
    SchemaFromWsdlMojo schemaFromWsdlMojo =
        SchemaFromWsdlMojo.builder()
            .wsdlFiles(singletonList("src/test/resources/wsdl/example.wsdl"))
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
            .wsdlFiles(singletonList("src/test/resources/xsd/example.wsdl"))
            .build();
    schemaFromWsdlMojo.execute();
  }
}
