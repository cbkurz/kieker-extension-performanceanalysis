package kieker.extension.performanceanalysis.epsilon;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.util.UMLUtil;

import static java.util.Objects.requireNonNull;

public class UmlModel extends EmfModel {

    @Override
    protected ResourceSet createResourceSet() {
        ResourceSet set = super.createResourceSet();
        UMLUtil.init(set);
        set.getPackageRegistry().put(UMLPackage.eINSTANCE.getNsURI(), UMLPackage.eINSTANCE);
        set.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        return requireNonNull(set);
    }
}
