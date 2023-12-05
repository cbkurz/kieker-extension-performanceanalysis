package kieker.extension.performanceanalysis.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandNames = {"Uml2PlantUml"},
        commandDescription = "Performs a model-to-text transformation. " +
                "Input is a UML model and the output is produced in PlantUml syntax.")
public class Uml2PlantUmlCli {

    @Parameter(names = {"--model", "-m"},
            required = true,
            description = "The path to the UML2 file that shall be transformed.")
    private Path umlPath;

    @Parameter(names = {"--output", "-o"},
            description = "The path to the directory where the output will be produced. " +
                    "Default is in the current directory.")
    private Path outputPath = Paths.get("output");

    public Path getUmlPath() {
        return umlPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }
}
