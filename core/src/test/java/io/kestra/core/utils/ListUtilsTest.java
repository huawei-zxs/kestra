package io.kestra.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class ListUtilsTest {

    @Test
    void emptyOnNull() {
        var list = ListUtils.emptyOnNull(null);
        assertThat(list, notNullValue());
        assertThat(list, empty());

        list = ListUtils.emptyOnNull(List.of("1"));
        assertThat(list, notNullValue());
        assertThat(list.size(), is(1));
    }

    @Test
    void shouldReturnEmptyListOnNull() {
        // the returned list must be non-null and directly usable: streaming or sizing it must not throw
        Assertions.assertDoesNotThrow(() -> ListUtils.emptyOnNull(null).stream().count());
        Assertions.assertEquals(0, ListUtils.emptyOnNull(null).size());
    }

    @Test
    void isEmpty() {
        assertThat(ListUtils.isEmpty(null), is(true));
        assertThat(ListUtils.isEmpty(Collections.emptyList()), is(true));
        assertThat(ListUtils.isEmpty(List.of("1")), is(false));
    }
}