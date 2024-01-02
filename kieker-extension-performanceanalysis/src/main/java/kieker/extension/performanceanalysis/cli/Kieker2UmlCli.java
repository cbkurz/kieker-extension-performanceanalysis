package kieker.extension.performanceanalysis.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import kieker.extension.performanceanalysis.cli.converters.PathConverter;
import kieker.extension.performanceanalysis.cli.validators.FileIsPresentValidator;
import kieker.extension.performanceanalysis.cli.validators.InputModelValidator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandNames = { "Kieker2Uml" },
            commandDescription = "Accepts kieker traces and transforms them into a uml model. " +
                                    "If a model exists, the results are merged.")
public class Kieker2UmlCli {

    @Parameter(names = {"-m", "--model"},
            description = "The model which is worked on. " +
                    "The input files are parsed and put into this model.",
            converter = PathConverter.class,
            validateWith = InputModelValidator.class
    )
    private Path modelPath = getDefaultModelPath();

    private static Path getDefaultModelPath() {
        final String defaultModelPath = "output/output.uml";
        new InputModelValidator().validate("default modelPath", defaultModelPath);
        return Paths.get(defaultModelPath);
    }

    @Parameter(names = {"-uc", "--use-case"},
            description = "The Name of the use case under which the input files are added. " +
                    "The use-case is added irrespectively if the use-case is already present.",
            required = true
    )
    private String useCaseName;

    @Parameter(names = {"-d", "--directory"},
            variableArity = true,
            description = "A directory or list of directories in which all files will be converted. " +
                    "The directories must contain kieker traces files. " +
                    "The name of the output is the same as the input but with the extension '.uml'",
            converter = PathConverter.class,
            validateWith = FileIsPresentValidator.class
    )
    private List<Path> inputDirectories = new ArrayList<>();


    public List<Path> getInputDirectories() {
        return inputDirectories;
    }

    public Path getModelPath() {
        return modelPath;
    }

    public String getUseCaseName() {
        return useCaseName;
    }
}
