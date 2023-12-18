package kieker.extension.performanceanalysis.epsilon;

import org.eclipse.epsilon.eol.models.Model;
import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.eclipse.epsilon.evl.launch.EvlRunConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {

    public static final Path UML_VALIDATION_SCRIPT = getResource("Uml2Lqn/UmlValidation.evl");
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    public static Path getResource(final String path) {
        try {
            return Paths.get(Util.class.getClassLoader().getResource(path).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void validate(final EvlRunConfiguration validationConfig) {
        validationConfig.run();
        @SuppressWarnings("unchecked") final Set<UnsatisfiedConstraint> set = (Set<UnsatisfiedConstraint>) validationConfig.get();


        if (set.isEmpty()) {
            return; // no constrains met
        }

        final String collect = set.stream()
                .map(Util::getConstraintAsString)
                .collect(Collectors.joining("\n"));
        final boolean constraintTriggered = set.stream().anyMatch(c -> !c.getConstraint().isCritique());
        if (!constraintTriggered) {
            System.out.println(collect);
            return;
        }
        throw new RuntimeException("Constraints that were not met: " + set.size() + "\n" + collect);
    }

    private static String getConstraintAsString(final UnsatisfiedConstraint constraint) {
        final String type = constraint.getConstraint().isCritique() ? "Critique" : "Constraint";
        return type + ": " + constraint.getConstraint() + " -> " + constraint.getMessage();
    }

    public static void validateUmlModel(final Path modelPath) {
        final Model uml = EpsilonModelBuilder.getInstance()
                .umlModel()
                .modelName("UML")
                .modelPath(modelPath)
                .readOnly(true)
                .storeOnDisposal(false)
                .build();
        validateUmlModel(uml);
    }

    public static void validateUmlModel(final Model uml) {
        uml.setName("UML");
        final EvlRunConfiguration validationConfig = EvlRunConfiguration.Builder()
                .withScript(UML_VALIDATION_SCRIPT)
                .withModel(uml)
                .build();
        validate(validationConfig);
        LOGGER.info("Model successfully validated.");
    }
}
