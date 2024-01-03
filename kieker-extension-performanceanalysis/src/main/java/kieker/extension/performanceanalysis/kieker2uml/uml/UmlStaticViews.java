package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.AbstractMessage;
import kieker.model.system.model.AllocationComponent;
import kieker.model.system.model.AssemblyComponent;
import kieker.model.system.model.Execution;
import kieker.model.system.model.ExecutionContainer;
import kieker.model.system.model.MessageTrace;
import kieker.model.system.model.Operation;
import org.eclipse.uml2.uml.Artifact;
import org.eclipse.uml2.uml.Component;
import org.eclipse.uml2.uml.Deployment;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.Manifestation;
import org.eclipse.uml2.uml.MessageSort;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Node;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.addId;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getTraceRepresentation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.isIdApplied;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setReferenceAnnotation;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setReferenceAnnotations;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlInteractions.getBesName;

/**
 * This class adds the static Views to the UML model:
 *  - staticView-Components
 *  - deploymentView
 * The staticView-Components contains:
 *  - Component - each component is manifested in an artifact
 *  - Operation - each operation is part of a Component and corresponds to an Interface
 *  - Interface - is the representation for an Execution in the Kieker-Trace
 *  - Usage - Is the representation of a Message in the Kieker-Trace, it shows that one interface requires another.
 * The deploymentView contains:
 *  - Node - the server the application runs on, it is mapped to the Kieker ExecutionContainer
 *  - Artifact - this is deployed in the node and is a manifestation of a Component, it is mapped to a Kieker AllocationComponent
 *
 * This Class is also responsible to create the connections (manifestation, deployment, interface realization) between the different Elements.
 */
