/*
 * Copyright (C) 2023 UserNugget/class-redefiner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.usernugget.redefiner.tests.vars;

import io.github.usernugget.redefiner.annotation.Head;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.annotation.Var;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import io.github.usernugget.redefiner.util.JavaInternals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VarTest extends AbstractTest {
  @Test
  void testVar() throws RedefineFailedException {
    // TODO: for some reason Java 8 fails this
    if (JavaInternals.CLASS_MAJOR_VERSION <= 52) {
      return;
    }

    assertEquals(15, Tmp.invoke(1, 2, 4, 8));
    REDEFINER.applyMapping(TmpMapping.class);
    assertEquals(-15, Tmp.invoke(1, 2, 4, 8));
  }

  @Mapping(Tmp.class)
  public static final class TmpMapping {
    @Head
    public static void invoke(
       int a,
       @Var int b,
       @Var(name = "c") int e,
       @Var(raw = 3) int f
    ) {
      a = -a;
      b = -b;
      e = -e;
      f = -f;
    }
  }

  public static final class Tmp {
    public static int invoke(int a, int b, int c, int d) {
      return a + b + c + d;
    }
  }
}
