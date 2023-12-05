package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.AbstractMessage;
import kieker.model.system.model.Execution;
import kieker.model.system.model.MessageTrace;
import kieker.model.system.model.SynchronousCallMessage;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Interaction;
import org.eclipse.uml2.uml.Lifeline;
import org.eclipse.uml2.uml.Message;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Node;
import org.eclipse.uml2.uml.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.addTraceId;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getMessageRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getRepresentationCount;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getTraceRepresentation;

public class MarteSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarteSupport.class);
    public static final String GA_EXEC_HOST = "GaExecHost";
    public static final String GA_SCENARIO = "GaScenario";
    public static final String GA_STEP_ANNOTATION_NAME = "GaStep";
    public static final String EXEC_TIME_ENTRIES_GA_STEP = "execTimeEntries";
    public static final String EXEC_TIME_GA_STEP = "execTime";
    public static final String REP_GA_STEP = "rep";

    /**
     * By Default the rep Attribute (repetition) is set to 1
     *
     * @param umlMessage the UML Element on which the annotations shall be applied.
     * @param execTime   the time of execution
     */
    static void setGaStep(final Element umlMessage, final double execTime) {
        final String execTimeString = Double.toString(execTime);
        Kieker2UmlUtil.setAnnotationDetail(umlMessage, GA_STEP_ANNOTATION_NAME, EXEC_TIME_ENTRIES_GA_STEP, execTimeString);
        Kieker2UmlUtil.setAnnotationDetail(umlMessage, GA_STEP_ANNOTATION_NAME, EXEC_TIME_GA_STEP, execTimeString);
        Kieker2UmlUtil.setAnnotationDetail(umlMessage, GA_STEP_ANNOTATION_NAME, REP_GA_STEP, "1");
    }

    /**
     * The repetition is determined by the amount of exeTimeEntries there are.
     * The execTime is determined by the mean value of the execTimeEntries.
     *
     * @param element The Element to which the GaStep shall be applied.
     */
    static void updateGaStep(final Element element, Double execTime) {
        final Optional<EMap<String, String>> annotationDetailsOptional = Kieker2UmlUtil.getAnnotationDetailsMap(element, GA_STEP_ANNOTATION_NAME);
        if (annotationDetailsOptional.isEmpty()) {
            setGaStep(element, execTime);
            return;
        }

        final EMap<String, String> details = annotationDetailsOptional.get();
        final String currentExecTimeEntries = details.get(EXEC_TIME_ENTRIES_GA_STEP);
        final String execTimeString = Double.toString(execTime);
        final String execTimeCSV = currentExecTimeEntries + "," + execTimeString;

        final String[] execTimeSplit = execTimeCSV.split(","); // the length of this is the amounts of repetitions
        final Double sum = Arrays.stream(execTimeSplit)
                .map(Double::parseDouble)
                .reduce(0D, Double::sum);

        final double execTimeMean = sum / (execTimeSplit.length + 0.0D);

        details.put(EXEC_TIME_ENTRIES_GA_STEP, execTimeCSV);
        details.put(EXEC_TIME_GA_STEP, Double.toString(execTimeMean));
        details.put(REP_GA_STEP, Integer.toString(execTimeSplit.length));
    }

    static void setGaWorkloadEvent(final NamedElement element, final String pattern) {
        Kieker2UmlUtil.setAnnotationDetail(element, "GaWorkloadEvent", "pattern", pattern);
    }

    static void setGaExecHost(final Node node) {
        node.getEAnnotations().stream()
                .filter(a -> GA_EXEC_HOST.equals(a.getSource()))
                .findFirst()
                .orElseGet(() -> node.createEAnnotation(GA_EXEC_HOST));
    }

    static void applyPerformanceStereotypesToInteraction(final Interaction interaction, final MessageTrace messageTrace) {

        LOGGER.trace("Starting to apply performance stereotypes to interaction");

        // fail fast
        requireNonNull(interaction, "interaction");
        requireNonNull(messageTrace, "messageTrace");
        final String traceRepresentation = getTraceRepresentation(messageTrace);
        final Optional<String> id = getRepresentation(interaction);
        if (id.isEmpty()) {
            throw new ModelNotComformantException("Cannot apply performance information to Interaction that does not have an id. Interaction: " + interaction.getName());
        }
        if (!traceRepresentation.equals(id.get())) {
            throw new IllegalArgumentException("Interaction does not represent MessageTrace. It is not possible to apply performance information.");
        }

        // start working
        // GaStep
        List<AbstractMessage> sequenceAsVector = messageTrace.getSequenceAsVector();
        for (int count = 0; count < sequenceAsVector.size(); count++) { // The count was introduced to have an additional separation option for Messages that have the same representation
            final AbstractMessage message = sequenceAsVector.get(count);
            if (message instanceof SynchronousCallMessage) {
                final String messageRepresentation = getMessageRepresentation(message);
                final Message umlMessage = getUmlMessage(interaction, messageRepresentation, count);
                // Ess - execution stack size
                final Long execTimeOtherExecutions = messageTrace.getSequenceAsVector().stream()
                        .filter(m -> m instanceof SynchronousCallMessage)
                        .filter(m -> message.getReceivingExecution().equals(m.getSendingExecution()))
                        .map(m -> getExecTime(m.getReceivingExecution()))
                        .reduce(Long::sum)
                        .orElse(0L); // if no other executions are found this execution does not call others and the total time should be applied
                final long totalExecTime = getExecTime(message.getReceivingExecution());
                final long execTime = totalExecTime - execTimeOtherExecutions;
                if (execTime < 0) {
                    throw new IllegalArgumentException("ExecTime cannot be less than zero. ExecTime value: " + execTime);
                }
                applyGaStep(umlMessage, execTime); // --> in the LQNS solver the default time is 10.000 time units, therefore one second should at least be possible
            }
        }

        // finnish
        addTraceId(interaction, messageTrace);
    }

    private static long getExecTime(final Execution execution) {
        return execution.getTout() - execution.getTin();
    }

    private static void applyGaStep(final Message umlMessage, final double execTime) {
        updateGaStep(umlMessage, execTime);
    }

    private static Lifeline getSenderLifeline(final Interaction interaction, final String identifier) {
        requireNonNull(interaction, "interaction");
        requireNonNull(identifier, "identifier");
        final List<Lifeline> list = interaction.getLifelines().stream()
                .filter(l -> identifier.equals(l.getName()))
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            throw new ModelNotComformantException("Lifeline not found with identifier: " + identifier);
        }
        if (list.size() > 1) {
            throw new ModelNotComformantException(String.format("To many Lifelines found for identifier: %s\nList size: %s\nList: %s", identifier, list.size(), list));
        }

        return list.get(0);
    }

    private static Message getUmlMessage(final Interaction interaction, final String messageRepresentation, final int count) {
        final List<Message> list = interaction.getMessages().stream()
                .filter(m -> getRepresentation(m).map(rep -> rep.equals(messageRepresentation)).orElse(false))
                .filter(m -> getRepresentationCount(m).map(messageCount -> messageCount == count).orElse(false))
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            throw new ModelNotComformantException("Message not found with representation: " + messageRepresentation);
        }
        if (list.size() > 1) {
            throw new ModelNotComformantException(String.format("To many Messages found for representation: %s\nList size: %s\nList: %s", messageRepresentation, list.size(), list));
        }
        return list.get(0);
    }

    public static void applyPerformanceStereotypesToNodes(final List<Node> nodeList) {
        requireNonNull(nodeList, "nodeList");
        nodeList.forEach(MarteSupport::setGaExecHost);
    }

    public static void applyGaScenario(final UseCase useCase) {
        useCase.getEAnnotations().stream()
                .filter(a -> GA_SCENARIO.equals(a.getSource()))
                .findFirst()
                .orElseGet(() -> useCase.createEAnnotation(GA_SCENARIO));
    }
}
