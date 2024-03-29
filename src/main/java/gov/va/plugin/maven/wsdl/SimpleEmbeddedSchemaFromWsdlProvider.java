package gov.va.plugin.maven.wsdl;

import java.net.URL;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Simple schema provider implementation.
 *
 * <p>This implementation is simplistic and makes the following assumptions:
 *
 * <p>1. wsdl contains an embedded inline schema.
 *
 * <p>2. wsdl only contains a single embedded inline schema.
 */
@Named
@Singleton
@Slf4j
public class SimpleEmbeddedSchemaFromWsdlProvider implements SchemaProvider {
  @Override
  public String getSchema(URL url) throws MojoExecutionException {
    try {
      log.info("Reading WSDL: {}", url.getFile());
      return WsdlUtilities.parseSchemaStringFromWsdl(url);
    } catch (WsdlUtilities.WsdlParseFailedException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }
}
