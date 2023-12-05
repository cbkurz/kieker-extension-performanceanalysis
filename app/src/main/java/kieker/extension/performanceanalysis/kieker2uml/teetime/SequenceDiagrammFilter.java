package kieker.extension.performanceanalysis.kieker2uml.teetime;

import kieker.analysis.plugin.trace.AbstractMessageTraceProcessingFilter;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.MessageTrace;
import org.eclipse.uml2.uml.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static java.lang.String.format;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlModel.addBehaviour;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlModel.addStaticView;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.loadModel;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.saveModel;

public class SequenceDiagrammFilter extends AbstractMessageTraceProcessingFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceDiagrammFilter.class);
    private final Path modelPath;
    private final String useCaseName;

    /**
     * @param repository model repository
     * @param modelPath  the path to the file to which the sequence diagramm is written.
     * @param useCaseName the name of the UML use case to which the interaction shall be added.
     */
    public SequenceDiagrammFilter(final SystemModelRepository repository, final Path modelPath, final String useCaseName) {
        super(repository);
        this.modelPath = modelPath;
        this.useCaseName = useCaseName;
    }

    @Override
    protected void execute(final MessageTrace mt) throws Exception {
        createUmlModel(mt);
        this.reportSuccess(mt.getTraceId());
    }

    private void createUmlModel(final MessageTrace mt) {
        LOGGER.debug("Successfully received MessageTrace: " + mt.getTraceId());

        // UML
        final Model model = loadModel(modelPath);
        addBehaviour(model, mt, useCaseName);
        addStaticView(model, mt);
        saveModel(model, modelPath);


        // logging
        LOGGER.debug("TraceId: " + mt.getTraceId());
        LOGGER.debug(format("Total number of messages: %s", mt.getSequenceAsVector().size()));
        LOGGER.debug(format("Total elapsed time for Trace Id %s: %s ms", mt.getTraceId(), (mt.getEndTimestamp() - mt.getStartTimestamp()) / 1_000_000.0));
    }
}
