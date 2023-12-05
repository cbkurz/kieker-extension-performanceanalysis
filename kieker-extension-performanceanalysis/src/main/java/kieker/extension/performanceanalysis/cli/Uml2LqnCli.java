package kieker.extension.performanceanalysis.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.nio.file.Path;
import java.nio.file.Paths;

@Parameters(commandNames = { "Uml2Lqn" },
        commandDescription = "Takes a uml-file that will be transformed to LQN model.")
public class Uml2LqnCli {

    @Parameter(names = {"--model", "-m"},
            required = true,
            description = "The path to the UML2 file that shall be transformed.")
    private Path umlPath;
    @Parameter(names = {"--output", "-o"},
            description = "The path where the lqn output is written to.")
    private Path lqnPath = Paths.get("output", "lqn.xml");

    public Path getUmlPath() {
        return umlPath;
    }

    public Path getLqnPath() {
        return lqnPath;
    }
}
