package kieker.extension.performanceanalysis.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

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
    private Path modelPath = Paths.get("model-output.uml");

    @Parameter(names = {"-uc", "--use-case"},
            description = "The Name of the use case under which the input files are added. " +
                    "The use-case is added irrespectively if the use-case is already present.",
            required = true
    )
    private String useCaseName;

    @Parameter(names = {"-f", "--file"},
            variableArity = true,
            description = "A file or list of files to parse. " +
                    "These files are converted to a UML model.",
            converter = PathConverter.class,
            validateWith = FileIsPresentValidator.class
    )
    private List<Path> inputFiles = new ArrayList<>();

    @Parameter(names = {"-d", "--directory"},
            variableArity = true,
            description = "A directory or list of directories in which all files will be converted. " +
                    "The name of the output is the same as the input but with the extension '.uml'",
            converter = PathConverter.class,
            validateWith = FileIsPresentValidator.class
    )
    private List<Path> inputDirectories = new ArrayList<>();

    @Parameter(names = {"-R", "--recursive"},
            description = "If this flag is set the given input directories will be searched recursively."
    )
    private boolean recursive = false;


    @Parameter(names = {"-o", "--output"},
            description = "The directory to which the output is written. If this is not set, the output will be written to the current directory.",
            converter = PathConverter.class,
            validateWith = OutputDirectoryValidator.class
    )
    private Path outputDirectory = Paths.get("");

    @Parameter(names = "--help", help = true)
    private boolean help = false;

    public List<Path> getInputFiles() {
        return inputFiles;
    }

    public List<Path> getInputDirectories() {
        return inputDirectories;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isHelp() {
        return help;
    }

    public Path getModelPath() {
        return modelPath;
    }

    public String getUseCaseName() {
        return useCaseName;
    }
}
