package kieker.extension.performanceanalysis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Strings;
import kieker.extension.performanceanalysis.cli.Kieker2UmlCli;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {

        final RunAllTransformationsCli runAllTransformationsCli = new RunAllTransformationsCli();
        final Kieker2UmlCli kieker2UmlCli = new Kieker2UmlCli();
        final Uml2PlantUmlCli uml2PlantUmlCli = new Uml2PlantUmlCli();
        final Uml2UmlCli uml2UmlCli = new Uml2UmlCli();
        final Uml2LqnCli uml2LqnCli = new Uml2LqnCli();

        JCommander jc = JCommander.newBuilder()
                .addCommand(runAllTransformationsCli)
                .addCommand(kieker2UmlCli)
                .addCommand(uml2PlantUmlCli)
                .addCommand(uml2UmlCli)
                .addCommand(uml2LqnCli)
                .build();
        try {
            jc.parse(args);
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            jc.usage();
            return;
        }
        String parsedCommand = jc.getParsedCommand();

        if (Strings.isStringEmpty(parsedCommand)) {
            LOGGER.info("No command was given.");
            jc.usage();
            return;
        }

        LOGGER.debug("Command: " + parsedCommand);
        switch (parsedCommand) {
            case "Kieker2Uml":
                new Kieker2Uml(args).run();
                break;
            case "Uml2PlantUml":
                new Uml2PlantUml(uml2PlantUmlCli.getUmlPath(), uml2PlantUmlCli.getOutputPath()).run();
                break;
            case "Uml2Uml":
//                new UmlCopyAndFilter(uml2UmlCli.getTransformationPath(), uml2UmlCli.getUmlPath(), uml2UmlCli.getUmlOutput()).run();
                new UmlCopyAndFilter2(uml2UmlCli.getTransformationPath(), uml2UmlCli.getUmlPath(), uml2UmlCli.getUmlOutput()).run();
                new UmlTransformation(uml2UmlCli.getUmlOutput(), uml2UmlCli.getTransformationPath()).run();
                break;
            case "Uml2Lqn":
                new Uml2Lqn(uml2LqnCli.getUmlPath(), uml2LqnCli.getLqnPath()).run();
                break;
            case "RunAllTransformations":
                new RunAllTransformations(args, runAllTransformationsCli.getModelPath(), runAllTransformationsCli.getTransformationPath()).run();
                break;
            default:
                jc.usage();
                throw new RuntimeException("Unknown command: " + parsedCommand);
        }
    }
}