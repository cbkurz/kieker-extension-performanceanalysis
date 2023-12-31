package kieker.extension.performanceanalysis.uml2uml;

import kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder;
import kieker.extension.performanceanalysis.epsilon.Util;
import org.eclipse.epsilon.eol.launch.EolRunConfiguration;
import org.eclipse.epsilon.eol.models.Model;

import java.nio.file.Path;

public class UmlTransformation implements Runnable {

    private final Path script;
    private final Model transformationModel;
    private final Model umlModel;

    public UmlTransformation(final Path umlPath, final Path transformationModelPath) {
        this.script = Util.getResource("Uml2Uml/Transformations.eol");
        this.transformationModel = getTransformationModel(transformationModelPath);
        this.umlModel = getUml(umlPath);

    }

    private static Model getUml(final Path path) {
        return EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(path)
                .readOnLoad(true)
                .storeOnDisposal(true)
                .build();
    }

    static Model getTransformationModel(final Path transformationModel) {
        return EpsilonModelBuilder.getInstance()
                .emfModel()
                .modelName("UmlTransformation")
                .modelAlias("UT")
                .metaModel("UmlTransformation.ecore")
                .modelPath(transformationModel)
                .readOnly(true)
                .build();
    }


    @Override
    public void run() {;
        final EolRunConfiguration transformUml = EolRunConfiguration.Builder()
                .withScript(script)
                .withModel(transformationModel)
                .withModel(umlModel)
                .withProfiling()
                .build();
        Util.validateUmlModel(umlModel);
        transformUml.get();
        umlModel.dispose();
        Util.validateUmlModel(umlModel); // storing and the validating makes it easier to analyse the model in case of validation failure
    }
}
