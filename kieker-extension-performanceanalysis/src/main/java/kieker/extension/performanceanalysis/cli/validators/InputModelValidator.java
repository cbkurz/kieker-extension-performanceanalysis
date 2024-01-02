package kieker.extension.performanceanalysis.cli.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.createModel;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.loadModel;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.saveModel;

public class InputModelValidator implements IParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InputModelValidator.class);

    @Override
    public void validate(final String name, final String value) throws ParameterException {
        if (!value.endsWith(UMLResource.FILE_EXTENSION)) {
            throw new ParameterException("File does not follow the UML convention to end with '.uml': " + value);
        }

        final Path path = Paths.get(value);

        // check if exists or can be created
        if (!path.toFile().exists()) {
            try {
                final Model model = createModel(path.getFileName().toString());
                saveModel(model, path);
            } catch (Exception e) {
                final String message = "File does not exist for paramter '" + name + "' and creating the respective model has failed: " + value;
                LOGGER.error(message, e);
                throw new ParameterException(message, e);
            }
            return;
        }

        // check if the model can be loaded.
        try {
            loadModel(path);
        } catch (Exception e) {
            LOGGER.error("An Error occurred while loading", e);
            throw new ParameterException("Unable to load model: " + value, e);
        }
    }
}
