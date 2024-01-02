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

    @Parameter(names = {"--omit-plantuml"},
            description = "Flag to omit the transformation of the uml into the PlantUml Syntax")
    private boolean omitUml2PlantUml = false;

    public Path getTransformationPath() {
        return transformationPath;
    }

    public boolean isOmitUml2PlantUml() {
        return omitUml2PlantUml;
    }
}
