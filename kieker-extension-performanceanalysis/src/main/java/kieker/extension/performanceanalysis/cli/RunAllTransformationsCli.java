package kieker.extension.performanceanalysis.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.nio.file.Path;

@Parameters(commandNames = { "RunAllTransformations" },
        commandDescription = "Runs all transformations with default values.")
public class RunAllTransformationsCli extends Kieker2UmlCli{


    @Parameter(names = {"--transformation-model", "-t"},
            required = true,
            description = "The path to the transformation model file.")
    private Path transformationPath;

    public Path getTransformationPath() {
        return transformationPath;
    }
}
