package kieker.extension.performanceanalysis.cli.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputDirectoryValidator implements IParameterValidator {
    @Override
    public void validate(final String name, final String value) throws ParameterException {
        final Path path;
        try {
            path = Paths.get(value);
            if (path.toFile().isDirectory()) {
                return; // everything is fine, leave
            }
        } catch (Exception e) {
            throw new ParameterException("Unable to convert string to path, please check the cli argument for '--output' : " + value);
        }


        if (path.toFile().isFile()) {
            throw new ParameterException("The given output path is a file and not a directory. " +
                    "Please provide a directory as output. " +
                    "Value: " + path.toAbsolutePath());
        }

        if (!path.toFile().mkdirs()) {
            throw new ParameterException("Unable to create output directory: " + path.toAbsolutePath());
        }
    }
}
