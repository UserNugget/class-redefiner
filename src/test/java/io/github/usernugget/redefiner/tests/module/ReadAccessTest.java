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

package io.github.usernugget.redefiner.tests.module;

import io.github.usernugget.redefiner.annotation.Head;
import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.op.Op;
import io.github.usernugget.redefiner.tests.AbstractTest;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReadAccessTest extends AbstractTest {
  @Test
  @DisabledOnJre(JRE.JAVA_8)
  void testModuleReadAccess() throws RedefineFailedException {
    assertThrows(NullPointerException.class, () -> "".getBytes((String) null));
    REDEFINER.applyMapping(ClassMapping.class);
    assertDoesNotThrow(() -> "".getBytes((String) null));
  }

  public static boolean alwaysTrue() {
    return true;
  }

  @Mapping(String.class)
  public static final class ClassMapping {
    @Head(method = "getBytes(Ljava/lang/String;)[B")
    void getBytes(String charset) {
      if(charset == null && alwaysTrue()) {
        Op.returnValue(new byte[0]);
      }
    }
  }
}
