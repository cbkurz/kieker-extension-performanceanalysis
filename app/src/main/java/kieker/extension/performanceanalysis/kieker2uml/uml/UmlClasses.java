package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.AbstractMessage;
import kieker.model.system.model.MessageTrace;
import kieker.model.system.model.Operation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.MessageSort;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.getMessageSort;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setAnnotationDetail;
import static kieker.extension.performanceanalysis.kieker2uml.uml.Kieker2UmlUtil.setReferenceAnnotations;

public class UmlClasses {

    final static private EClass CLASS_E_CLASS = UMLFactory.eINSTANCE.createClass().eClass();
    static void addClasses(final Model model, final MessageTrace messageTrace) {
        requireNonNull(model, "model");
        requireNonNull(messageTrace, "messageTrace");

        final org.eclipse.uml2.uml.Package staticView = Kieker2UmlUtil.getPackagedElement(model, "staticView-classes");

        for (final AbstractMessage message : messageTrace.getSequenceAsVector()) {

            if (getMessageSort(message).equals(MessageSort.REPLY_LITERAL)) {
                // replys of messages do not need to be handled since both the receiver and the sender are added otherwise
                // It also does not add a required operation to the sender since it is a reply to a call and not the call itself.
                continue;
            }
            final org.eclipse.uml2.uml.Operation sender = getOperation(message.getSendingExecution().getOperation(), staticView);
            final org.eclipse.uml2.uml.Operation receiver = getOperation(message.getReceivingExecution().getOperation(), staticView);

            setReferenceAnnotations(sender, message.getSendingExecution());
            setReferenceAnnotations(receiver, message.getReceivingExecution());
            addDependency(sender, message.getReceivingExecution().getOperation());

            Kieker2UmlUtil.createAssociation(sender.getClass_(), receiver.getClass_());
        }
    }

    private static void addDependency(final org.eclipse.uml2.uml.Operation from, final Operation operation) {
        setAnnotationDetail(from, "CallsToQualifiedNames", operation.toString(), null); // TODO: this does not jet add all dependencies but only one.
    }

    private static org.eclipse.uml2.uml.Operation getOperation(final Operation operation, final org.eclipse.uml2.uml.Package staticView) {

        // Traverse over all Classes, get the Operations and find the one with the qualified name of the kieker Operation.
        final Optional<org.eclipse.uml2.uml.Operation> umlOperation = staticView.getPackagedElements().stream()
                .filter(pe -> CLASS_E_CLASS.equals(pe.eClass()))
                .map(pe -> (org.eclipse.uml2.uml.Class) pe)
                .filter(c -> Kieker2UmlUtil.removeInstanceInformation(operation.getComponentType().getFullQualifiedName()).equals(c.getName()))
                .flatMap(c -> c.getOperations().stream())
                .filter(op -> operation.getSignature().toString().equals(op.getName()))
                .findFirst();

        if (umlOperation.isEmpty()) {
            final Class umlClass = createUmlClass(Kieker2UmlUtil.removeInstanceInformation(operation.getComponentType().getFullQualifiedName()), staticView);
            return umlClass.createOwnedOperation(operation.getSignature().toString(), null, null);
        }

        return umlOperation.get();
    }


    private static org.eclipse.uml2.uml.Class createUmlClass(final String clazzName, final org.eclipse.uml2.uml.Package staticView) {
        return staticView.getPackagedElements().stream()
                .filter(pe -> clazzName.equals(pe.getName()))
                .findFirst()
                .map(pe -> (org.eclipse.uml2.uml.Class) pe)
                .orElseGet(() -> (org.eclipse.uml2.uml.Class) staticView.createPackagedElement(clazzName, CLASS_E_CLASS));
    }
}
