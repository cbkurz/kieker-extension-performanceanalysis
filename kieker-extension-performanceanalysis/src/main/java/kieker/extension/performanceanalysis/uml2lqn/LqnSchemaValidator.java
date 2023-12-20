package kieker.extension.performanceanalysis.uml2lqn;

import kieker.extension.performanceanalysis.epsilon.Util;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LqnSchemaValidator {

    private final Validator validator;
    private final XmlErrorHandler errorHandler;

    LqnSchemaValidator() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Source schemaFile = new StreamSource(Util.getResource("metamodels/lqn.xsd").toFile());
        try {
            Schema schema = factory.newSchema(schemaFile);
            this.validator = schema.newValidator();
            this.errorHandler = new XmlErrorHandler();
            validator.setErrorHandler(this.errorHandler);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void validate(final File file) {
        try {
            final StreamSource source = new StreamSource(file);
            validator.validate(source);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (errorHandler.getExceptions().isEmpty()) {
            return;
        }
        final String collect = errorHandler.getExceptions().stream()
                .map(e -> String.format("Line: %s:%s. Message: %s", e.getLineNumber(), e.getColumnNumber(), e.getMessage()))
                .collect(Collectors.joining("\n"));
        throw new RuntimeException(collect);
    }

    static class XmlErrorHandler implements ErrorHandler {

        private List<SAXParseException> exceptions;

        public XmlErrorHandler() {
            this.exceptions = new ArrayList<>();
        }

        public List<SAXParseException> getExceptions() {
            return exceptions;
        }

        @Override
        public void warning(SAXParseException exception) {
            exceptions.add(exception);
        }

        @Override
        public void error(SAXParseException exception) {
            exceptions.add(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) {
            exceptions.add(exception);
        }
    }

}
