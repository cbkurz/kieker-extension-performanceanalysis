package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.MessageTrace;
import org.eclipse.uml2.uml.Interaction;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Node;
import org.eclipse.uml2.uml.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlInteractions.addLifelines;

public class Kieker2UmlModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kieker2UmlModel.class);

    public static void main(String[] args) {
        Model m = Kieker2UmlUtil.loadModel(Paths.get("Kieker2Uml/input-data/uml/SequenceDiagrams.uml"));
        System.out.println(m.getName());
        Kieker2UmlUtil.saveModel(m, Paths.get("Kieker2Uml/input-data/uml/SequenceDiagrams2.uml"));
    }

    public static void addBehaviour(final Model model, final MessageTrace messageTrace, final String useCaseName) {
        final String traceRepresentation = Kieker2UmlUtil.getTraceRepresentation(messageTrace);
        final UseCase useCase = UmlUseCases.getUseCase(model, useCaseName, traceRepresentation);
        MarteSupport.applyGaScenario(useCase);

        final Optional<Interaction> interaction = UmlInteractions.getInteraction(useCase, traceRepresentation);
        if (interaction.isEmpty()) { // create Interaction
            LOGGER.info("Creating interaction for Trace: " + messageTrace.getTraceId());

            final Interaction newInteraction = UmlInteractions.createInteraction(UmlInteractions.getInteractionName(useCase), messageTrace);
            useCase.getOwnedBehaviors().add(newInteraction);

            addLifelines(newInteraction, messageTrace.getSequenceAsVector());
            MarteSupport.applyPerformanceStereotypesToInteraction(newInteraction, messageTrace);
            UmlInteractions.connectEntryLifelineToActor(useCase);
        } else if (!Kieker2UmlUtil.isIdApplied(interaction.get(), Long.toString(messageTrace.getTraceId()))) { // update Interaction
            LOGGER.info("Interaction was created before, performance information will now be added to Trace with id: " + messageTrace.getTraceId());
            MarteSupport.applyPerformanceStereotypesToInteraction(interaction.get(), messageTrace);
        } else {
            LOGGER.info(String.format("Trace with id '%s' was applied before and is therefore skipped.", messageTrace.getTraceId()));
        }
    }

    public static void addStaticView(final Model model, final MessageTrace trace) {
        UmlClasses.addClasses(model, trace);
        UmlComponents.addComponentsAndDeployment(model, trace);
        final List<Node> nodeList = model.allOwnedElements().stream()
                .filter(pe -> pe instanceof Node).map(pe -> (Node) pe)
                .collect(Collectors.toList());
        MarteSupport.applyPerformanceStereotypesToNodes(nodeList);
    }

}
