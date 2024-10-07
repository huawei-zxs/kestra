package io.kestra.core.schedulers;

import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.flows.Flow;
import io.kestra.core.models.triggers.AbstractTrigger;
import io.kestra.core.models.triggers.PollingTriggerInterface;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.models.triggers.TriggerService;
import io.kestra.core.runners.FlowListeners;
import io.kestra.core.runners.TestMethodScopedWorker;
import io.kestra.core.runners.Worker;
import io.kestra.core.tasks.test.Sleep;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.jdbc.runner.JdbcScheduler;
import jakarta.inject.Inject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class SchedulerDedupTest extends AbstractSchedulerTest {
    @Inject
    protected FlowListeners flowListenersService;

    @Test
    void triggerIsNotFiredWhilePreviousExecutionIsRunning() throws Exception {
        // mock flow listeners
        FlowListeners flowListenersServiceSpy = spy(this.flowListenersService);

        AlwaysTrigger trigger = AlwaysTrigger.builder()
            .id("always")
            .type(AlwaysTrigger.class.getName())
            .build();

        // the flow task sleeps 30s so the first execution stays running
        // during the whole observation window
        Flow flow = Flow.builder()
            .id(IdUtils.create())
            .namespace("io.kestra.unittest")
            .revision(1)
            .triggers(List.of(trigger))
            .tasks(List.of(Sleep.builder()
                .id("sleep")
                .type(Sleep.class.getName())
                .duration(30000L)
                .build()))
            .build();

        doReturn(List.of(flow))
            .when(flowListenersServiceSpy)
            .flows();

        CountDownLatch firstExecution = new CountDownLatch(1);
        Set<String> executionIds = ConcurrentHashMap.newKeySet();

        // scheduler
        try (
            AbstractScheduler scheduler = new JdbcScheduler(
                applicationContext,
                flowListenersServiceSpy
            );
            Worker worker = applicationContext.createBean(TestMethodScopedWorker.class, IdUtils.create(), 8, null)
        ) {
            // wait for execution
            Flux<Execution> receive = TestsUtils.receive(executionQueue, either -> {
                Execution execution = either.getLeft();
                if (execution.getFlowId().equals(flow.getId())) {
                    executionIds.add(execution.getId());
                    firstExecution.countDown();
                }
            });

            // start the worker as it executes polling triggers
            worker.run();
            scheduler.run();

            // wait for the first execution to be triggered
            assertThat(firstExecution.await(1, TimeUnit.MINUTES), is(true));

            // The first execution is still running (its task sleeps 30s).
            // The scheduler must NOT fire the trigger again while a previous execution is still running:
            // we wait 12s and assert that no new execution has been created.
            Thread.sleep(12000);

            // needed for RetryingTest to work since there is no context cleaning between method => we have to clear assertion receiver manually
            receive.blockLast();

            assertThat(executionIds.size(), is(1));
        }
    }

    @SuperBuilder
    @ToString
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    public static class AlwaysTrigger extends AbstractTrigger implements PollingTriggerInterface {
        @Builder.Default
        private final Duration interval = Duration.ofSeconds(2);

        @Override
        public Optional<Execution> evaluate(ConditionContext conditionContext, TriggerContext context) {
            return Optional.of(TriggerService.generateExecution(this, conditionContext, context, Collections.emptyMap()));
        }
    }
}
