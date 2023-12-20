package kieker.extension.performanceanalysis.uml2lqn;

import kieker.extension.performanceanalysis.epsilon.EpsilonModelBuilder;
import kieker.extension.performanceanalysis.epsilon.Util;
import org.eclipse.epsilon.emc.plainxml.PlainXmlModel;
import org.eclipse.epsilon.eol.launch.EolRunConfiguration;
import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.etl.launch.EtlRunConfiguration;
import org.eclipse.epsilon.evl.launch.EvlRunConfiguration;

import java.nio.file.Path;

public class Uml2Lqn implements Runnable {

    private final Path uml2LqnScript;
    private final Path changeRootScript;

    private final Model umlModel;
    private final Model lqnModel;
    private final Path lqnModelPath;

    public Uml2Lqn(final Path umlModel, final Path lqnModel) {
        this.uml2LqnScript = Util.getResource("Uml2Lqn/Uml2Lqn.etl");
        this.changeRootScript = Util.getResource("Uml2Lqn/ChangeRoot.eol");
        this.umlModel = getUmlModel(umlModel);
        this.lqnModel = getLqnModel(lqnModel);
        this.lqnModelPath = lqnModel;
    }

    private Model getLqnModel(final Path lqnModel) {
        return EpsilonModelBuilder.getInstance()
                .xmlModel("lqn.xsd")
                .modelName("LQN")
                .modelPath(lqnModel)
                .readOnLoad(false)
                .storeOnDisposal(true)
                .build();
    }

    private Model getUmlModel(final Path umlModel) {
        return EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(umlModel)
                .readOnly(true)
                .storeOnDisposal(false)
                .build();
    }

    @Override
    public void run() {

        final EtlRunConfiguration uml2qn = EtlRunConfiguration.Builder()
                .withScript(uml2LqnScript)
                .withModel(umlModel)
                .withModel(lqnModel)
                .withProfiling()
                .build();
        Util.validateUmlModel(umlModel);
        uml2qn.run();
        uml2qn.get();
        lqnModel.dispose();

        final PlainXmlModel plainLqnModel = getPlainLqnModel();

        final EolRunConfiguration changeRoot = EolRunConfiguration.Builder()
                .withScript(changeRootScript)
                .withModel(plainLqnModel)
                .withProfiling()
                .build();
        changeRoot.run();
        plainLqnModel.dispose();
        final LqnSchemaValidator lqnSchemaValidator = new LqnSchemaValidator();
        lqnSchemaValidator.validate(plainLqnModel.getFile());
    }

    private PlainXmlModel getPlainLqnModel() {
        final PlainXmlModel plainLqnModel = new PlainXmlModel();
        plainLqnModel.setName("PlainLQN");
        plainLqnModel.setFile(lqnModelPath.toFile());
        plainLqnModel.setStoredOnDisposal(true);
        plainLqnModel.setReadOnLoad(true);
        return plainLqnModel;
    }
}
