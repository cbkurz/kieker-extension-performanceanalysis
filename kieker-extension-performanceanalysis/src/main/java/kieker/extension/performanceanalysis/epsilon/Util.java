package kieker.extension.performanceanalysis.epsilon;

import org.eclipse.epsilon.evl.execute.UnsatisfiedConstraint;
import org.eclipse.epsilon.evl.launch.EvlRunConfiguration;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class Util {


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

}
