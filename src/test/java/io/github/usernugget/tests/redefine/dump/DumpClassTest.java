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

package io.github.usernugget.tests.redefine.dump;

import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DumpClassTest extends AbstractRedefineTest {

  @Test
  void testDumpClass() throws IOException {
    ClassFile classFile = REDEFINER.getClassSerializer().readClass(
       Tmp.class, ClassReader.SKIP_CODE
    );

    assertEquals(2, classFile.methods.size()); // <init>()V, test()V
    assertEquals("test", classFile.methods.get(1).name);
  }

  public static final class Tmp {
    public void test() { }
  }
}
