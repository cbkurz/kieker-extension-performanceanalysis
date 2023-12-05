package kieker.extension.performanceanalysis.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandNames = { "Uml2Uml" },
        commandDescription = "Takes a uml-model and transformation model and transforms the input uml-model to a future uml." +
                "The future uml model contains only the elements specified in the transformation model and is transformed by the transformation rules.")
public class Uml2UmlCli {
    @Parameter(names = {"--model", "-m"},
            required = true,
            description = "The path to the UML2 file that shall be transformed.")
    private Path umlPath;

    @Parameter(names = {"--transformation-model", "-t"},
            required = true,
            description = "The path to the transformation model file.")
    private Path transformationPath;

    @Parameter(names = {"--output", "-o"},
            description = "The path where the result shall be stored.")
    private Path umlOutput = Paths.get("output", "FutureUml.uml");

    public Path getUmlPath() {
        return umlPath;
    }

    public Path getTransformationPath() {
        return transformationPath;
    }

    public Path getUmlOutput() {
        return umlOutput;
    }
}
