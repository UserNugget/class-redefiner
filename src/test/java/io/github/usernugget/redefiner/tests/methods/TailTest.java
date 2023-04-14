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

package io.github.usernugget.redefiner.tests.methods;

import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.annotation.Tail;
import io.github.usernugget.redefiner.op.Op;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TailTest extends AbstractTest {
  @Test
  void testTail() throws RedefineFailedException {
    assertTrue(Tmp.invoke());
    REDEFINER.applyMapping(TmpMapping.class);
    assertFalse(Tmp.invoke());
  }

  @Mapping(Tmp.class)
  public static final class TmpMapping {
    @Tail
    public static void invoke() {
      Op.returnValue(false);
    }
  }

  public static final class Tmp {
    public static boolean invoke() {
      return true;
    }
  }
}
