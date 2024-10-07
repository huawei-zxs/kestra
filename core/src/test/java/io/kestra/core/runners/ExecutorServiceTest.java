package io.kestra.core.runners;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.flows.Concurrency;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.flows.State;
import io.kestra.plugin.core.debug.Return;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class ExecutorServiceTest {
    @Inject
    ExecutorService executorService;

    private Flow flow(Concurrency.Behavior behavior) {
        return Flow.builder()
            .id("flow")
            .namespace("io.kestra.test")
            .revision(1)
            .concurrency(Concurrency.builder().limit(1).behavior(behavior).build())
            .tasks(List.of(
                Return.builder()
                    .id("return")
                    .type(Return.class.getName())
                    .format("ok")
                    .build()
            ))
            .build();
    }

    @Test
    void shouldQueueWhenCountEqualsLimit() {
        Flow flow = flow(Concurrency.Behavior.QUEUE);
        Execution execution = Execution.newExecution(flow, null, null, Optional.empty());
        Executor executor = new Executor(execution, null).withFlow(flow);

        Executor result = executorService.checkConcurrencyLimit(executor, flow, execution, 1);

        assertThat(result.getExecutionRunning(), notNullValue());
        assertThat(result.getExecutionRunning().getConcurrencyState(), is(ExecutionRunning.ConcurrencyState.QUEUED));
        assertThat(result.getExecution().getState().getCurrent(), is(State.Type.QUEUED));
    }

    @Test
    void shouldCancelWhenCountEqualsLimit() {
        Flow flow = flow(Concurrency.Behavior.CANCEL);
        Execution execution = Execution.newExecution(flow, null, null, Optional.empty());
        Executor executor = new Executor(execution, null).withFlow(flow);

        Executor result = executorService.checkConcurrencyLimit(executor, flow, execution, 1);

        assertThat(result.getExecution().getState().getCurrent(), is(State.Type.CANCELLED));
    }

    @Test
    void shouldFailWhenCountEqualsLimit() {
        Flow flow = flow(Concurrency.Behavior.FAIL);
        Execution execution = Execution.newExecution(flow, null, null, Optional.empty());
        Executor executor = new Executor(execution, null).withFlow(flow);

        Executor result = executorService.checkConcurrencyLimit(executor, flow, execution, 1);

        assertThat(result.getException(), notNullValue());
    }

    @Test
    void shouldNotThrottleWhenCountBelowLimit() {
        Flow flow = flow(Concurrency.Behavior.QUEUE);
        Execution execution = Execution.newExecution(flow, null, null, Optional.empty());
        Executor executor = new Executor(execution, null).withFlow(flow);

        Executor result = executorService.checkConcurrencyLimit(executor, flow, execution, 0);

        assertThat(result.getExecutionRunning(), notNullValue());
        assertThat(result.getExecutionRunning().getConcurrencyState(), is(ExecutionRunning.ConcurrencyState.RUNNING));
        assertThat(result.getExecution().getState().getCurrent(), is(State.Type.CREATED));
    }
}
