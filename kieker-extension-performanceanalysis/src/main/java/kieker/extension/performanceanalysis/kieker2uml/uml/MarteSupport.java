package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.AbstractMessage;
import kieker.model.system.model.Execution;
import kieker.model.system.model.MessageTrace;
import kieker.model.system.model.SynchronousCallMessage;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.uml2.uml.BehaviorExecutionSpecification;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Interaction;
import org.eclipse.uml2.uml.Lifeline;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Node;
import org.eclipse.uml2.uml.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.addId;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getAnnotationDetail;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getIds;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getMessageRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getRepresentationCount;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getTraceRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setAnnotationDetail;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlInteractions.getBESRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlUseCases.KIEKER_ENTRY_NAME;

/**
 * This class applies the MARTE Stereotypes to the uml elements.
 * To do this a workaround is used by applying stereotypes with EAnnotations.
 * The Name of the Annotation is the name of the Stereotype.
 * The details of the Annotation are the fields of the Stereotype.
 * With the method {@link MarteSupport#applyPerformanceStereotypesToInteraction} performance information are provided in bulk.
 */
public class MarteSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarteSupport.class);
    public static final String GA_EXEC_HOST = "GaExecHost";
    public static final String GA_SCENARIO = "GaScenario";
    public static final String GA_STEP_ANNOTATION_NAME = "GaStep";
    public static final String EXEC_TIME_ENTRIES_GA_STEP = "execTimeEntries";
    public static final String EXEC_TIME_GA_STEP = "execTime";
    public static final String REP_GA_STEP = "rep";
    public static final String PERFORMANCE_INFORMATION = "PerformanceInformation";

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
     * The repetition remains "1" independent of the entries.
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
        final String execTimesCSV = currentExecTimeEntries + "," + execTimeString;

        final String[] execTimeSplit = execTimesCSV.split(","); // the length of this is the amounts of repetitions
        final Double sum = Arrays.stream(execTimeSplit)
                .map(Double::parseDouble)
                .reduce(0D, Double::sum);

        final double execTimeMean = sum / (double) execTimeSplit.length;

        details.put(EXEC_TIME_ENTRIES_GA_STEP, execTimesCSV);
        details.put(EXEC_TIME_GA_STEP, Double.toString(execTimeMean));
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

        LOGGER.debug("Starting to apply performance stereotypes to interaction");

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

        // calculate the open workload
        setOpenWorkloadInformation(interaction, messageTrace.getStartTimestamp(), messageTrace.getEndTimestamp());
        // GaStep
        setGaStep(interaction, messageTrace);

        // finnish
        addId(interaction, Long.toString(messageTrace.getTraceId()));
    }

    /**
     * <p>
     *     This method calculates all GaStep Stereotypes for the {@link Interaction}.
     *     This is done by iterating over all {@link AbstractMessage} in the {@link MessageTrace}.
     *     Only {@link SynchronousCallMessage} are processed.
     *     For the GaStep the net time is set as the execution time.
     *     This method works together with the method {@link UmlInteractions#addLifelines(Interaction, List)}.
     * </p>
     * @param interaction - The {@link Interaction} to which the performance information shall be applied.
     * @param messageTrace - The {@link MessageTrace} that holds the performance information.
     */
    private static void setGaStep(final Interaction interaction, final MessageTrace messageTrace) {
        List<AbstractMessage> sequenceAsVector = messageTrace.getSequenceAsVector();
        // the count of 0 is the first lifeline after "'Entry'"
        // The count was introduced to have an additional separation option for Messages that have the same representation
        for (int count = 0; count < sequenceAsVector.size(); count++) {
            final AbstractMessage message = sequenceAsVector.get(count);
            if (!(message instanceof SynchronousCallMessage)) {
                continue;
            }

            final String besRepresentation = getBESRepresentation(getMessageRepresentation(message));
            final BehaviorExecutionSpecification bes = getBES(interaction, besRepresentation, count);
            // We start at the first lifeline after "'Entry'" the net time must be calculated for the receivingExecution
            // This happens since the first message in the vector is from "'Entry'" to the next Lifeline
            final long execTime = getNetExecTime(messageTrace, message.getReceivingExecution());
            updateGaStep(bes, (double) execTime);
        }
        // Entry Lifeline BES
        final BehaviorExecutionSpecification entryBes = getBES(interaction, "'Entry'", -1); // this is set
        final long otherTime = getTotalExecTime(messageTrace.getSequenceAsVector().get(0).getReceivingExecution()); // Entry-Lifeline only has one
        final long execTime = (messageTrace.getEndTimestamp() - messageTrace.getStartTimestamp()) - otherTime;
        updateGaStep(entryBes, (double) (execTime == 0 ? 1 : execTime));
    }

    /**
     * This method calculated the net execution time.
     *
     * The net execution time is calculated by finding all messages that were sent from the execution,
     * adding their total execution time and subcontracting it from the total execution time of the current execution
     * @param messageTrace all messages
     * @param receivingExecution the execution
     * @return the net execution time
     */
    private static long getNetExecTime(final MessageTrace messageTrace, final Execution receivingExecution) {
        final Long execTimeOtherExecutions = messageTrace.getSequenceAsVector().stream()
                .filter(m -> m instanceof SynchronousCallMessage)
                .filter(m -> receivingExecution.equals(m.getSendingExecution())) // this collects all messages that are send by the execution
                .map(m -> getTotalExecTime(m.getReceivingExecution()))
                .reduce(Long::sum)
                .orElse(0L); // if no other executions are found this execution does not call others and the total time should be applied
        final long totalExecTime = getTotalExecTime(receivingExecution);
        // the execTime should be set to the time the message is actually working and not waiting for the execution of other messages
        final long execTime = totalExecTime - execTimeOtherExecutions;
        if (execTime < 0) {
            throw new IllegalArgumentException("ExecTime cannot be less than zero. ExecTime value: " + execTime);
        }
        return execTime;
    }

    /**
     * Calculates the total execution time with Tout - Tin
     * @param execution the execution
     * @return the time in nanos it took for the execution.
     */
    private static long getTotalExecTime(final Execution execution) {
        // Ess - execution stack size
        // Tin - time in nanos when the execution was entered
        // Tout - time in nanos when the execution was left
        return execution.getTout() - execution.getTin();
    }


    private static BehaviorExecutionSpecification getBES(final Interaction interaction, final String messageRepresentation, final int count) {
        final List<BehaviorExecutionSpecification> list = interaction.getFragments().stream()
                .filter(f -> f instanceof BehaviorExecutionSpecification)
                .filter(f -> Kieker2UmlUtil.getRepresentation(f).map(r -> r.equals(messageRepresentation)).orElse(false))
                .filter(f -> getRepresentationCount(f).map(c -> c == count).orElse(false))
                .map(f -> (BehaviorExecutionSpecification) f)
                .collect(Collectors.toList());;

        if (list.isEmpty()) {
            throw new ModelNotComformantException("BES not found with representation: " + messageRepresentation);
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

    /**
     * <p>
     *     This method calculated the open arrival rate and sets it on the lifeline as the stereotype GaWorkloadEvent.
     *     For the arrival rate to be calculated three values are required:
     * </p>
     * <ul>
     *     <li>the earliest start time</li>
     *     <li>the latest end time</li>
     *     <li>the amounts of recorded MessageTraces for the {@link Interaction}</li>
     * </ul>
     *
     * <p>
     *     First the number of traces is received (N), the the execution time (T) is calculated by subtracting the start
     *     time from the end time. Thereafter the open arrival rate is calculated by: N / T
     * </p>
     *
     * @param interaction                   the interaction in question
     * @param startTimestamp                the start timestamp for the <b>current</b> {@link MessageTrace}
     * @param endTimestamp                  the end timestamp for the <b>current</b> {@link MessageTrace}
     * @throws ModelNotComformantException  if the {@link Interaction} is not owned by a {@link UseCase}
     */
    @SuppressWarnings("SimplifyOptionalCallChains")
    private static void setOpenWorkloadInformation(final Interaction interaction, final long startTimestamp, final long endTimestamp) {
        final Element useCase = interaction.getOwner();
        // fail fast
        if (isNull(useCase) || !(useCase instanceof UseCase)) {
            throw new ModelNotComformantException("An Interaction must be owned by a UseCase, this requirement was not met. Interaction: " + interaction);
        }
        // work
        // get start and end times
        final Long startTime = getAnnotationDetail(useCase, PERFORMANCE_INFORMATION, "startTime")
                .map(Long::parseLong)
                .map(v -> startTimestamp < v ? startTimestamp : v)
                .orElse(startTimestamp);
        final Long endTime = getAnnotationDetail(useCase, PERFORMANCE_INFORMATION, "endTime")
                .map(Long::parseLong)
                .map(v -> endTimestamp > v ? endTimestamp : v)
                .orElse(endTimestamp);

        LOGGER.debug("start timestamp received: " + startTimestamp);
        LOGGER.debug("end timestamp received: " + endTimestamp);
        LOGGER.debug("start time: " + startTime);
        LOGGER.debug("end time: " + endTime);

        // write current values back
        setAnnotationDetail(useCase, PERFORMANCE_INFORMATION, "startTime", startTime.toString());
        setAnnotationDetail(useCase, PERFORMANCE_INFORMATION, "endTime", endTime.toString());

        // calculate open arrival rate
        final BigDecimal numberOfTraces = BigDecimal.valueOf(getIds(interaction).map(s -> s.size() + 1).orElse(1));
        final BigDecimal executionTime = BigDecimal.valueOf(endTime - startTime);
        final String openWorkload = numberOfTraces.divide(executionTime, 20, RoundingMode.HALF_UP).toPlainString();

        // set GaWorkloadEvent with open arrival rate
        final Lifeline lifeline = interaction.getLifeline(KIEKER_ENTRY_NAME);
        MarteSupport.setGaWorkloadEvent(lifeline, "open:" + openWorkload); // this follows the pattern required by the Uml2Lqn transformation
    }
}
