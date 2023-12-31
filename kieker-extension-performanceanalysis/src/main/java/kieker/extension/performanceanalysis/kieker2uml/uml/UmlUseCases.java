package kieker.extension.performanceanalysis.kieker2uml.uml;

import com.beust.jcommander.Strings;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.uml2.uml.Actor;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UseCase;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * This class handels the creation of UML UseCases and Actors.
 * This creates part of the dynamicView package.
 */
public class UmlUseCases {

    public static final EClass USE_CASE_E_CLASS = UMLFactory.eINSTANCE.createUseCase().eClass();
    public static final String KIEKER_ENTRY_NAME = "'Entry'";


    /**
     * set by the developers different cases how the use case / interaction relation is
     * see {@link UmlUseCases#getUseCase(String, Package, String)} method for usage.
     */
    public static String UC_MODE = "multi-interaction";

    static UseCase getUseCase(final Model model, final String useCaseName, final String traceRepresentation) {
        final org.eclipse.uml2.uml.Package dynamicView = getDynamicView(model);
        final UseCase useCase = getUseCase(useCaseName, dynamicView, traceRepresentation);
        final Actor actor = getActor(dynamicView, useCase);
        Kieker2UmlUtil.createAssociation(useCase, actor);
        return useCase;
    }

    private static UseCase getUseCase(final String useCaseName, final org.eclipse.uml2.uml.Package dynamicView, final String traceRepresentation) {

        if (UC_MODE.equals("single-interaction")) { // each interaction has their own use-case
            final List<UseCase> ucList = dynamicView.getPackagedElements().stream()
                    .filter(pe -> pe instanceof UseCase)
                    .map(uc -> (UseCase) uc)
                    .filter(uc -> uc.getName().contains(useCaseName))
                    .collect(Collectors.toList());
            return ucList.stream()
                    .filter(uc -> Kieker2UmlUtil.getRepresentation(uc).map(r -> r.equals(traceRepresentation)).orElse(false))
                    .findFirst()
                    .orElseGet(() -> {
                        final UseCase useCase = (UseCase) dynamicView.createPackagedElement(useCaseName + "-" + ucList.size(), USE_CASE_E_CLASS);
                        Kieker2UmlUtil.setRepresentation(useCase, traceRepresentation);
                        return useCase;
                    });
        }
        if (UC_MODE.equals("multi-interaction")) {
            final List<UseCase> ucList = dynamicView.getPackagedElements().stream()
                    .filter(pe -> pe instanceof UseCase)
                    .map(uc -> (UseCase) uc)
                    .filter(uc -> uc.getName().equals(useCaseName))
                    .collect(Collectors.toList());
            if (ucList.isEmpty()) {
                final UseCase useCase = (UseCase) dynamicView.createPackagedElement(useCaseName , USE_CASE_E_CLASS);
                Kieker2UmlUtil.setRepresentation(useCase, traceRepresentation);
                return useCase;
            }
            if (ucList.size() > 1) {
                final String collect = ucList.stream().map(Object::toString).collect(Collectors.joining(", "));
                throw new RuntimeException("To many use cases. Expected was exactly one. List: " + collect);
            }
            return ucList.get(0);
        }
        throw new RuntimeException("Unable to find appropriate UseCase Mode, please contact the developers.");
    }

    static Actor getActor(final org.eclipse.uml2.uml.Package dynamicView, final UseCase useCase) {
        requireNonNull(dynamicView, "dynamicView");
        final String actorName = KIEKER_ENTRY_NAME + "-" + useCase.getName();
        return dynamicView.getPackagedElements().stream()
                .filter(e -> UMLPackage.Literals.ACTOR.equals(e.eClass()))
                .filter(e -> actorName.equals(e.getName()))
                .filter(a -> Kieker2UmlUtil.isAssociateWith(a, useCase))
                .map(e -> (Actor) e)
                .findFirst()
                .orElseGet(() -> (Actor) dynamicView.createPackagedElement(actorName, UMLPackage.Literals.ACTOR));
    }

    static org.eclipse.uml2.uml.Package getDynamicView(final Model model) {
        requireNonNull(model, "model");
        return model.getPackagedElements().stream()
                .filter(pe -> pe.getName().equals("dynamicView"))
                .findFirst().map(pe -> (org.eclipse.uml2.uml.Package) pe)
                .orElseGet(() -> (org.eclipse.uml2.uml.Package) model.createPackagedElement("dynamicView", UMLPackage.Literals.PACKAGE));
    }

}
