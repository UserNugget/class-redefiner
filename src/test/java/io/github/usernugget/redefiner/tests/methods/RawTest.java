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
import io.github.usernugget.redefiner.annotation.Raw;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IRETURN;

public class RawTest extends AbstractTest {
  @Test
  void testRaw() throws RedefineFailedException {
    assertTrue(Tmp.invoke());
    REDEFINER.applyMapping(TmpMapping.class);
    assertFalse(Tmp.invoke());
  }

  @Mapping(Tmp.class)
  public static final class TmpMapping {
    @Raw
    public static void invoke(CodeGenerator code, Insts insts) {
      insts.clear();
      insts.op(ICONST_0);
      insts.op(IRETURN);
    }
  }

  public static final class Tmp {
    public static boolean invoke() {
      return true;
    }
  }
}
