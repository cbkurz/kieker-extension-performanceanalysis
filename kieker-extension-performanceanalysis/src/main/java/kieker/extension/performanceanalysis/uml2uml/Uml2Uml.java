package kieker.extension.performanceanalysis.uml2uml;

import kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder;
import kieker.extension.performanceanalysis.epsilon.Util;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.etl.launch.EtlRunConfiguration;

import java.nio.file.Path;

public class Uml2Uml implements Runnable {

    private final Model umlSourceModel;
    private final Model transformationModel;
    private final Model umlFutureModel;
    private final Path script;

    public Uml2Uml(final Path umlSourceModel, final Path transformationModel, final Path umlFutureModel) {
        this.script = Util.getResource("Uml2Uml/Uml2Uml.etl");
        this.umlSourceModel = getSourceUml(umlSourceModel);
        this.transformationModel = getTransformationModel(transformationModel);
        this.umlFutureModel = getFuml(umlFutureModel);
    }

    private static Model getSourceUml(final Path umlSourceModel) {
        return EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(umlSourceModel)
                .readOnly(true)
                .build();
    }

    private static Model getFuml(final Path umlFutureModel) {
        return EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("FUML")
                .modelPath(umlFutureModel)
                .readOnLoad(false)
                .storeOnDisposal(true)
                .build();
    }

    private static Model getTransformationModel(final Path transformationModel) {
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
    public void run() {
        final EtlRunConfiguration runConfiguration = EtlRunConfiguration.Builder()
                .withScript(script)
                .withModel(umlSourceModel)
                .withModel(transformationModel)
                .withModel(umlFutureModel)
                .withProfiling()
                .build();
        Util.validateUmlModel(umlSourceModel);
        runConfiguration.run();
        umlFutureModel.dispose();
        Util.validateUmlModel(umlFutureModel);
    }
}
