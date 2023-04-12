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

package kk.tests.impl;

import kk.redefiner.annotation.Head;
import kk.redefiner.annotation.Mapping;
import kk.redefiner.op.Op;
import kk.redefiner.throwable.RedefineFailedException;
import kk.tests.AbstractTest;
import org.junit.jupiter.api.Test;
import java.nio.charset.Charset;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestInternalClasses extends AbstractTest {
  @Test
  void redefineString() throws RedefineFailedException {
    assertThrows(
       NullPointerException.class,
       () -> "Hello world!".getBytes((Charset) null)
    );

    REDEFINER.applyMapping(StringMapping.class);

    assertDoesNotThrow(
       () -> "Hello world!".getBytes((Charset) null)
    );
  }

  @Mapping(String.class) // String has null class loader
  public static final class StringMapping {
    byte[] value;

    @Head(method = "getBytes(Ljava/nio/charset/Charset;)[B")
    public void getBytes(Charset charset) {
      if(charset == null) {
        Op.returnValue(this.value);
      }
    }
  }
}
