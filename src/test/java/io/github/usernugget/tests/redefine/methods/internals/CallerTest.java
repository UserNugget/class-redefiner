/*
 * Copyright (C) 2024 UserNugget/class-redefiner
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
import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIf("shouldIgnore") // OpenJ9 for Java 11 doesnt allow MethodHandle to access private fields
public class CallerTest extends AbstractRedefineTest {

  static boolean shouldIgnore() {
    return JavaInternals.JAVA_VERSION == 11 &&
      System.getProperty("java.vendor").toLowerCase().contains("ibm corporation");
  }

  public static final CallerTest INSTANCE = new CallerTest();
  public static final Charset CHARSET = new Charset("test", new String[0]) {
    @Override
    public boolean contains(Charset charset) {
      return false;
    }

    @Override
    public CharsetDecoder newDecoder() {
      return null;
    }

    @Override
    public CharsetEncoder newEncoder() {
      return null;
    }
  };

  public static boolean VIRTUAL_CALLED;
  public static boolean STATIC_CALLED;

  private static int A = (int) System.currentTimeMillis();
  private static int[] B = { (int) System.currentTimeMillis() };
  private static Object[] C = { System.currentTimeMillis() };
  private static long D = System.currentTimeMillis();

  private int a = (int) System.currentTimeMillis();
  private int[] b = { (int) System.currentTimeMillis() };
  private Object[] c = { System.currentTimeMillis() };
  private long d = System.currentTimeMillis();

  @Mapping(targetClass = String.class)
  public static class StringMapping {
    byte[] value;

    @Head(method = "getBytes(Ljava/nio/charset/Charset;)[B")
    void getBytes(Charset charset) {
      if (charset == CHARSET) {
        Op.returnOp(CallerTest.INSTANCE.handleVirtual(CallerTest.INSTANCE.a,
          CallerTest.INSTANCE.b, CallerTest.INSTANCE.c, CallerTest.INSTANCE.d,
          handleStatic(A, B, C, D, this.value)));
      }
    }
  }

  public byte[] handleVirtual(int a, int[] b, Object[] c, long d, byte[] value) {
    if (a != this.a || b != this.b || c != this.c || d != this.d) {
      throw new IllegalStateException("mismatch!");
    }

    VIRTUAL_CALLED = true;
    return new byte[0];
  }

  public static byte[] handleStatic(int a, int[] b, Object[] c, long d, byte[] value) {
    if (a != A || b != B || c != C || d != D) {
      throw new IllegalStateException("mismatch!");
    }

    STATIC_CALLED = true;
    return new byte[0];
  }

  @Test
  void testFields() throws Throwable {
    assertThrows(NullPointerException.class, () -> "".getBytes(CHARSET));

    REDEFINER.transformClass(StringMapping.class);

    assertNotNull("".getBytes(CHARSET));
    assertTrue(STATIC_CALLED);
    assertTrue(VIRTUAL_CALLED);
  }
}
