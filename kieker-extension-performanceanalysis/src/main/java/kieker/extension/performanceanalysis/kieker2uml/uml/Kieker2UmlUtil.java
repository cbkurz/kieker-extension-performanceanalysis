package kieker.extension.performanceanalysis.kieker2uml.uml;

import kieker.model.system.model.AbstractMessage;
import kieker.model.system.model.Execution;
import kieker.model.system.model.MessageTrace;
import kieker.model.system.model.Operation;
import kieker.model.system.model.SynchronousCallMessage;
import kieker.model.system.model.SynchronousReplyMessage;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.MessageSort;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static kieker.extension.performanceanalysis.kieker2uml.uml.UmlComponents.getInterfaceName;

/**
 * This class provides standard methods using uml models.
 * In the model transformation Kieker2Uml a major factor is that information is not applied by stereotypes but with EAnnotations
 * This class provides several methods to support this factor.
 */
public class Kieker2UmlUtil {
    public static final EClass PACKAGE_E_CLASS = UMLFactory.eINSTANCE.createPackage().eClass();
    public static final String REFERENCE_ANNOTATION_NAME = "Reference";
    public static final String REPRESENTATION_ANNOTATION_NAME = "Representation";
    public static final String REPRESENTATION_NAME = "representation";
    public static final String REPRESENTATION_COUNT = "count";
    public static final String TRACE_IDS_SET_NAME = "AppliedIds";

    static org.eclipse.uml2.uml.Package getPackagedElement(final Model model, final String packageName) {
        return (org.eclipse.uml2.uml.Package) model.getPackagedElements().stream()
                .filter(p -> packageName.equals(p.getName()))
                .findFirst()
                .orElseGet(() -> model.createPackagedElement(packageName, PACKAGE_E_CLASS));
    }

    static void setAnnotationDetail(final Element element, final String annotationName, final String key, final String value) {
        final EAnnotation eAnnotation = element.getEAnnotations().stream()
                .filter(a -> a.getSource().equals(annotationName))
                .findFirst()
                .orElseGet(() -> element.createEAnnotation(annotationName));

        final EMap<String, String> details = eAnnotation.getDetails();
        if (details.containsKey(key)) {
            details.removeKey(key);
            details.put(key, value);
        } else {
            details.put(key, value);
        }
    }
    static Optional<String> getAnnotationDetail(final Element element, final String annotationName, final String key) {
        final EAnnotation eAnnotation = element.getEAnnotations().stream()
                .filter(a -> a.getSource().equals(annotationName))
                .findFirst()
                .orElseGet(() -> element.createEAnnotation(annotationName));

        return Optional.ofNullable(eAnnotation.getDetails().get(key));
    }

