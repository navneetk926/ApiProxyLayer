import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Pain001Validator {

    // 1. Friendly field names mapping
    private static final Map<String, String> fieldLabels = new HashMap<>();
    static {
        fieldLabels.put("DbtrAcct", "Debtor Account");
        fieldLabels.put("CdtrAcct", "Creditor Account");
        fieldLabels.put("ReqdExctnDt", "Payment Execution Date");
        fieldLabels.put("InstdAmt", "Payment Amount");
        fieldLabels.put("Nm", "Name");
        fieldLabels.put("IBAN", "IBAN Number");
        fieldLabels.put("BIC", "Bank Identifier Code (BIC)");
        fieldLabels.put("PmtInf", "Payment Information Block");
        fieldLabels.put("CdtTrfTxInf", "Credit Transfer Transaction");
    }

    public static void main(String[] args) {
        File schemaFile = new File("pain.001.001.03.xsd"); // Change if you use another version
        File xmlFile = new File("pain001.xml");

        List<String> userMessages = new ArrayList<>();
        List<String> technicalMessages = new ArrayList<>();

        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(schemaFile);
            Validator validator = schema.newValidator();

            // 2. Attach custom error handler
            validator.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException {
                    handleError(e, userMessages, technicalMessages);
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    handleError(e, userMessages, technicalMessages);
                }

                @Override
                public void warning(SAXParseException e) {
                    technicalMessages.add("Warning at line " + e.getLineNumber() + ": " + e.getMessage());
                }
            });

            // 3. Run validation
            validator.validate(new StreamSource(xmlFile));

            // 4. Show results
            if (userMessages.isEmpty()) {
                System.out.println("✅ XML is valid.");
            } else {
                System.out.println("❌ XML validation failed. Issues:");
                userMessages.forEach(msg -> System.out.println(" - " + msg));
            }

            // Optional: log full technical errors for debugging
            if (!technicalMessages.isEmpty()) {
                System.out.println("\nTechnical log (for developers):");
                technicalMessages.forEach(System.out::println);
            }

        } catch (SAXException | IOException e) {
            System.err.println("Validation process failed: " + e.getMessage());
        }
    }

    // 5. Error translator
    private static void handleError(SAXParseException e, List<String> userMessages, List<String> technicalMessages) {
        String raw = e.getMessage();
        String friendly = translateError(raw, e.getLineNumber());
        userMessages.add(friendly);
        technicalMessages.add("Line " + e.getLineNumber() + ": " + raw);
    }

    private static String translateError(String raw, int line) {
        for (String key : fieldLabels.keySet()) {
            if (raw.contains(key)) {
                String field = fieldLabels.get(key);

                if (raw.contains("not expected")) {
                    return field + " is not allowed in this section (line " + line + ").";
                } else if (raw.contains("is not complete")) {
                    return field + " is required but missing (line " + line + ").";
                } else if (raw.contains("xs:date")) {
                    return field + " must be in format YYYY-MM-DD (line " + line + ").";
                } else if (raw.contains("xs:decimal")) {
                    return field + " must be a valid number (line " + line + ").";
                } else if (raw.contains("cannot find the declaration of element 'Document'")) {
                    return "Root element <Document> is invalid. Check ISO20022 version/namespace.";
                }
                return "Problem with " + field + ": " + raw;
            }
        }
        return "Validation error at line " + line + ": " + raw;
    }
}
