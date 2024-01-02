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

/**
 * <p>
 * This class manages the creation of the UML Model.
 * It provides two major methods:
 * </p>
 * <ul>
 *     <li>
 *         {@link Kieker2UmlModel#addStaticAndDeploymentPackage(Model, MessageTrace)} - This method creates all elements for the
 *         deployment and the component (or static) packages
 *         </li>
 *     <li>
 *         {@link Kieker2UmlModel#addBehaviourToDynamicPackage(Model, MessageTrace, String)} - his method creates the behavioural
 *         package elements i.e. {@link UseCase} and {@link Interaction}s
 *     </li>
 * </ul>
 * <p>The MARTE parts are automatically at the end of the execution.</p>
 * <p>
 * Duplications can occur in two ways, a {@link MessageTrace} is the same as another but recorded at a different time,
 * and the exact same {@link MessageTrace} has been added before.
 *</p>
 * <br/>
 * <p>
 * <b>Recognition of representation:</b> <br />
 * In order to find out if a MessageTrace is already represented as an Interaction the MessageTrace is represented
 * as a String. The sequence of {@link kieker.model.system.model.AbstractMessage} is made to strings and added
 * together in sequence. The sequence is added as a {@link org.eclipse.emf.ecore.EAnnotation} (source=
 * {@link Kieker2UmlUtil#REPRESENTATION_ANNOTATION_NAME}) for the Interaction. When the incoming representation of
 * the {@link MessageTrace} matches the on of the {@link Interaction}, no new interaction is added.
 * </p>
 * <br/>
 * <p>
 * <b>Recognition of same {@link MessageTrace}:</b><br/>
 * After the adding of each {@link MessageTrace} the traceId is added in the {@link org.eclipse.emf.ecore.EAnnotation}
 * (source={@link Kieker2UmlUtil#TRACE_IDS_SET_NAME}). If the traceId is applied twice the {@link MessageTrace} is ignored.
 * </p>
 *
 */
public class Kieker2UmlModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kieker2UmlModel.class);

    public static void main(String[] args) {
        Model m = Kieker2UmlUtil.loadModel(Paths.get("Kieker2Uml/input-data/uml/SequenceDiagrams.uml"));
        System.out.println(m.getName());
        Kieker2UmlUtil.saveModel(m, Paths.get("Kieker2Uml/input-data/uml/SequenceDiagrams2.uml"));
    }

    /**
     * <p>This method adds the behavioural Package:</p>
     * <ul>
     *     <li>{@link UseCase}</li>
     *     <li>{@link org.eclipse.uml2.uml.Actor}</li>
     *     <li>{@link Interaction}s and their internals</li>
     *     <li>MARTE stereotypes: GaStep, GaScenario, see class {@link MarteSupport} for details.</li>
     * </ul>
     *
     * <p>MessageTraces are only create new Interactions if the interaction is not already represented within a use case.
     * If a MessageTrace is already represented only the performance information of the MARTE stereotypes is updated:</p>
     * <ul>
     *     <li>GaStep execTimeEntries - a comma separated list of the net execution times</li>
     *     <li>GaStep execTime - the mean net execution time for this method</li>
     *     <li>open arrival rate - added for the first {@link org.eclipse.uml2.uml.Lifeline}</li>
     * </ul>
     * <p>If a MessageTrace is already added to an interaction it is ignored.</p>
     * <p>The first Lifeline of each Interaction is connected to the Actor of the {@link UseCase}.</p>
     * @param model - The UML {@link Model} to which the behaviour is added.
     * @param messageTrace - The Kieker {@link MessageTrace} that is processed to become an {@link Interaction}.
     * @param useCaseName - The name of the {@link UseCase} to which the {@link Interaction} shall be added.
     */
    public static void addBehaviourToDynamicPackage(final Model model, final MessageTrace messageTrace, final String useCaseName) {
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

    /**
     * <p>
     *     This method adds static and dynamic view to the model this includes the following elements:
     * </p>
     * <ul>
     *     <li>{@link org.eclipse.uml2.uml.Component}</li>
     *     <li>{@link org.eclipse.uml2.uml.Operation}</li>
     *     <li>{@link org.eclipse.uml2.uml.Interface}</li>
     *     <li>{@link org.eclipse.uml2.uml.Usage}</li>
     *     <li>{@link Node}</li>
     *     <li>{@link org.eclipse.uml2.uml.Artifact}</li>
     *     <li>MARTE stereotypes: GaExecHost (see {@link MarteSupport#applyPerformanceStereotypesToNodes(List)})</li>
     * </ul>
     * <p>If the id of a MessageTrace is already added it is ignored.</p>
     * @param model - The UML {@link Model} to which the static and deployment is added.
     * @param trace - The Kieker {@link MessageTrace} that is processed to become the UML elements.
     */
    public static void addStaticAndDeploymentPackage(final Model model, final MessageTrace trace) {
        try {
            UmlClasses.addClasses(model, trace);
        } catch (Exception e) {
            // This addition of classes is not relevant to other transformations and can be ignored if it fails.
        }
        UmlStaticViews.addComponentsAndDeployment(model, trace);
        final List<Node> nodeList = model.allOwnedElements().stream()
                .filter(pe -> pe instanceof Node).map(pe -> (Node) pe)
                .collect(Collectors.toList());
        MarteSupport.applyPerformanceStereotypesToNodes(nodeList);
    }

}
