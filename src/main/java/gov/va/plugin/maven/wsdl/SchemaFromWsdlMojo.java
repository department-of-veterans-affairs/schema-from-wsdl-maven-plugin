package gov.va.plugin.maven.wsdl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Builder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Maven Mojo that writes an embedded schema found in a WSDL to a file.
 *
 * <p>Schema files will have the same name as the associated WSDL but with extension <code>.xsd
 * </code>.
 *
 * <p>The parameter configuration portion of this Mojo is heavily based on jaxws-maven-plugin.
 *
 * <p>However, the configuration for this Mojo has been implemented to meet the bare requirements
 * for the current use case and is much more simplistic than jaxws-maven-plugin. Currently it does
 * not support the whole suite of features for WSDL location although additional features can be
 * added as required.
 */
@Mojo(name = "schema-from-wsdl", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SchemaFromWsdlMojo extends AbstractMojo {

  /** Extension used for extracted schema files. */
  private static final String SCHEMA_FILE_EXTENSION = ".xsd";

  /** A class used to look up .wsdl documents from a given directory. */
  private static final FileFilter WSDL_FILE_FILTER = f -> f.getName().endsWith(".wsdl");

  /**
   * List of files to use for WSDLs. If not specified, all <code>.wsdl</code> files in the <code>
   * wsdlDirectory</code> will be used.
   */
  @Parameter protected List<String> wsdlFiles;

  /** Inject an implementation of a schema provider. */
  @Inject private SimpleEmbeddedSchemaFromWsdlProvider versionProvider;

  /** Directory containing WSDL files. */
  @Parameter(defaultValue = "${project.basedir}/src/wsdl")
  private File wsdlDirectory;

  /** Directory to output schema parsed from WSDL files. */
  @Parameter(defaultValue = "${project.basedir}/src/xsd")
  private File sourceDestDir;

  @Builder
  private SchemaFromWsdlMojo(
      List<String> wsdlFiles,
      File wsdlDirectory,
      File sourceDestDir,
      SimpleEmbeddedSchemaFromWsdlProvider versionProvider) {
    this.wsdlFiles = wsdlFiles;
    this.wsdlDirectory = wsdlDirectory;
    this.sourceDestDir = sourceDestDir;
    this.versionProvider = versionProvider;
  }

  /**
   * Execute the plugin.
   *
   * @throws MojoExecutionException Exception if unexpected condition occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {
    final List<URL> urlList = getWsdlUrlList();
    for (URL url : urlList) {
      String schema = versionProvider.getSchema(url);
      writeSchemaToFile(url, schema);
    }
  }

  /**
   * Get a list of URL for each specified WSDL.
   *
   * @return List of URL.
   * @throws MojoExecutionException Exception if unexpected condition occurs such as if no WSDLs
   *     configured.
   */
  private List<URL> getWsdlUrlList() throws MojoExecutionException {
    final List<URL> urlList = new ArrayList<>();
    if (wsdlFiles == null) {
      // If directory exists try to find wsdls there.
      if (wsdlDirectory.exists()) {
        final File[] wsdls = wsdlDirectory.listFiles(WSDL_FILE_FILTER);
        if ((wsdls != null) && (wsdls.length > 0)) {
          for (final File wsdl : wsdls) {
            urlList.add(urlFromFile(wsdl));
          }
        } else {
          throw new MojoExecutionException(
              "No WSDL found in specified directory: " + wsdlDirectory.getAbsolutePath());
        }
      } else {
        throw new MojoExecutionException(
            "Must specify a WSDL and/or directory to search for WSDL.  "
                + "No WSDL specified and directory does not exist: "
                + wsdlDirectory.getAbsolutePath());
      }
    } else {
      for (final String filename : wsdlFiles) {
        urlList.add(urlFromFilename(filename));
      }
    }
    return urlList;
  }

  /**
   * Get a URL for a WSDL File.
   *
   * @param wsdl WSDL File.
   * @return URL.
   * @throws MojoExecutionException Exception if unexpected condition occurs such as file not found.
   */
  private URL urlFromFile(final File wsdl) throws MojoExecutionException {
    try {
      final URI uri = wsdl.toURI();
      if (Files.exists(Paths.get(uri))) {
        return uri.toURL();
      } else {
        throw new MojoExecutionException("WSDL does not exist: " + wsdl.getAbsolutePath());
      }
    } catch (final MalformedURLException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }

  /**
   * Get a URL for a WSDL filename.
   *
   * @param filename WSDL filename.
   * @return URL.
   * @throws MojoExecutionException Exception if unexpected condition occurs such as file not found.
   */
  private URL urlFromFilename(final String filename) throws MojoExecutionException {
    File wsdl = new File(filename);
    if (!wsdl.isAbsolute()) {
      wsdl = new File(wsdlDirectory, filename);
    }
    return urlFromFile(wsdl);
  }

  /**
   * Output the wsdl associated schema to a file.
   *
   * @param url The url of the wsdl.
   * @param schema String representation of the associated schema.
   */
  private void writeSchemaToFile(final URL url, final String schema) throws MojoExecutionException {
    try {
      Path path = Paths.get(url.toURI());
      Path filePath = path.getFileName();
      if (filePath == null) {
        throw new MojoExecutionException("Unable to obtain path for: " + url.toString());
      }
      String fileName = filePath.toString();
      int index = fileName.lastIndexOf('.');
      if (index > 0) {
        fileName = fileName.substring(0, index);
      }
      fileName += SCHEMA_FILE_EXTENSION;
      File output = new File(sourceDestDir, fileName);
      File parentDirectory = output.getParentFile();
      if (parentDirectory == null) {
        throw new MojoExecutionException(
            "Unable to obtain parent for: " + output.getAbsolutePath());
      }
      if (!parentDirectory.exists() && !output.getParentFile().mkdirs()) {
        throw new MojoExecutionException(
            "Unable to create parent for: " + output.getAbsolutePath());
      }
      System.out.println("Writing schema: " + output.getAbsolutePath());
      Files.write(output.toPath(), schema.getBytes(StandardCharsets.UTF_8));
    } catch (URISyntaxException | IOException | SecurityException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }
}
