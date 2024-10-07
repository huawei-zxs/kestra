package io.kestra.core.server;

import io.kestra.core.junit.annotations.KestraTest;
import io.micronaut.context.ApplicationContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KestraTest
class ServerInstanceFactoryTest {

    @Inject
    ApplicationContext applicationContext;

    @Test
    void shouldCreateServerInstanceFromContext() {
        ServerInstanceFactory factory = applicationContext.getBean(ServerInstanceFactory.class);
        Assertions.assertNotNull(factory);

        ServerInstance instance = factory.newServerInstance();
        Assertions.assertNotNull(instance);
        Assertions.assertNotNull(instance.id());
        Assertions.assertNotNull(instance.type());
        Assertions.assertNotNull(instance.hostname());
    }
}
