package kieker.extension.performanceanalysis.kieker2uml.teetime;

import com.beust.jcommander.JCommander;
import kieker.common.configuration.Configuration;
import kieker.common.exception.ConfigurationException;
import kieker.extension.performanceanalysis.cli.Kieker2UmlCli;
import kieker.tools.common.AbstractService;

import java.nio.file.Path;

import static java.lang.Boolean.FALSE;

public class Kieker2UmlTeeTimeService extends AbstractService<TeeTimeConfiguration, Kieker2UmlCli> {

    private final Kieker2UmlCli kieker2UmlCli = new Kieker2UmlCli();
    private Kieker2UmlTeeTimeService() {
    }

    public static Kieker2UmlTeeTimeService getInstance() {
        return new Kieker2UmlTeeTimeService();
    }

    @Override
    protected TeeTimeConfiguration createTeetimeConfiguration() throws ConfigurationException {
        return new TeeTimeConfiguration(this.kieker2UmlCli);
    }

    /**
     * This method shall check the parameters to their correctness.
     * However, this is not necessary since this can be implemented directly into JCommander
     * For details check the {@link Kieker2UmlCli} class.
     * </p>
     * the help parameter has to be read out and is handled by {@link kieker.tools.common.AbstractLegacyTool#run(String, String, String[], Object)} correctly.
     */
    @Override
    protected boolean checkParameters(final JCommander commander) throws ConfigurationException {
        return true;
    }

    @Override
    protected Path getConfigurationPath() {
        return null;
    }

    @Override
    protected boolean checkConfiguration(final Configuration configuration, final JCommander commander) {
        return true;
    }

    @Override
    protected void shutdownService() {
        // nothing special to shutdown
    }

    public Kieker2UmlCli getParameters() {
        return kieker2UmlCli;
    }
}