public class UmlStaticViews {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmlStaticViews.class);
    public static final String DEPLOYMENT_VIEW = "deploymentView";
    public static final String STATIC_VIEW_COMPONENTS = "staticView-components";


    static void addComponentsAndDeployment(final Model model, final MessageTrace messageTrace) {
        requireNonNull(model, "model");
        requireNonNull(messageTrace, "messageTrace");

        final String traceRepresentation = getTraceRepresentation(messageTrace);
        final org.eclipse.uml2.uml.Package staticView = Kieker2UmlUtil.getPackagedElement(model, STATIC_VIEW_COMPONENTS);
        final org.eclipse.uml2.uml.Package deploymentView = Kieker2UmlUtil.getPackagedElement(model, DEPLOYMENT_VIEW);

        // if the trace with the representation is already applied, no action is required
        if (isIdApplied(staticView, traceRepresentation) && isIdApplied(deploymentView, traceRepresentation)) {
            LOGGER.info("Trace was already applied to the componentView and the deploymentView. TraceId: " + traceRepresentation);
            return;
        }

        for (final AbstractMessage message : messageTrace.getSequenceAsVector()) {

            if (Kieker2UmlUtil.getMessageSort(message).equals(MessageSort.REPLY_LITERAL)) {
                continue;
            }

            // sender
            // uml elements
            final Component senderComponent = getComponent(staticView, message.getSendingExecution().getAllocationComponent().getAssemblyComponent());
            final Interface senderInterface = getInterface(staticView, message.getSendingExecution());
            final Node senderNode = getNode(deploymentView, message.getSendingExecution().getAllocationComponent().getExecutionContainer());
            final Artifact senderArtifact = getArtifact(deploymentView, message.getSendingExecution().getAllocationComponent());
            createOperation(senderComponent, message.getSendingExecution().getOperation());

            // connection
            doConnections(senderNode, senderArtifact, senderComponent, senderInterface);

            // receiver
            // uml elements
            final Component receiverComponent = getComponent(staticView, message.getReceivingExecution().getAllocationComponent().getAssemblyComponent());
            final Interface receiverInterface = getInterface(staticView, message.getReceivingExecution());
            final Node receiverNode = getNode(deploymentView, message.getReceivingExecution().getAllocationComponent().getExecutionContainer());
            final Artifact receiverArtifact = getArtifact(deploymentView, message.getReceivingExecution().getAllocationComponent());
            createOperation(receiverComponent, message.getReceivingExecution().getOperation());

            // connection
            doConnections(receiverNode, receiverArtifact, receiverComponent, receiverInterface);

            // sender uses receiver
            getUsage(staticView, senderInterface, receiverInterface);

        }

        // finnish
        addId(staticView, traceRepresentation);
        addId(deploymentView, traceRepresentation);
    }

    /**
     * Creates an operation of the component.
     * The Operation holds a reference to the BES that represent it, this is for easier lookup in the Uml2Lqn transformation
     * @param component the {@link Component} that is the owner of the {@link org.eclipse.uml2.uml.Operation}
     * @param operation the Kieker {@link Operation} that is the provider for the {@link org.eclipse.uml2.uml.Operation}
     * @return the uml {@link org.eclipse.uml2.uml.Operation} created
     */
    private static org.eclipse.uml2.uml.Operation createOperation(final Component component, final Operation operation) {
        final org.eclipse.uml2.uml.Operation ownedOperation = component.getOwnedOperation(getInterfaceName(operation), null, null, false, true);
        final String besName = getBesName(operation);
        setReferenceAnnotation(ownedOperation, "BES", besName);
        return ownedOperation;
    }

    static String getInterfaceName(final Operation operation) {
        final String name = operation.getSignature().toString();
        if (name.contains("<init>")) { // "<init>" is the representation of calling a constructor
            return name.replaceAll("<init>", operation.getComponentType().getTypeName()); // create constructor with name instead of "<init>"
        }
        return name;
    }

    static Node getNode(final Package deploymentView, final ExecutionContainer nodeName) {
        return (Node) deploymentView.getPackagedElement(nodeName.getIdentifier(), false, UMLPackage.Literals.NODE, true);
    }

    static Artifact getArtifact(final Package deploymentView, final AllocationComponent allocationComponent) {
        return (Artifact) deploymentView.getPackagedElement(allocationComponent.getIdentifier(), false, UMLPackage.Literals.ARTIFACT, true);
    }

    static Component getComponent(final Package staticView, final AssemblyComponent component) {
        // getPackagedElement(String name, boolean ignoreCase, EClass eClass, boolean createOnDemand) <-- names of the parameters
        return (Component) staticView.getPackagedElement(component.getIdentifier(), false, UMLPackage.Literals.COMPONENT, true);
    }

    /**
     * A Usage defines if one interface uses another.
     * @return The Usage
     */
    private static Usage getUsage(final org.eclipse.uml2.uml.Package staticView, final Interface sender, final Interface receiver) {
        return staticView.getPackagedElements().stream()
                .filter(pe -> pe instanceof Usage)
                .map(pe -> (Usage) pe)
                .filter(u -> nonNull(u.getClient(sender.getName())))
                .filter(u -> nonNull(u.getSupplier(receiver.getName())))
                .findFirst()
                .orElseGet(() -> sender.createUsage(receiver));
    }

    private static Interface getInterface(final Package staticView, final Execution execution) {
        return (Interface) staticView.getPackagedElement(getInterfaceName(execution.getOperation()), false, UMLPackage.Literals.INTERFACE, true);
    }

    /**
     * Creates the different connections required for the introduced Elements.
     * Node -deploys-> Artifact
     * Artifact -manifests-> Component
     * Component -realizes-> Interface
     *
     * Additionally, an Operation on the Component is created.
     *
     * @param node
     * @param artifact
     * @param component
     * @param anInterface
     */
    private static void doConnections(final Node node, final Artifact artifact, final Component component, final Interface anInterface) {
        component.getInterfaceRealization(anInterface.getName(), anInterface, false, true);

        final Manifestation m = artifact.getManifestation(component.getName(), component, false, true);
        final Deployment deployment = node.getDeployment("NodeDeployment-" + node.getName(), false, true);
        if (!deployment.getDeployedArtifacts().contains(artifact)) {
            deployment.getDeployedArtifacts().add(artifact);
        }
    }
}
