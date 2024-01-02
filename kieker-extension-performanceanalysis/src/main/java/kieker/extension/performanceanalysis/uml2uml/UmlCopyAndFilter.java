package kieker.extension.performanceanalysis.uml2uml;

import kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder;
import kieker.extension.performanceanalysis.epsilon.Util;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.etl.launch.EtlRunConfiguration;

import java.nio.file.Path;

public class UmlCopyAndFilter implements Runnable {

    private final Path script;
    private final Model transformationModel;
    private final Model umlSourceModel;
    private final Model umlTargetModel;

    public UmlCopyAndFilter(final Path transformationModel, final Path umlSourceModel, final Path umlTargetPath) {
        this.script = Util.getResource("Uml2Uml/Uml2Uml.etl");
        this.umlSourceModel = getSourceUml(umlSourceModel);
        this.transformationModel = getTransformationModel(transformationModel);
        this.umlTargetModel = getTargetModel(umlTargetPath);
    }

    private static Model getSourceUml(final Path umlSourceModel) {
        return EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(umlSourceModel)
                .readOnly(true)
                .build();
    }

    private static Model getTargetModel(final Path targetPath) {
        return EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("FUML")
                .modelPath(targetPath)
                .readOnLoad(false)
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
    public void run() {
        final EtlRunConfiguration runConfiguration = EtlRunConfiguration.Builder()
                .withScript(script)
                .withModel(umlSourceModel)
                .withModel(transformationModel)
                .withModel(umlTargetModel)
                .withProfiling()
                .build();
        Util.validateUmlModel(umlSourceModel);
        runConfiguration.get();
        Util.validateUmlModel(umlTargetModel);
        umlTargetModel.dispose();
    }
}
