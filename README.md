# schema-from-wsdl-maven-plugin

Maven Plugin to extract an inline embedded schema found in specified WSDL(s).

Schema files will have the same name as the associated WSDL but with extension `.xsd`
 
The parameter configuration portion of this Mojo is heavily based on `jaxws-maven-plugin`.
 
However, the configuration for this Mojo has been implemented to meet the bare requirements for the current use case and is much more simplistic than `jaxws-maven-plugin`. Currently it does not support the whole suite of features for WSDL location although additional features can be added as required.

## Usage

Add build plugin to `pom.xml` as in the `usage-example`:

```
<build>
    <plugins>
        <plugin>
            <groupId>gov.va.plugin.maven</groupId>
            <artifactId>schema-from-wsdl-maven-plugin</artifactId>
            <version>${schema-from-wsdl-maven-plugin.version}</version>
            <executions>
                <execution>
                    <phase>generate-sources</phase>
                    <goals>
                        <goal>schema-from-wsdl</goal>
                    </goals>
                    <configuration>
                        <wsdlDirectory>src/main/resources/META-INF/wsdl/</wsdlDirectory>
                        <wsdlFiles>
                            <wsdlFile>example.wsdl</wsdlFile>
                        </wsdlFiles>
                        <sourceDestDir>${basedir}/target/classes/META-INF/wsdl</sourceDestDir>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Example

A usage example is shown in sub-project `usage-example`.

Navigate to the example project and build.  Note the extracted schema found in the target directory.

```
cd usage-example
mvn clean generate-sources
```

## Known Issues/Concerns

1. Utilities for parsing wsdl specifically to obtain an embedded schema. This implementation is simplistic although additional features can be added as required. 

   This implementation is simplistic and makes the following assumptions:
   * wsdl contains an embedded inline schema.
   * wsdl only contains a single embedded inline schema..
