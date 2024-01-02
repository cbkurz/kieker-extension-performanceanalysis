package kieker.extension.performanceanalysis.uml2plantuml;

import kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder;
import kieker.extension.performanceanalysis.epsilon.Util;
import org.eclipse.epsilon.egl.launch.EgxRunConfiguration;
import org.eclipse.epsilon.eol.models.Model;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class Uml2PlantUml implements Runnable {

    private final Model umlModel;
    private final Path driver;
    private final Path output;

    public Uml2PlantUml(final Path umlModel, final Path output) {
        requireNonNull(umlModel, "umlModel");
        requireNonNull(output, "output");
        this.umlModel = EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(umlModel)
                .build();
        this.driver = Util.getResource("Uml2PlantUml/Driver.egx");
        this.output = output;
    }

    @Override
    public void run() {
        final EgxRunConfiguration runConfiguration = EgxRunConfiguration.Builder()
                .parallel()
                .withScript(driver)
                .withModel(umlModel)
                .withOutputRoot(output)
                .withProfiling()
                .build();
        runConfiguration.get();
    }
}
