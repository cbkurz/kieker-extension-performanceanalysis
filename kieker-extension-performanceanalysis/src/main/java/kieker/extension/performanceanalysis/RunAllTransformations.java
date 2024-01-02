package kieker.extension.performanceanalysis;

import kieker.extension.performanceanalysis.cli.RunAllTransformationsCli;
import kieker.extension.performanceanalysis.cli.Uml2LqnCli;
import kieker.extension.performanceanalysis.cli.Uml2PlantUmlCli;
import kieker.extension.performanceanalysis.cli.Uml2UmlCli;
import kieker.extension.performanceanalysis.kieker2uml.Kieker2Uml;
import kieker.extension.performanceanalysis.uml2lqn.Uml2Lqn;
import kieker.extension.performanceanalysis.uml2plantuml.Uml2PlantUml;
import kieker.extension.performanceanalysis.uml2uml.UmlCopyAndFilter;
import kieker.extension.performanceanalysis.uml2uml.UmlCopyAndFilter2;
import kieker.extension.performanceanalysis.uml2uml.UmlTransformation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class RunAllTransformations implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunAllTransformations.class);

    private final String[] args;
    private final Path modelPath;
    private final Path transformationModelPath;
    private final boolean deleteOutputFolder;

    public RunAllTransformations(final String[] args, final Path modelPath, final Path transformationPath) {
        LOGGER.debug("Creating RunAllTransformations Object");
        this.args = args;
        this.modelPath = modelPath;
        this.transformationModelPath = transformationPath;
        this.deleteOutputFolder = true;
    }

    @Override
    public void run() {
        LOGGER.debug("Running RunAllTransformations");
        // setup
        final Uml2UmlCli uml2UmlCli = new Uml2UmlCli();
        final Uml2LqnCli uml2LqnCli = new Uml2LqnCli();
        final Uml2PlantUmlCli uml2PlantUmlCli = new Uml2PlantUmlCli();
        final Path futureUmlModel = uml2UmlCli.getUmlOutput();
        final Path outputFolder = futureUmlModel.getParent();

        LOGGER.debug("Successfully created required CLI-Information");

        if (deleteOutputFolder && outputFolder.toFile().exists()) {
            LOGGER.debug(String.format("Deleting '%s' directory...", outputFolder));
            try {
                FileUtils.deleteDirectory(outputFolder.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            LOGGER.debug(String.format("Successfully deleted '%s' directory", outputFolder));
        }

        // transformations
        // Kieker2Uml
        LOGGER.debug("Running Kieker2Uml transformation...");
        final Kieker2Uml kieker2Uml = new Kieker2Uml(args, new RunAllTransformationsCli(), false);
        kieker2Uml.run();
        LOGGER.debug("Successfully run Kieker2Uml transformation.");

        // Uml2PlantUml
        LOGGER.debug("Running Uml2PlantUml transformation for present UML...");
        final Uml2PlantUml uml2PlantPresent = new Uml2PlantUml(modelPath, uml2PlantUmlCli.getOutputPath().resolve("present"));
        uml2PlantPresent.run();

        // Uml2Uml
        LOGGER.debug("Running Uml2Uml transformation...");
        final UmlCopyAndFilter2 umlCopyAndFilter = new UmlCopyAndFilter2(transformationModelPath, modelPath, futureUmlModel);
        final UmlTransformation umlTransformation = new UmlTransformation(futureUmlModel, transformationModelPath);
        umlCopyAndFilter.run();
        umlTransformation.run();

        // Transform the future Uml2PlantUml
        LOGGER.debug("Running Uml2PlantUml transformation for future UML...");
        final Uml2PlantUml uml2PlantTransformed = new Uml2PlantUml(futureUmlModel, uml2PlantUmlCli.getOutputPath().resolve("transformed"));
        uml2PlantTransformed.run();

        // Uml2Lqn
        LOGGER.debug("Running Uml2Lqn transformation...");
        final Uml2Lqn uml2Lqn = new Uml2Lqn(futureUmlModel, uml2LqnCli.getLqnPath());
        uml2Lqn.run();
    }
}
