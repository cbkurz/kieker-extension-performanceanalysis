package kieker.extension.performanceanalysis.uml2uml;

import kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder;
import kieker.extension.performanceanalysis.epsilon.Util;
import org.apache.commons.io.FileUtils;
import org.eclipse.epsilon.eol.launch.EolRunConfiguration;
import org.eclipse.epsilon.eol.models.Model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static kieker.extension.performanceanalysis.uml2uml.UmlTransformation.getTransformationModel;

public class UmlCopyAndFilter implements Runnable {

    private final Path script;
    private final Model transformationModel;
    private final Path umlSourceModel;
    private final Path umlTargetPath;

    public UmlCopyAndFilter(final Path transformationModel, final Path umlSourcePath, final Path umlTargetPath) {
        this.script = Util.getResource("Uml2Uml/Filter.eol");
        this.transformationModel = getTransformationModel(transformationModel);
        this.umlSourceModel = umlSourcePath;
        this.umlTargetPath = umlTargetPath;
    }

    @Override
    public void run() {
        Util.validateUmlModel(umlSourceModel);
        try {
            // copy the model to the target location
            FileUtils.copyFile(umlSourceModel.toFile(), umlTargetPath.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // load the model from the target location
        final Model targetModel = EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(umlTargetPath)
                .readOnLoad(true)
                .storeOnDisposal(true)
                .build();

        // load the script Uml2Uml/Filter.eol and execute it on the model in the target location
        final EolRunConfiguration transformation = EolRunConfiguration.Builder()
                .withProfiling()
                .withScript(script)
                .withModel(targetModel)
                .withModel(transformationModel)
                .build();
        transformation.get();
        targetModel.dispose(); // store model
        Util.validateUmlModel(targetModel); // storing and the validating makes it easier to analyse the model in case of validation failure
    }
}
