package hu.mik.prog2.converter.strategy;

import hu.mik.prog2.converter.exception.ConverterException;
import hu.mik.prog2.converter.exception.InvalidFileStructureException;
import hu.mik.prog2.converter.model.Document;
import hu.mik.prog2.converter.model.DocumentHeader;
import hu.mik.prog2.converter.model.DocumentRecord;
import hu.mik.prog2.converter.model.FormatType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete strategy for parsing and serializing XML (Extensible Markup Language).
 * Uses standard Java DOM and Transformer with secure XML parsing configurations.
 */
public class XmlFormatStrategy extends AbstractFormatStrategy {

    public XmlFormatStrategy() {
        super(FormatType.XML);
    }

    @Override
    public Document parse(InputStream inputStream) throws ConverterException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Secure XML processing to prevent XXE attacks
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            // Custom ErrorHandler to prevent Xerces from dumping "[Fatal Error]" to stderr on invalid input
            db.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    // ignore warnings
                }

                @Override
                public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXParseException {
                    throw e;
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXParseException {
                    throw e;
                }
            });
            org.w3c.dom.Document xmlDoc = db.parse(inputStream);
            xmlDoc.getDocumentElement().normalize();

            Element root = xmlDoc.getDocumentElement();
            NodeList recordNodes = root.getElementsByTagName("record");

            if (recordNodes.getLength() == 0) {
                // Also support child elements if root children are directly the records
                recordNodes = root.getChildNodes();
            }

            List<Map<String, String>> objectList = new ArrayList<>();
            Set<String> headerSet = new LinkedHashSet<>();

            for (int i = 0; i < recordNodes.getLength(); i++) {
                Node node = recordNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (!node.getNodeName().equalsIgnoreCase("record") && recordNodes == root.getChildNodes()) {
                    continue;
                }

                Element recordElem = (Element) node;
                NodeList fields = recordElem.getChildNodes();
                Map<String, String> rowMap = new LinkedHashMap<>();

                for (int j = 0; j < fields.getLength(); j++) {
                    Node fNode = fields.item(j);
                    if (fNode.getNodeType() == Node.ELEMENT_NODE) {
                        String colName = fNode.getNodeName();
                        String val = fNode.getTextContent();
                        rowMap.put(colName, val != null ? val : "");
                        headerSet.add(colName);
                    }
                }

                if (!rowMap.isEmpty()) {
                    objectList.add(rowMap);
                }
            }

            if (objectList.isEmpty() || headerSet.isEmpty()) {
                throw new InvalidFileStructureException("XML contains no valid <record> elements with fields.");
            }

            DocumentHeader header = new DocumentHeader(new ArrayList<>(headerSet));
            List<DocumentRecord> records = new ArrayList<>(objectList.size());
            for (Map<String, String> obj : objectList) {
                records.add(new DocumentRecord(obj));
            }

            return new Document(header, records);
        } catch (ParserConfigurationException e) {
            throw new ConverterException("XML parser configuration error: " + e.getMessage(), e);
        } catch (InvalidFileStructureException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileStructureException("Failed to parse XML document: " + e.getMessage(), e);
        }
    }

    @Override
    public void write(Document document, OutputStream outputStream) throws ConverterException {
        validateDocumentForWriting(document);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = db.newDocument();

            Element rootElement = xmlDoc.createElement("document");
            xmlDoc.appendChild(rootElement);

            DocumentHeader header = document.getHeader();
            for (DocumentRecord record : document.getRecords()) {
                Element recordElem = xmlDoc.createElement("record");
                for (int i = 0; i < header.size(); i++) {
                    String col = header.getColumn(i);
                    String safeTag = sanitizeXmlTag(col);
                    Element fieldElem = xmlDoc.createElement(safeTag);
                    fieldElem.setTextContent(record.getValueOrDefault(col, ""));
                    recordElem.appendChild(fieldElem);
                }
                rootElement.appendChild(recordElem);
            }

            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource domSource = new DOMSource(xmlDoc);
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(domSource, streamResult);
        } catch (Exception e) {
            throw new ConverterException("Failed to write XML output: " + e.getMessage(), e);
        }
    }

    private String sanitizeXmlTag(String name) {
        if (name == null || name.isBlank()) return "field";
        String tag = name.trim().replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (!Character.isLetter(tag.charAt(0)) && tag.charAt(0) != '_') {
            tag = "_" + tag;
        }
        return tag;
    }
}
