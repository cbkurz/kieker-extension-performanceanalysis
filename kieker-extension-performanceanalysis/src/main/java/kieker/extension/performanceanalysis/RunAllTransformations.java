package kieker.extension.performanceanalysis;

import kieker.extension.performanceanalysis.cli.RunAllTransformationsCli;
import kieker.extension.performanceanalysis.cli.Uml2LqnCli;
import kieker.extension.performanceanalysis.cli.Uml2PlantUmlCli;
import kieker.extension.performanceanalysis.cli.Uml2UmlCli;
import kieker.extension.performanceanalysis.kieker2uml.Kieker2Uml;
import kieker.extension.performanceanalysis.uml2lqn.Uml2Lqn;
import kieker.extension.performanceanalysis.uml2plantuml.Uml2PlantUml;
import kieker.extension.performanceanalysis.uml2uml.UmlCopyAndFilter;
import kieker.extension.performanceanalysis.uml2uml.UmlTransformation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class RunAllTransformations implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunAllTransformations.class);

    private final String[] args;
    private final Path modelPath;
    private final Path transformationModelPath;
    private final boolean deleteOutputFolder;
    private final RunAllTransformationsCli cli;

    public RunAllTransformations(final String[] args, RunAllTransformationsCli cli) {
        LOGGER.debug("Creating RunAllTransformations Object");
        this.cli = cli;
        this.args = args;
        this.modelPath = cli.getModelPath();
        this.transformationModelPath = cli.getTransformationPath();
        this.deleteOutputFolder = true;
    }

    @Override
    public void run() {
        LOGGER.debug("Running RunAllTransformations");
        final Instant startTotal = Instant.now();
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
        final Instant kieker2umlStart = Instant.now();
        final Kieker2Uml kieker2Uml = new Kieker2Uml(args, new RunAllTransformationsCli(), false);
        kieker2Uml.run();
        LOGGER.debug("Successfully run Kieker2Uml transformation.");
        final Instant kieker2umlEnd = Instant.now();

        // Uml2PlantUml
        final Instant firstPlantStart = Instant.now();
        if (!cli.isOmitUml2PlantUml()) {
            LOGGER.debug("Running Uml2PlantUml transformation for present UML...");
            final Uml2PlantUml uml2PlantPresent = new Uml2PlantUml(modelPath, uml2PlantUmlCli.getOutputPath().resolve("present"));
            uml2PlantPresent.run();
        }
        final Instant firstPlantEnd = Instant.now();

        // Uml2Uml
        LOGGER.debug("Running Uml2Uml transformation...");
        final Instant uml2umlStart = Instant.now();
        final UmlCopyAndFilter umlCopyAndFilter = new UmlCopyAndFilter(transformationModelPath, modelPath, futureUmlModel);
        umlCopyAndFilter.run();
        final UmlTransformation umlTransformation = new UmlTransformation(futureUmlModel, transformationModelPath);
        umlTransformation.run();
        final Instant uml2umlEnd = Instant.now();

        // Transform the future Uml2PlantUml
        final Instant secondPlantStart = Instant.now();
        if (!cli.isOmitUml2PlantUml()) {
            LOGGER.debug("Running Uml2PlantUml transformation for future UML...");
            final Uml2PlantUml uml2PlantTransformed = new Uml2PlantUml(futureUmlModel, uml2PlantUmlCli.getOutputPath().resolve("transformed"));
            uml2PlantTransformed.run();
        }
        final Instant secondPlantEnd = Instant.now();

        // Uml2Lqn
        final Instant uml2lqnStart = Instant.now();
        LOGGER.debug("Running Uml2Lqn transformation...");
        final Uml2Lqn uml2Lqn = new Uml2Lqn(futureUmlModel, uml2LqnCli.getLqnPath());
        uml2Lqn.run();
        final Instant uml2lqnEnd = Instant.now();
        final Instant endTotal = Instant.now();
        final Duration totalDuration = Duration.between(startTotal, endTotal);
        final Duration kieker2umlDuration = Duration.between(kieker2umlStart, kieker2umlEnd);
        final Duration firstPlantDuration = Duration.between(firstPlantStart, firstPlantEnd);
        final Duration uml2umlDuration = Duration.between(uml2umlStart, uml2umlEnd);
        final Duration secondPlantDuration = Duration.between(secondPlantStart, secondPlantEnd);
        final Duration uml2lqnDuration = Duration.between(uml2lqnStart, uml2lqnEnd);
        LOGGER.info("Total time for all transformations: " + totalDuration);
        LOGGER.info("Time for kieker2uml: " + kieker2umlDuration);
        LOGGER.info("Time for firstPlantTrafoDuration: " + firstPlantDuration);
        LOGGER.info("Time for uml2uml: " + uml2umlDuration);
        LOGGER.info("Time for secondPlantTrafoDuration: " + secondPlantDuration);
        LOGGER.info("Time for uml2lqn: " + uml2lqnDuration);
    }
}
