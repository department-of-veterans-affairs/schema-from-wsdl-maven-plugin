package gov.va.plugin.maven.wsdl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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
@NoArgsConstructor
@Mojo(
  name = "schema-from-wsdl",
  defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
@Slf4j
public class SchemaFromWsdlMojo extends AbstractMojo {

  /** Extension used for extracted schema files. */
  private static final String SCHEMA_FILE_EXTENSION = ".xsd";

  /** A class used to look up .wsdl documents from a given directory. */
  private static final FileFilter WSDL_FILE_FILTER = f -> f.getName().endsWith(".wsdl");

  /** The Maven Project Object. */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  @Setter
  private MavenProject project;

  /**
   * List of files to use for WSDLs. If not specified, all <code>.wsdl</code> files in the <code>
   * wsdlDirectory</code> will be used.
   */
  @Parameter @Setter private List<String> wsdlFiles;

  /** Inject an implementation of a schema provider. */
  @Inject @Setter private SimpleEmbeddedSchemaFromWsdlProvider versionProvider;

  /** Directory containing WSDL files. */
  @Parameter(defaultValue = "${project.basedir}/src/wsdl")
  @Setter
  private File wsdlDirectory;

  /** Optional groupId:artifactId reference to project dependency to look for WSDL resources. */
  @Parameter @Setter private String wsdlDependency;

  /** Directory to output schema parsed from WSDL files. */
  @Parameter(defaultValue = "${project.basedir}/src/xsd")
  @Setter
  private File sourceDestDir;

  /**
   * Execute the plugin.
   *
   * @throws MojoExecutionException Exception if unexpected condition occurs.
   */
  @Override
  public void execute() throws MojoExecutionException {

    List<URL> urlList;
    if (wsdlDependency != null) {
      urlList = getWsdlFromClasspathUrlList();
    } else {
      urlList = getWsdlFromDirectoryUrlList();
    }

    if (urlList.isEmpty()) {
      log.warn("No wsdl found.");
    }

    for (URL url : urlList) {
      writeSchemaToFile(url, versionProvider.getSchema(url));
    }
  }

  /**
   * Get a list of URL for each specified WSDL from the specified dependency.
   *
   * @return List of URL.
   * @throws MojoExecutionException Exception if unexpected condition occurs such as if no WSDLs
   *     found.
   */
  private List<URL> getWsdlFromClasspathUrlList() throws MojoExecutionException {

    if ((wsdlFiles == null) || wsdlFiles.isEmpty()) {
      throw new MojoExecutionException(
          "Must specify at least one wsdl file if WSDL dependency is specified.");
    }

    // Parse the referenced dependency to obtain groupId:artifactId.
    final String[] wsdlDependencyArray = wsdlDependency.split(":");
    if (wsdlDependencyArray.length != 2) {
      throw new MojoExecutionException("WSDL dependency invalid: " + wsdlDependency);
    }
    final String groupId = wsdlDependencyArray[0];
    final String artifactId = wsdlDependencyArray[1];

    // Match the dependency from the project.  The resulting list should only be size of 1.
    final List<String> cpList =
        project
            .getArtifacts()
            .stream()
            .filter(
                a ->
                    (groupId.equals(a.getGroupId())
                        && artifactId.equals(a.getArtifactId())
                        && null != a.getFile()))
            .map(a -> a.getFile().getPath())
            .collect(Collectors.toList());
    if (cpList.size() != 1) {
      throw new MojoExecutionException(
          "Expected only 1 but found " + cpList.size() + " matching WSDL dependency.");
    }

    // Convert the path to URL.
    URL url = null;
    try {
      url = new File(cpList.get(0)).toURI().toURL();
    } catch (final MalformedURLException e) {
      throw new MojoExecutionException("Error obtaining WSDL dependency classpath URL: ", e);
    }

    // Using the URL and classloader obtain URL of WSDL resources.
    final List<URL> urlList = new ArrayList<>();
    if (url != null) {
      try (URLClassLoader loader = new URLClassLoader(new URL[] {url})) {
        for (final String wsdlResource : wsdlFiles) {
          final URL loadedResourceUrl = loader.getResource(wsdlResource);
          if (loadedResourceUrl == null) {
            throw new MojoExecutionException("Wsdl resource not found: " + wsdlResource);
          }
          urlList.add(loadedResourceUrl);
        }
      } catch (IOException e) {
        throw new MojoExecutionException(e.getMessage());
      }
    }

    return urlList;
  }

  /**
   * Get a list of URL for each specified WSDL.
   *
   * @return List of URL.
   * @throws MojoExecutionException Exception if unexpected condition occurs such as if no WSDLs
   *     found.
   */
  private List<URL> getWsdlFromDirectoryUrlList() throws MojoExecutionException {
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
      String fileName = new File(url.getPath()).getName();
      final int index = fileName.lastIndexOf('.');
      if (index > 0) {
        fileName = fileName.substring(0, index);
      }
      fileName += SCHEMA_FILE_EXTENSION;
      final File output = new File(sourceDestDir, fileName);
      final File parentDirectory = output.getParentFile();
      if (parentDirectory == null) {
        throw new MojoExecutionException(
            "Unable to obtain parent for: " + output.getAbsolutePath());
      }
      if (!parentDirectory.exists() && !output.getParentFile().mkdirs()) {
        throw new MojoExecutionException(
            "Unable to create parent for: " + output.getAbsolutePath());
      }
      log.info("Writing schema: {}", output.getAbsolutePath());
      Files.write(output.toPath(), schema.getBytes(StandardCharsets.UTF_8));
    } catch (IOException | SecurityException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }
}
