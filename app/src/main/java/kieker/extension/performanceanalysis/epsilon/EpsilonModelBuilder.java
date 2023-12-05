package kieker.extension.performanceanalysis.epsilon;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.emf.xml.XmlModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.models.Model;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class EpsilonModelBuilder {
    private static final Path MODELS = Util.getResource("models");
    private final static Path META_MODELS = Util.getResource("metamodels");

    private ModelType modelType;

    private final StringProperties properties;

    private EpsilonModelBuilder() {
        this.properties = new StringProperties();
        setDefaults();
    }

    private void setDefaults() {
        this.properties.setProperty(EmfModel.PROPERTY_READONLOAD, "true");
        this.properties.setProperty(EmfModel.PROPERTY_STOREONDISPOSAL, "true");
    }


    public static ModelTypeBuilder getInstance() {
        final EpsilonModelBuilder epsilonModelBuilder = new EpsilonModelBuilder();
        return new ModelTypeBuilder(epsilonModelBuilder);
    }

    public EpsilonModelBuilder readOnLoad(final boolean readOnLoad) {
        this.properties.setProperty(EmfModel.PROPERTY_READONLOAD, Boolean.toString(readOnLoad));
        return this;
    }
    public EpsilonModelBuilder storeOnDisposal(final boolean storeOnDisposal) {
        this.properties.setProperty(EmfModel.PROPERTY_STOREONDISPOSAL, Boolean.toString(storeOnDisposal));
        return this;
    }
    public EpsilonModelBuilder modelName(final String name) {
        this.properties.setProperty(EmfModel.PROPERTY_NAME, name);
        return this;
    }
    public EpsilonModelBuilder modelAlias(final String alias) {
        this.properties.setProperty(EmfModel.PROPERTY_ALIASES, alias);
        return this;
    }
    public EpsilonModelBuilder readOnly(final boolean readOnly) {
        this.properties.setProperty(EmfModel.PROPERTY_READONLY, Boolean.toString(readOnly));
        return this;
    }
    public EpsilonModelBuilder validate(final boolean validate) {
        this.properties.setProperty(EmfModel.PROPERTY_VALIDATE, Boolean.toString(validate));
        return this;
    }

    public EpsilonModelBuilder metaModel(final URI metaModel) {
        this.properties.setProperty(EmfModel.PROPERTY_METAMODEL_URI, metaModel.toString());
        return this;
    }
    public EpsilonModelBuilder metaModel(final Path metaModel) {
        this.properties.setProperty(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, metaModel.toAbsolutePath().toUri().toString());
        return this;
    }
    public EpsilonModelBuilder metaModel(final String metaModel) {
        this.properties.setProperty(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI, META_MODELS.resolve(metaModel).toAbsolutePath().toUri().toString());
        return this;
    }
    public EpsilonModelBuilder reuseMetaModel(final boolean reuseMetaModel) {
        this.properties.setProperty(EmfModel.PROPERTY_REUSE_UNMODIFIED_FILE_BASED_METAMODELS, Boolean.toString(reuseMetaModel));
        return this;
    }

    public EpsilonModelBuilder modelPath(final Path model) {
        this.properties.setProperty(EmfModel.PROPERTY_MODEL_URI, model.toAbsolutePath().toUri().toString());
        return this;
    }
    public EpsilonModelBuilder modelPath(final String model) {
        this.properties.setProperty(EmfModel.PROPERTY_MODEL_URI, MODELS.resolve(model).toAbsolutePath().toUri().toString());
        return this;
    }


    public Model build() {
        checkRequired();
        final Model model;
        switch (this.modelType) {
            case XML:
                model = new XmlModel();
                break;
            case UML:
                model = new UmlModel();
                break;
            case EMF:
                model = new EmfModel();
                break;
            default:
                throw new RuntimeException("No appropriate ModelType was selected.");
        }
        try {
            model.load(this.properties);
        } catch (EolModelLoadingException e) {
            throw new RuntimeException(e);
        }
        return model;
    }

    private void checkRequired() {
        Objects.requireNonNull(this.properties.get(EmfModel.PROPERTY_NAME), "A name for the model is required.");
        if ((this.modelType.equals(ModelType.EMF)
                || this.modelType.equals(ModelType.UML))
                && isNull(this.properties.get(EmfModel.PROPERTY_FILE_BASED_METAMODEL_URI))
                && isNull(this.properties.get(EmfModel.PROPERTY_METAMODEL_URI))) {
            throw new NullPointerException("A meta-model is required.");
        }
        if (this.modelType.equals(ModelType.XML)) {
            requireNonNull(this.properties.get(XmlModel.PROPERTY_XSD_URI));
        }
        Objects.requireNonNull(this.properties.get(EmfModel.PROPERTY_MODEL_URI));
    }

    private static URI getUmlUri() {
        try {
            return new URI("http://www.eclipse.org/uml2/5.0.0/UML");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    public static class ModelTypeBuilder {
        private final EpsilonModelBuilder builder;

        private ModelTypeBuilder(EpsilonModelBuilder epsilonModelBuilder) {
            this.builder = epsilonModelBuilder;
        }

        public EpsilonModelBuilder emfModel() {
            this.builder.modelType = ModelType.EMF;
            return this.builder;
        }

        public EpsilonModelBuilder xmlModel(final String xsd) {
            this.builder.modelType = ModelType.XML;
            this.builder.properties.setProperty(XmlModel.PROPERTY_XSD_URI, META_MODELS.resolve(xsd).toAbsolutePath().toUri().toString());
            return this.builder;
        }
        public EpsilonModelBuilder umlModel() {
            this.builder.modelType = ModelType.UML;
            this.builder.properties.setProperty(XmlModel.PROPERTY_METAMODEL_URI, getUmlUri().toString());
            return this.builder;
        }

    }

    enum ModelType {
        EMF,UML, XML
    }
}
