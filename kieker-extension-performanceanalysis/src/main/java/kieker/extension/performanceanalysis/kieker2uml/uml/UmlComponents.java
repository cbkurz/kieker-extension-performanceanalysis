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
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.addTraceId;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.isTraceApplied;

public class UmlComponents {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmlComponents.class);
    public static final String DEPLOYMENT_VIEW = "deploymentView";
    public static final String STATIC_VIEW_COMPONENTS = "staticView-components";


    static void addComponentsAndDeployment(final Model model, final MessageTrace messageTrace) {
        requireNonNull(model, "model");
        requireNonNull(messageTrace, "messageTrace");

        final org.eclipse.uml2.uml.Package staticView = Kieker2UmlUtil.getPackagedElement(model, STATIC_VIEW_COMPONENTS);
        final org.eclipse.uml2.uml.Package deploymentView = Kieker2UmlUtil.getPackagedElement(model, DEPLOYMENT_VIEW);

        if (isTraceApplied(staticView, messageTrace.getTraceId()) && isTraceApplied(deploymentView, messageTrace.getTraceId())) {
            LOGGER.info("Trace was already applied to the componentView and the deploymentView. TraceId: " + messageTrace.getTraceId());
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

            // connection
            doConnections(senderNode, senderArtifact, senderComponent, senderInterface);

            // receiver
            // uml elements
            final Component receiverComponent = getComponent(staticView, message.getReceivingExecution().getAllocationComponent().getAssemblyComponent());
            final Interface receiverInterface = getInterface(staticView, message.getReceivingExecution());
            final Node receiverNode = getNode(deploymentView, message.getReceivingExecution().getAllocationComponent().getExecutionContainer());
            final Artifact receiverArtifact = getArtifact(deploymentView, message.getReceivingExecution().getAllocationComponent());

            // connection
            doConnections(receiverNode, receiverArtifact, receiverComponent, receiverInterface);

            // sender uses receiver
            getUsage(staticView, senderInterface, receiverInterface);

        }

        // finnish
        addTraceId(staticView, messageTrace);
        addTraceId(deploymentView, messageTrace);
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

    static Artifact getArtifact(final Package deploymentView, final AllocationComponent artifactName) {
        return (Artifact) deploymentView.getPackagedElement(artifactName.getIdentifier(), false, UMLPackage.Literals.ARTIFACT, true);
    }

    static Component getComponent(final Package staticView, final AssemblyComponent component) {
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

    private static Interface getInterface(final Package staticView, final Execution interfaceName) {
        return (Interface) staticView.getPackagedElement(getInterfaceName(interfaceName.getOperation()), false, UMLPackage.Literals.INTERFACE, true);
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
        component.getOwnedOperation(anInterface.getName(), null, null, false, true);
        component.getInterfaceRealization(anInterface.getName(), anInterface, false, true);

        artifact.getManifestation("ArtifactManifestation-" + component.getName(), component, false, true);
        final Deployment deployment = node.getDeployment("NodeDeployment-" + node.getName(), false, true);
        if (!deployment.getDeployedArtifacts().contains(artifact)) {
            deployment.getDeployedArtifacts().add(artifact);
        }
    }
}
