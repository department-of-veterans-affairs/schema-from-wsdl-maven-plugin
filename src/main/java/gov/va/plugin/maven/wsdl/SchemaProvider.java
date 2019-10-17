package gov.va.plugin.maven.wsdl;

import java.net.URL;
import org.apache.maven.plugin.MojoExecutionException;

/** Interface a schema provider should implement. */
public interface SchemaProvider {
  String getSchema(URL url) throws MojoExecutionException;
}
