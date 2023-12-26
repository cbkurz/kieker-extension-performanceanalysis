package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.AbstractMessage;
import kieker.model.system.model.AssemblyComponent;
import kieker.model.system.model.MessageTrace;
import kieker.model.system.model.Operation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.Actor;
import org.eclipse.uml2.uml.BehaviorExecutionSpecification;
import org.eclipse.uml2.uml.Component;
import org.eclipse.uml2.uml.Interaction;
import org.eclipse.uml2.uml.Lifeline;
import org.eclipse.uml2.uml.MessageOccurrenceSpecification;
import org.eclipse.uml2.uml.MessageSort;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.createAssociation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getModel;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setReferenceAnnotation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setReferenceAnnotations;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setRepresentationCount;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlStaticViews.STATIC_VIEW_COMPONENTS;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlStaticViews.getInterfaceName;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlUseCases.KIEKER_ENTRY_NAME;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlUseCases.getActor;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlUseCases.getDynamicView;

/**
 * This class adds interactions to the model.
 * Interactions are also known as sequence diagrams.
 * The "'Entry'" Execution from Kieker is set as the first lifeline and needs special treatment.
 * "'Entry'" is also set to the Actor.
 */
class UmlInteractions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmlInteractions.class);
    public static final EClass MESSAGE_OCCURRENCE_E_CLASS = UMLPackage.Literals.MESSAGE_OCCURRENCE_SPECIFICATION;
    public static final EClass BEHAVIOUR_EXECUTION_E_CLASS = UMLPackage.Literals.BEHAVIOR_EXECUTION_SPECIFICATION;
    public static final String BEHAVIOUR_EXECUTION_SPECIFICATION_PREFIX = "BES-";
    public static final String RECEIVE_MESSAGE_OCCURRENCE_SPECIFICATION = "ReceiveMOS-";
    public static final String SEND_MESSAGE_OCCURRENCE_SPECIFICATION = "SendMOS-";

    static Interaction createInteraction(final String interactionName, final MessageTrace messageTrace) {
        final Interaction interaction = UMLFactory.eINSTANCE.createInteraction();

        interaction.setName(interactionName);
        Kieker2UmlUtil.addId(interaction, Long.toString(messageTrace.getTraceId()));
        setRepresentation(interaction, Kieker2UmlUtil.getTraceRepresentation(messageTrace));

        return interaction;
    }

    /**
     * @param useCase             The UseCase to which this Interaction will be added.
     * @param traceRepresentation The traceRepresentation for the Interaction so it can be correctly identified.
     * @return The Interaction, either an existing one or the newly created one. The Interaction will have the traceRepresentation set in the id-Annotation.
     */
    static Optional<Interaction> getInteraction(final UseCase useCase, final String traceRepresentation) {
        requireNonNull(useCase, "useCase");
        requireNonNull(traceRepresentation, "traceRepresentation");

        return useCase.getOwnedBehaviors().stream()
                .filter(i -> i instanceof Interaction)
                .map(i -> (Interaction) i)
                .filter(i -> getRepresentation(i).map(s -> s.equals(traceRepresentation)).orElse(false))
                .findFirst();
    }

    /**
     * The Name is used as File-Name later in the process and simply is "Interaction-" + the count of the interaction.
     * count starts with zero.
     *
     * @param useCase The MessageTrace that is used to create the name.
     * @return The Name of the Trace beginning with "Interaction-" + COUNT
     */
    static String getInteractionName(final UseCase useCase) {
        return "Interaction-" + useCase.getOwnedBehaviors().size();
    }

    /**
     * Adds Lifelines, Messages and representations of the components
     * Note: The First Lifeline needs special treatment since no {@link org.eclipse.uml2.uml.BehaviorExecutionSpecification} will be created for it.
     * Creates the following Types in the UML2 Model:
     * * {@link org.eclipse.uml2.uml.Lifeline} - The Lifelines represent the different objects that are interaction within the Trace of the application
     * * {@link org.eclipse.uml2.uml.Message} - The Messages represent the calls that the objects in the application are making to each other
     * * {@link org.eclipse.uml2.uml.MessageOccurrenceSpecification} - This is required by UML2 and connects the Message with the lifeline
     * * {@link org.eclipse.uml2.uml.BehaviorExecutionSpecification} - This is required by UML2 and represents when an object of the application is active
     *
     * @param interaction - Representing the whole interaction all other Types are enclosed by this Type.
     * @param messages    - The Kieker Messages of the {@link MessageTrace}
     */
    static void addLifelines(final Interaction interaction, final List<AbstractMessage> messages) {
        requireNonNull(getModel(interaction));
        final Package staticView = Kieker2UmlUtil.getPackagedElement(getModel(interaction), STATIC_VIEW_COMPONENTS);
        // assumption: the messages are ordered
        int count = 0; // The count was introduced to have an additional separation option for Messages that have the same representation
        for (final AbstractMessage message : messages) {
            final AssemblyComponent senderComponent = message.getSendingExecution().getAllocationComponent().getAssemblyComponent();
            final AssemblyComponent receiverComponent = message.getReceivingExecution().getAllocationComponent().getAssemblyComponent();

            // getLifeline(name, ignoreCase, createOnDemand) <-- naming of the parameters
            final org.eclipse.uml2.uml.Lifeline senderLifeline = getLifeline(interaction, senderComponent);
            final org.eclipse.uml2.uml.Lifeline receiverLifeline = getLifeline(interaction, receiverComponent);

            // note that every lifeline will be a receiver at one point
            setRepresents(receiverLifeline, staticView, receiverComponent);
            setReferenceAnnotations(receiverLifeline, message.getReceivingExecution());

            final String messageRepresentation = Kieker2UmlUtil.getMessageRepresentation(message);
            createMessage(interaction, message, senderLifeline, receiverLifeline, messageRepresentation, count);
            count++;
        }
        setBehaviourSpecificationForFirstLifeline(messages.get(0), interaction, count - 1);
    }

    private static void setRepresents(final Lifeline lifeline, final Package staticView, final AssemblyComponent assemblyComponent) {
        if (isNull(lifeline.getRepresents())) {
            final Component component = UmlStaticViews.getComponent(staticView, assemblyComponent);
            final Parameter ownedParameter = lifeline.getInteraction().getOwnedParameter("Representation-" + component.getName(), component, false, true);
            lifeline.setRepresents(ownedParameter);
        }
    }

    private static Lifeline getLifeline(final Interaction interaction, final AssemblyComponent senderComponent) {
        return interaction.getLifeline(senderComponent.getIdentifier(), false, true);
    }

    private static void setBehaviourSpecificationForFirstLifeline(final AbstractMessage firstMessage, final Interaction interaction, final int finalCount) {
        final String messageId = Kieker2UmlUtil.getMessageRepresentation(firstMessage);
        final String lifelineName = firstMessage.getSendingExecution().getAllocationComponent().getAssemblyComponent().getIdentifier();
        final Lifeline lifeline = requireNonNull(interaction.getLifeline(lifelineName));

        // get all MOS
        final List<MessageOccurrenceSpecification> mosList = lifeline.getCoveredBys().stream()
                .filter(c -> c instanceof MessageOccurrenceSpecification)
                .map(c -> (MessageOccurrenceSpecification) c)
                .collect(Collectors.toList());

        // This assumes that the MOS are in order, which they should be if executed correctly
        final MessageOccurrenceSpecification startMos = mosList.get(0);
        final MessageOccurrenceSpecification finishMos = mosList.get(mosList.size() - 1);

        // open BES
        final BehaviorExecutionSpecification startBes = startBehaviourSpecification(interaction, lifeline, startMos, firstMessage.getSendingExecution().getOperation());
        setRepresentation(startBes, "'Entry'");
        setRepresentationCount(startBes, -1);
        setReferenceAnnotation(startBes, "OpenMessage", "'Entry'");
        setReferenceAnnotation(startBes, "OpenMessageCount", -1 + "");

        // close BES
        final BehaviorExecutionSpecification finishBes = getCurrentOpenBES(lifeline);
        finishBes.setFinish(finishMos);
        setReferenceAnnotation(finishBes, "CloseMessage", messageId);
        setReferenceAnnotation(finishBes, "CloseMessageCount",  finalCount + "");
    }

    static String getBesName(final Operation operation) {
        return BEHAVIOUR_EXECUTION_SPECIFICATION_PREFIX + operation.toString();
    }

    private static void createMessage(final Interaction interaction, final AbstractMessage message, final Lifeline senderLifeline, final Lifeline receiverLifeline, final String messageId, final int count) {
        requireNonNull(interaction, "interaction");
        requireNonNull(message, "message");
        requireNonNull(senderLifeline, "senderLifeline");
        requireNonNull(receiverLifeline, "receiverLifeline");

        final String messageLabel = getInterfaceName(message.getReceivingExecution().getOperation());
        final org.eclipse.uml2.uml.Message umlMessage = interaction.createMessage(messageLabel);
        final MessageSort messageSort = Kieker2UmlUtil.getMessageSort(message);
        umlMessage.setMessageSort(messageSort);

        final MessageOccurrenceSpecification mosSend = createMessageOccurrence(interaction, umlMessage, senderLifeline, messageLabel + "SendEvent");
        final MessageOccurrenceSpecification mosReceive = createMessageOccurrence(interaction, umlMessage, receiverLifeline, messageLabel + "ReceiveEvent");

        umlMessage.setSendEvent(mosSend);
        umlMessage.setReceiveEvent(mosReceive);

        final BehaviorExecutionSpecification bes;
        if (messageSort.equals(MessageSort.SYNCH_CALL_LITERAL)) {
            bes = startBehaviourSpecification(interaction, receiverLifeline, mosReceive, message.getReceivingExecution().getOperation());

            setRepresentation(bes, getBESRepresentation(messageId));
            setRepresentationCount(bes, count);
            setReferenceAnnotation(bes, "OpenMessage", messageId);
            setReferenceAnnotation(bes, "OpenMessageCount", count + "");
        } else if (messageSort.equals(MessageSort.REPLY_LITERAL)) {
            bes = getCurrentOpenBES(senderLifeline);
            bes.setFinish(mosSend); // the current mos will now the next BES

            setReferenceAnnotation(bes, "CloseMessage", messageId);
            setReferenceAnnotation(bes, "CloseMessageCount", count + "");
        }
        // set Metadata
        // uml message
        setRepresentation(umlMessage, messageId);
        setRepresentationCount(umlMessage, count);
        setReferenceAnnotations(umlMessage, message.getReceivingExecution());
        // message occurrence send
        setRepresentation(mosSend, getSendMOSRepresentation(messageId));
        setRepresentationCount(mosSend, count);
        setReferenceAnnotations(mosSend, message.getSendingExecution());
        // message occurrence receive
        setRepresentation(mosReceive, getReceiveMOSRepresentation(messageId));
        setRepresentationCount(mosReceive, count);
        setReferenceAnnotations(mosReceive, message.getReceivingExecution());
    }

    private static BehaviorExecutionSpecification getCurrentOpenBES(final Lifeline senderLifeline) {
        final List<BehaviorExecutionSpecification> list = senderLifeline.getCoveredBys().stream()
                .filter(cb -> cb instanceof BehaviorExecutionSpecification)
                .map(cb -> (BehaviorExecutionSpecification) cb)
                .filter(bes -> isNull(bes.getFinish()))
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new RuntimeException("There is no open BehaviorExecutionSpecification. At least one was expected.");
        }
        // get last opened BehaviorExecutionSpecification on lifeline.
        // this is possible since a single kieker-trace is sequential.
        return list.get(list.size() - 1);
    }

    private static BehaviorExecutionSpecification startBehaviourSpecification(final Interaction interaction, final Lifeline umlLifeline, final MessageOccurrenceSpecification messageOccurrenceReceive, final Operation operation) {
        final BehaviorExecutionSpecification behaviour = (BehaviorExecutionSpecification) interaction.createFragment(getBesName(operation), BEHAVIOUR_EXECUTION_E_CLASS);
        behaviour.getCovereds().add(umlLifeline);

        behaviour.setStart(messageOccurrenceReceive);
        return behaviour;
    }

    private static MessageOccurrenceSpecification createMessageOccurrence(final Interaction interaction, final org.eclipse.uml2.uml.Message umlMessage, final org.eclipse.uml2.uml.Lifeline lifeline, final String label) {
        final MessageOccurrenceSpecification fragment = (MessageOccurrenceSpecification) interaction.createFragment(label, MESSAGE_OCCURRENCE_E_CLASS);

        fragment.getCovereds().add(lifeline);
        fragment.setMessage(umlMessage);
        return fragment;
    }

    static void connectEntryLifelineToActor(final UseCase useCase) {
        final Model model = getModel(useCase);
        final org.eclipse.uml2.uml.Package dynamicView = getDynamicView(model);
        final Actor actor = getActor(dynamicView, useCase);

        useCase.getOwnedBehaviors().stream()
                .filter(b -> b instanceof Interaction)
                .map(b -> (Interaction) b)
                .filter(i -> nonNull(i.getLifeline(KIEKER_ENTRY_NAME)))
                .forEach(i -> {
                    final Lifeline lifeline = i.getLifeline(KIEKER_ENTRY_NAME);
                    lifeline.setRepresents(createAssociation(actor, i).getMemberEnd(actor.getName(), null));
                    MarteSupport.setGaWorkloadEvent(lifeline, "closed:1");
                });
    }

    static String getBESRepresentation(final String messageId) {
        return BEHAVIOUR_EXECUTION_SPECIFICATION_PREFIX + messageId;
    }

    static String getReceiveMOSRepresentation(final String messageId) {
        return RECEIVE_MESSAGE_OCCURRENCE_SPECIFICATION + messageId;
    }

    static String getSendMOSRepresentation(final String messageId) {
        return SEND_MESSAGE_OCCURRENCE_SPECIFICATION + messageId;
    }
}
