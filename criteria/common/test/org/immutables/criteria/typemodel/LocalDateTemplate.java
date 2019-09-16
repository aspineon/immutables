/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.typemodel;

import org.immutables.check.IterableChecker;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.personmodel.CriteriaChecker;
import org.immutables.criteria.repository.sync.SyncReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Testing predicates, projections, sorting etc. on booleans
 */
public class LocalDateTemplate {

  private final LocalDateHolderRepository repository;
  private final LocalDateHolderCriteria holder;
  private final Supplier<ImmutableLocalDateHolder> generator;

  protected LocalDateTemplate(Backend backend) {
    this.repository = new LocalDateHolderRepository(backend);
    this.holder = LocalDateHolderCriteria.localDateHolder;
    this.generator = TypeHolder.LocalDateHolder.generator();
  }

  @Test
  void empty() {
    ids(repository.find(holder.value.is(LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.isNot(LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.in(LocalDate.now(), LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.notIn(LocalDate.now(), LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.atLeast(LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.atMost(LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.between(LocalDate.now(), LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.greaterThan(LocalDate.now()))).isEmpty();
    ids(repository.find(holder.value.lessThan(LocalDate.now()))).isEmpty();
  }

  @Test
  void equality() {
    final LocalDate date1 = LocalDate.now();
    repository.insert(generator.get().withId("id1").withValue(date1));

    ids(repository.find(holder.value.is(date1))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.is(date1.plusDays(1)))).isEmpty();
    ids(repository.find(holder.value.isNot(date1))).isEmpty();
    ids(repository.find(holder.value.isNot(date1.plusDays(1)))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.in(date1, date1))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.notIn(date1, date1))).isEmpty();
    ids(repository.find(holder.value.notIn(date1.plusMonths(1), date1.plusDays(1)))).hasContentInAnyOrder("id1");

    final LocalDate date2 = LocalDate.now().plusWeeks(2);
    repository.insert(generator.get().withId("id2").withValue(date1).withValue(date2));
    ids(repository.find(holder.value.is(date2))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.value.isNot(date2))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.isNot(date1))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.value.in(date1, date2))).hasContentInAnyOrder("id1", "id2");
    ids(repository.find(holder.value.in(date1.plusDays(1), date2.plusDays(1)))).isEmpty();
    ids(repository.find(holder.value.notIn(date1, date2))).isEmpty();
    ids(repository.find(holder.value.notIn(date1.plusDays(1), date2))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.notIn(date1, date2.plusDays(1)))).hasContentInAnyOrder("id2");
  }

  @Test
  protected void comparison() {
    final LocalDate date1 = LocalDate.of(2010, 5, 1);
    final LocalDate date2 = LocalDate.of(2010, 10, 2);
    // invariant date1 < date2
    Assertions.assertTrue(date1.compareTo(date2) < 0, String.format("Invariant: %s < %s", date1, date2));

    repository.insert(generator.get().withId("id1").withValue(date1));
    repository.insert(generator.get().withId("id2").withValue(date2));

    ids(repository.find(holder.value.greaterThan(date2))).isEmpty();
    ids(repository.find(holder.value.greaterThan(date1))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.value.lessThan(date1))).isEmpty();
    ids(repository.find(holder.value.lessThan(date2))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.between(date1, date2))).hasContentInAnyOrder("id1", "id2");
    ids(repository.find(holder.value.between(date2.plusDays(1), date2.plusDays(2)))).isEmpty();
    ids(repository.find(holder.value.between(date2, date1))).isEmpty();
    ids(repository.find(holder.value.atMost(date1))).hasContentInAnyOrder("id1");
    ids(repository.find(holder.value.atMost(date1.minusDays(1)))).isEmpty();
    ids(repository.find(holder.value.atMost(date2))).hasContentInAnyOrder("id1", "id2");
    ids(repository.find(holder.value.atLeast(date1))).hasContentInAnyOrder("id1", "id2");
    ids(repository.find(holder.value.atLeast(date2))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.value.atLeast(date2.plusDays(1)))).isEmpty();
  }

  @Test
  void nullable() {
    LocalDate date = LocalDate.now();
    repository.insert(generator.get().withId("id1").withNullable(null));
    repository.insert(generator.get().withId("id2").withNullable(date));

    ids(repository.find(holder.nullable.isPresent())).hasContentInAnyOrder("id2");
    ids(repository.find(holder.nullable.isAbsent())).hasContentInAnyOrder("id1");
    ids(repository.find(holder.nullable.is(date))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.nullable.atLeast(date))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.nullable.atMost(date))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.nullable.greaterThan(date))).isEmpty();
    ids(repository.find(holder.nullable.lessThan(date))).isEmpty();
  }

  @Test
  protected void optional() {
    LocalDate date = LocalDate.now();
    repository.insert(generator.get().withId("id1").withOptional(Optional.empty()));
    repository.insert(generator.get().withId("id2").withOptional(Optional.of(date)));
    ids(repository.find(holder.optional.isPresent())).hasContentInAnyOrder("id2");
    ids(repository.find(holder.optional.isAbsent())).hasContentInAnyOrder("id1");
    ids(repository.find(holder.optional.is(date))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.optional.atLeast(date))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.optional.atMost(date))).hasContentInAnyOrder("id2");
    ids(repository.find(holder.optional.greaterThan(date))).isEmpty();
    ids(repository.find(holder.optional.lessThan(date))).isEmpty();
  }

  private static IterableChecker<List<String>, String> ids(SyncReader<TypeHolder.LocalDateHolder> reader) {
    return CriteriaChecker.<TypeHolder.LocalDateHolder>of(reader).toList(TypeHolder.LocalDateHolder::id);
  }

}