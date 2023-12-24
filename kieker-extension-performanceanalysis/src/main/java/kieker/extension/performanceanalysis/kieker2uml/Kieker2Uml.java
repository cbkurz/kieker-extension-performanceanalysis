package kieker.extension.performanceanalysis.kieker2uml;

import kieker.extension.performanceanalysis.cli.Kieker2UmlCli;
import kieker.extension.performanceanalysis.cli.RunAllTransformationsCli;
import kieker.extension.performanceanalysis.epsilon.Util;
import kieker.extension.performanceanalysis.kieker2uml.teetime.Kieker2UmlTeeTimeService;

public class Kieker2Uml implements Runnable {

    final private String[] args;
    private final boolean systemExitAfterExecution;
    private final Kieker2UmlCli cli;

    public Kieker2Uml(final String[] args) {
        this(args, new Kieker2UmlCli(), true);
    }
    public Kieker2Uml(final String[] args, final Kieker2UmlCli cli, final boolean systemExitAfterExecution) {
        String[] newArgs = new String[args.length - 1];

        for (int i = 1; i < args.length; i++) {
            newArgs[i-1] = args[i];
        }

        this.args = newArgs;
        this.cli = cli;
        this.systemExitAfterExecution = systemExitAfterExecution;
    }

    @Override
    public void run() {
        final Kieker2UmlTeeTimeService service = Kieker2UmlTeeTimeService.getInstance(this.cli);
        final int kieker2Uml = service.run("Kieker2Uml", "kieker-2-uml", args, service.getParameters());
        Util.validateUmlModel(service.getParameters().getModelPath());
        if (systemExitAfterExecution) {
            System.exit(kieker2Uml);
        }
    }
}
