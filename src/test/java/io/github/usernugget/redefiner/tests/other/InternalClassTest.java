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

package io.github.usernugget.redefiner.tests.other;

import io.github.usernugget.redefiner.annotation.Head;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.op.Op;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InternalClassTest extends AbstractTest {
  @Test
  void testInternal() throws RedefineFailedException {
    assertThrows(
       NullPointerException.class,
       () -> "Hello world!".getBytes((Charset) null)
    );

    REDEFINER.applyMapping(StringMapping.class);

    assertArrayEquals(new byte[0], "Hello world!".getBytes((Charset) null));
  }

  @Mapping(String.class) // String has null ClassLoader
  public static final class StringMapping {
    @Head(method = "getBytes(Ljava/nio/charset/Charset;)[B")
    public void getBytes(Charset charset) {
      if(charset == null) {
        Op.returnValue(new byte[0]);
      }
    }
  }
}
