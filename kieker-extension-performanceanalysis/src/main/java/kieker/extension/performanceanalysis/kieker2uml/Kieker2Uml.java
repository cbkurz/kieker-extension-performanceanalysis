package kieker.extension.performanceanalysis.kieker2uml;

import kieker.extension.performanceanalysis.kieker2uml.teetime.Kieker2UmlTeeTimeService;

public class Kieker2Uml implements Runnable {

    final private String[] args;

    public Kieker2Uml(final String[] args) {
        this.args = args;
    }

    @Override
    public void run() {
        final Kieker2UmlTeeTimeService service = Kieker2UmlTeeTimeService.getInstance();
        System.exit(service.run("Kieker2Uml", "kieker-2-uml", args, service.getParameters()));
    }
}