    public static Path saveModel(Model model, Path targetFile) {
        final ResourceSet resourceSet = new ResourceSetImpl();

        UMLResourcesUtil.init(resourceSet);

        Resource resource = resourceSet.createResource(URI.createURI(targetFile.toString()));
        resource.getContents().add(model);

        // And save
        try {
            resource.save(null); // no save options needed
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return targetFile;
    }

    public static Model loadModel(Path pathToModel) {

        // unclear why this is necessary,
        // however if this isn't initialized here again,
        // the program fails since the model will not be loaded when it is created by the InputModelValidator.class.
        URI typesUri = URI.createFileURI(pathToModel.toString());
        ResourceSet set = new ResourceSetImpl();

        set.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
        set.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        set.createResource(typesUri);
        Resource r = set.getResource(typesUri, true);

        return requireNonNull((Model) EcoreUtil.getObjectByType(r.getContents(), UMLPackage.Literals.MODEL));
    }

    public static Model createModel(final String name) {
        final Model model = UMLFactory.eINSTANCE.createModel();
        model.setName(name);
        return model;
    }

    /**
     * {@link MessageSort} is an enumeration of different kinds of messages.
     * This enumeration determines if it is a call or a reply.
     * This method only expects there to be 2 types {MessageSort.SYNCH_CALL_LITERAL} or {MessageSort.REPLY_LITERAL}
     * {@link MessageSort}
     * @param message the kieker trace message, two types are considered {@link SynchronousCallMessage} and {@link SynchronousReplyMessage} if neither are matched an exception is thrown.
     * @return MessageSort Literal
     */
    static MessageSort getMessageSort(final AbstractMessage message) {
        if (message instanceof SynchronousCallMessage) {
            return MessageSort.SYNCH_CALL_LITERAL;
        }
        if (message instanceof SynchronousReplyMessage) {
            return MessageSort.REPLY_LITERAL;
        }
        throw new RuntimeException("Unexpected message type of: " + message);
    }

    /**
     * References are there to enable a trace back where an element originated.
     * This method sets some default values
     * @param element the uml element
     * @param execution the kieker execution the element relates to.
     */
    static void setReferenceAnnotations(final Element element, final Execution execution) {
        setAnnotationDetail(element, REFERENCE_ANNOTATION_NAME, "package", execution.getOperation().getComponentType().getPackageName());
        setAnnotationDetail(element, REFERENCE_ANNOTATION_NAME, "class", execution.getOperation().getComponentType().getTypeName());
        setAnnotationDetail(element, REFERENCE_ANNOTATION_NAME, "fullQualifiedName", execution.getOperation().getComponentType().getFullQualifiedName());
        setAnnotationDetail(element, REFERENCE_ANNOTATION_NAME, "fullQualifiedNameSignature", execution.getOperation().toString());
        setAnnotationDetail(element, REFERENCE_ANNOTATION_NAME, "signature", execution.getOperation().getSignature().toString());
    }

    /**
     * References are there to enable a trace back where an element originated.
     * @param element the uml element
     * @param name the name of the reference
     * @param value the value of the reference
     */
    static void setReferenceAnnotation(final Element element, final String name, final String value) {
        setAnnotationDetail(element, REFERENCE_ANNOTATION_NAME, name, value);
    }

    static Optional<String> getReference(final Element element, final String key) {
        return getAnnotationDetailsMap(element, REFERENCE_ANNOTATION_NAME).map(entries -> entries.get(key));
    }

    static Optional<EMap<String, String>> getAnnotationDetailsMap(final Element element, final String annotationName) {
        return element.getEAnnotations().stream()
                .filter(a -> annotationName.equals(a.getSource()))
                .findFirst()
                .map(EAnnotation::getDetails);
    }

    static String removeInstanceInformation(final String name) {
        return name.replaceAll("\\$[0-9]*", "");
    }


    /**
     * The Representation of the message is independent of the execution time, starting time, trace id or
     * any other variable that can change within multiple executions of the same sequence of messages.
     * The message is represented by:
     * <ul>
     * <li>the full qualified name of the calling method and</li>
     * <li>the full qualified name of the called method.</li>
     * </ul>
     *
     * @param message the message to be represented as a string
     * @return the string representation of the message, independend of changing parameters
     */
    static String getMessageRepresentation(final AbstractMessage message) {
        final String sender = "Sender--" + getExecutionRepresentation(message.getSendingExecution());
        final String receiver = "--Receiver--" + getExecutionRepresentation(message.getReceivingExecution())
                + message.getReceivingExecution().getOperation().getComponentType().getFullQualifiedName()
                + message.getReceivingExecution().getOperation().getSignature().toString();
        return sender + receiver;
    }

    private static String getExecutionRepresentation(final Execution execution) {
        return execution.getOperation().getComponentType().getFullQualifiedName() + execution.getOperation().getSignature().toString();
    }

    /**
     * The Trace Representation is the concatenation of the contained messages.
     * @param messageTrace The messageTrace to be represented.
     * @return The string representing this Trace
     */
    static String getTraceRepresentation(final MessageTrace messageTrace) {
        return messageTrace.getSequenceAsVector().stream()
                .map(Kieker2UmlUtil::getMessageRepresentation)
                .collect(Collectors.joining());
    }

    static boolean isMessageEqual(AbstractMessage abstractMessage1, AbstractMessage abstractMessage2) {
        return getMessageRepresentation(abstractMessage1).equals(getMessageRepresentation(abstractMessage2));
    }

    /**
     * The equality shall determine if the same messages and the same sequence of messages are called.
     * The equality is determined by the String-Representations of the Traces.
     * <p>
     * Equality is *not* given by the objects the traces contain since they will be always different.
     * <p>
     * Traces have different, names, starting timestamps and execution times.
     * All these factors play a role in the equality of objects given in the trace.
     * @param messageTrace1 The first Trace
     * @param messageTrace2 The second Trace
     * @return Equality of Traces
     */
    static boolean isTraceEqual(final MessageTrace messageTrace1, final MessageTrace messageTrace2) {
        if (messageTrace1.getSequenceAsVector().size() != messageTrace2.getSequenceAsVector().size()) {
            return false; // different lengths in message Traces cannot be equal.
        }

        final String traceRep1 = getTraceRepresentation(messageTrace1);
        final String traceRep2 = getTraceRepresentation(messageTrace2);
        return traceRep1.equals(traceRep2); // The equality is Determined by the String representation of the Traces
    }

    static void setAnnotationSetEntry(final Element element, final String setName, final String entry) {
        setAnnotationDetail(element, setName, entry, null);
    }

    static Optional<Set<String>> getAnnotationSet(final Element element, final String setName) {
        return getAnnotationDetailsMap(element, setName).map(EMap::keySet);
    }

    static void setRepresentation(final Element element, final String representation) {
        EAnnotation idAnnotation = Optional.ofNullable(element.getEAnnotation(REPRESENTATION_ANNOTATION_NAME))
                .orElseGet(() -> element.createEAnnotation(REPRESENTATION_ANNOTATION_NAME));
        idAnnotation.getDetails().put(REPRESENTATION_NAME, representation);
    }

    static void setRepresentationCount(final Element element, final Integer count) {
        EAnnotation idAnnotation = Optional.ofNullable(element.getEAnnotation(REPRESENTATION_ANNOTATION_NAME))
                .orElseGet(() -> element.createEAnnotation(REPRESENTATION_ANNOTATION_NAME));
        idAnnotation.getDetails().put(REPRESENTATION_COUNT, count + "");

    }
    static Optional<String> getRepresentation(final Element element) {
        return Optional.ofNullable(element.getEAnnotation(REPRESENTATION_ANNOTATION_NAME))
                .map(a -> a.getDetails().get(REPRESENTATION_NAME));
    }

    static Optional<Integer> getRepresentationCount(final Element element) {
        return Optional.ofNullable(element.getEAnnotation(REPRESENTATION_ANNOTATION_NAME))
                .map(a -> a.getDetails().get(REPRESENTATION_COUNT))
                .map(Integer::parseInt);
    }

    static void addId(final NamedElement element, final String id) {
        setAnnotationSetEntry(element, TRACE_IDS_SET_NAME, id);
    }
    static Optional<Set<String>> getIds(final NamedElement element) {
        return getAnnotationSet(element, TRACE_IDS_SET_NAME);
    }

    static boolean isIdApplied(final NamedElement element, final String id) {
        return Optional.ofNullable(element.getEAnnotation(TRACE_IDS_SET_NAME))
                .map(annotation -> annotation.getDetails().containsKey(id))
                .orElse(false);
    }

    static Association createAssociation(final Type from, final Type to) {
        requireNonNull(from, "from");
        requireNonNull(to, "to");

        final Optional<Association> first = from.getAssociations().stream()
                .filter(a -> a.getMemberEnds().size() == 2)
                .filter(a -> a.getMembers().stream().anyMatch(me -> from.equals(((Property) me).getType())))
                .filter(a -> a.getMembers().stream().anyMatch(me -> to.equals(((Property) me).getType())))
                .findFirst();

        if (first.isPresent()) {
            return first.get();
        }

        // Some of these values are chosen at random and have no meaning besides being there.
        // The following is the parameterlist and their names:
        //   boolean end1IsNavigable, AggregationKind end1Aggregation, String end1Name, int end1Lower, int end1Upper,
        //   Type end1Type, boolean end2IsNavigable, AggregationKind end2Aggregation, String end2Name, int end2Lower, int end2Upper
        return from.createAssociation(true, AggregationKind.NONE_LITERAL, to.getName(), 1, 1,
                to, true, AggregationKind.NONE_LITERAL, from.getName(), 1, 1);
    }

    /**
     * There are circumstances where the "getModel()" method returns null.
     * In such cases this method can be called
     * @param element some element
     * @return the Model
     * @throws NullPointerException In case the element or some owner of the element returns null and the Model has not been reached.
     */
    static Model getModel(final Element element) {
        requireNonNull(element, "element");
        if (element instanceof Model) {
            return (Model) element;
        }
        final Element owner = element.getOwner();
        requireNonNull(owner, "owner");
        if (owner instanceof Model) {
            return (Model) owner;
        } else {
            return getModel(owner);
        }
    }

    static boolean isAssociateWith(final Element from, final Element to) {
        requireNonNull(from, "from");
        requireNonNull(to, "to");
        return from.getRelationships().stream()
                .filter(r -> r instanceof Association)
                .map(r -> (Association) r)
                .anyMatch(a -> a.getEndTypes().contains(to));
    }
}
