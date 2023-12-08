package kieker.extension.performanceanalysis.kieker2uml.teetime;

import kieker.analysis.architecture.trace.execution.ExecutionRecordTransformationStage;
import kieker.analysis.architecture.trace.reconstruction.TraceReconstructionStage;
import kieker.analysis.generic.DynamicEventDispatcher;
import kieker.analysis.generic.IEventMatcher;
import kieker.analysis.generic.ImplementsEventMatcher;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.extension.performanceanalysis.cli.Kieker2UmlCli;
import kieker.model.repository.SystemModelRepository;
import kieker.tools.source.LogsReaderCompositeStage;
import teetime.framework.Configuration;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class TeeTimeConfiguration extends Configuration {

    private SystemModelRepository systemModelRepository;
    private LogsReaderCompositeStage reader;
    private DynamicEventDispatcher dispatcher;
    private IEventMatcher<? extends OperationExecutionRecord> operationExecutionDispatcherOutput;
    private TraceReconstructionStage traceReconstructionStage;
    private ExecutionRecordTransformationStage executionRecordTransformationStage;

    private UmlModelFilter sequenceDiagramFilter;


    public TeeTimeConfiguration(final Kieker2UmlCli parameters) {
        setup(parameters);

        this.connectPorts(reader.getOutputPort(), dispatcher.getInputPort());
        this.connectPorts(operationExecutionDispatcherOutput.getOutputPort(), this.executionRecordTransformationStage.getInputPort());
        this.connectPorts(this.executionRecordTransformationStage.getOutputPort(), this.traceReconstructionStage.getInputPort());
        this.connectPorts(this.traceReconstructionStage.getMessageTraceOutputPort(), sequenceDiagramFilter.getInputPort());
    }

    private void setup(final Kieker2UmlCli parameters) {
        this.systemModelRepository = new SystemModelRepository();

        this.reader = new LogsReaderCompositeStage(
                parameters.getInputDirectories().stream().map(Path::toFile).collect(Collectors.toList()),
                false,
                null
        );

        this.dispatcher = new DynamicEventDispatcher(null, false, true, false);
        this.operationExecutionDispatcherOutput = new ImplementsEventMatcher<>(OperationExecutionRecord.class, null);
        dispatcher.registerOutput(operationExecutionDispatcherOutput);

        this.traceReconstructionStage = new TraceReconstructionStage(this.systemModelRepository, TimeUnit.MILLISECONDS, true, Long.MAX_VALUE);

        this.executionRecordTransformationStage = new ExecutionRecordTransformationStage(this.systemModelRepository);
        executionRecordTransformationStage.declareActive();

        this.sequenceDiagramFilter = new UmlModelFilter(this.systemModelRepository, parameters.getModelPath(), parameters.getUseCaseName() );
    }

}
