package kieker.extension.performanceanalysis.kieker2uml;

import kieker.extension.performanceanalysis.kieker2uml.teetime.Kieker2UmlTeeTimeService;

import java.util.Arrays;

public class Kieker2Uml implements Runnable {

    final private String[] args;

    public Kieker2Uml(final String[] args) {
        String[] newArgs = new String[args.length - 1];

        for (int i = 1; i < args.length; i++) {
            newArgs[i-1] = args[i];
        }

        this.args = newArgs;
    }

    @Override
    public void run() {
        final Kieker2UmlTeeTimeService service = Kieker2UmlTeeTimeService.getInstance();
        System.exit(service.run("Kieker2Uml", "kieker-2-uml", args, service.getParameters()));
    }
}
