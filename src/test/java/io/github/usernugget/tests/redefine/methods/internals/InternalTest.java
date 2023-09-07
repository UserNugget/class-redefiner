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

package io.github.usernugget.tests.redefine.methods.internals;

import io.github.usernugget.redefiner.Mapping;
import io.github.usernugget.redefiner.handlers.Op;
import io.github.usernugget.redefiner.handlers.types.annotations.Head;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InternalTest extends AbstractRedefineTest {
  @Mapping(targetClass = String.class)
  public static class StringMapping {
    byte[] value;

    @Head(method = "getBytes(Ljava/nio/charset/Charset;)[B")
    void getBytes(Charset charset) {
      if (charset == null) {
        Op.returnOp(this.value);
      }
    }
  }

  @Test
  void testNullClassLoader() throws Throwable {
    assertThrows(NullPointerException.class, () -> "".getBytes((Charset) null));

    REDEFINER.transformClass(StringMapping.class);

    assertNotNull("".getBytes((Charset) null));
  }
}
