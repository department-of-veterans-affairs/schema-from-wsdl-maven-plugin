package gov.va.plugin.maven.wsdl;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utilities for parsing wsdl specifically to obtain an embedded schema. This implementation is
 * simplistic although additional features can be added as required.
 *
 * <p>This implementation is simplistic and makes the following assumptions:
 *
 * <p>1. wsdl contains an embedded inline schema.
 *
 * <p>2. wsdl only contains a single embedded inline schema.
 */
public final class WsdlUtilities {

  private static final String SAX_FEATURE_EXTERNAL_GENERAL_ENTITIES =
      "http://xml.org/sax/features/external-general-entities";

  private static final String SAX_FEATURE_EXTERNAL_PARAMETER_ENTITIES =
      "http://xml.org/sax/features/external-parameter-entities";

  private static final String ELEMENT_TAG_SCHEMA = "schema";

  /**
   * Get a document builder factory.
   *
   * @return DocumentBuilderFactory.
   * @throws WsdlParseFailedException Exception if failed to obtain factory.
   */
  private static DocumentBuilderFactory getDocumentBuilderFactory()
      throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(SAX_FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
    factory.setFeature(SAX_FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setNamespaceAware(true);
    return factory;
  }

  /**
   * Obtain a document representation of the wsdl located at URL.
   *
   * @param url The URL of the wsdl.
   * @return A document.
   * @throws WsdlParseFailedException Exceptional condition if problem parsing wsdl occurred.
   */
  private static Document parse(final URL url) throws WsdlParseFailedException {
    try {
      DocumentBuilderFactory factory = getDocumentBuilderFactory();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputSource is = new InputSource(url.toString());
      return builder.parse(is);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new WsdlParseFailedException(e);
    }
  }

  /**
   * Get the primary schema from Wsdl. NOTE: this is a simplistic implementation that only obtains
   * the first schema within a wsdl.
   *
   * @param wsdlDoc A Document representation of the wsdl.
   * @return The first schema node found within the wsdl.
   * @throws WsdlParseFailedException Exception if an unexpected condition occurred during parsing.
   */
  private static org.w3c.dom.Node parseSchemaNodeFromWsdl(final Document wsdlDoc)
      throws WsdlParseFailedException {
    final NodeList schemas =
        wsdlDoc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, ELEMENT_TAG_SCHEMA);
    // NOTE: this simplisitic implementation assumes only one schema in the wsdl.
    if (schemas.getLength() != 1) {
      throw new WsdlParseFailedException("Expected a single schema within the given wsdl.");
    }
    return schemas.item(0);
  }

  /**
   * Get the string representation of an embedded schema within a wsdl.
   *
   * @param url The URL of the wsdl to parse.
   * @return A string.
   * @throws WsdlParseFailedException Exception if unexpected error occurred.
   */
  public static String parseSchemaStringFromWsdl(final URL url) throws WsdlParseFailedException {
    return parseStringFromNode(parseSchemaNodeFromWsdl(parse(url)));
  }

  /**
   * Get the string representation of a dom source.
   *
   * @param domSource The domSource.
   * @return A string.
   * @throws WsdlParseFailedException Exception if unexpected error occurred.
   */
  private static String parseStringFromDomSource(DOMSource domSource)
      throws WsdlParseFailedException {
    try {
      TransformerFactory tf = TransformerFactory.newInstance();
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      Transformer transformer = tf.newTransformer();
      StringWriter writer = new StringWriter();
      StreamResult result = new StreamResult(writer);
      transformer.transform(domSource, result);
      return writer.toString();
    } catch (TransformerException e) {
      throw new WsdlParseFailedException(e);
    }
  }

  /**
   * Get the string representation of a node.
   *
   * @param node The node.
   * @return A string.
   * @throws WsdlParseFailedException Exception if unexpected error occurred.
   */
  private static String parseStringFromNode(org.w3c.dom.Node node) throws WsdlParseFailedException {
    return parseStringFromDomSource(new DOMSource(node));
  }

  /** Encapsulate exceptional conditions with this custom exception. */
  public static class WsdlParseFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    WsdlParseFailedException(String message) {
      super(message);
    }

    WsdlParseFailedException(Exception cause) {
      super(cause);
    }
  }
}
