package kieker.extension.performanceanalysis.cli.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.Paths;

public class FileIsPresentValidator implements IParameterValidator {

    @Override
    public void validate(final String name, final String value) throws ParameterException {
        if (!Paths.get(value).toFile().exists()) {
            throw new ParameterException("File or Directory cannot be found: " + name);
        }
    }
}
