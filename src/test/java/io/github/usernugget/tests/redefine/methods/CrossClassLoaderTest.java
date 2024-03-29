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

package io.github.usernugget.tests.redefine.methods;

import io.github.usernugget.redefiner.Mapping;
import io.github.usernugget.redefiner.handlers.Op;
import io.github.usernugget.redefiner.handlers.types.annotations.Head;
import io.github.usernugget.tests.redefine.AbstractRedefineTest;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CrossClassLoaderTest extends AbstractRedefineTest {
  // Class for ClassLoader A
  public static final class A {
    public static Boolean alwaysTrue(Object unused) {
      return true;
    }

    // madness
    public static int value(
      Class<?> ldc,
      Function<Object, Boolean> invokedynamic,
      Object checkcast,
      Object[] anewarray,
      Object[][][][] multianewarray
    ) {
      if (!ldc.getName().contains("CrossClassLoaderTest$B")) {
        throw new IllegalStateException("ldc failed");
      }

      if (!checkcast.getClass().getName().contains("CrossClassLoaderTest$B")) {
        throw new IllegalStateException("checkcast failed");
      }

      if (anewarray.length != 6) {
        throw new IllegalStateException("anewarray failed");
      }

      if (multianewarray.length != 6) {
        throw new IllegalStateException("multianewarray failed");
      }

      if (!invokedynamic.apply(false)) {
        throw new IllegalStateException("invokedynamic failed");
      }

      return 1024;
    }

    public static int testStatic(boolean value) {
      return 0;
    }
  }

  // Class for ClassLoader B
  public static final class B {
    public static Boolean alwaysTrue(Object unused) {
      return true;
    }

    // FIXME: trick <init>
    public static Object create() {
      return new B();
    }
  }

  @Mapping(targetClassName = "io.github.usernugget.tests.redefine.methods.CrossClassLoaderTest$A")
  public static final class BMapping {
    @Head
    public static void testStatic(boolean value) {
      if (value) {
        // Test for method invocation and .class values
        Op.returnOp(A.value(
          B.class, B::alwaysTrue,
          (B) B.create(), new B[6],
          new B[6][6][6][6]
        ));
      }
    }
  }

  @Test
  void testInjection() throws Throwable {
    try (
      FakeClassLoader a = new FakeClassLoader(
        A.class.getName(),
        getClass().getClassLoader().getResource(A.class.getName().replace('.', '/') + ".class"),
        ClassLoader.getPlatformClassLoader()
      );
      FakeClassLoader b = new FakeClassLoader(
        B.class.getName(),
        getClass().getClassLoader().getResource(B.class.getName().replace('.', '/') + ".class"),
        a
      )
    ) {
      REDEFINER.transformClass(b, BMapping.class);

      Class<?> A = a.loadClass(A.class.getName());

      Method method = A.getDeclaredMethod("testStatic", boolean.class);

      assertEquals(1024, method.invoke(null, true)); // Redefined
      assertEquals(0, method.invoke(null, false)); // Original
    }
  }

  public static final Charset CHARSET = new Charset("hi", null) {
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

  public static byte[] getBytes() {
    return null;
  }

  @Mapping(targetClass = String.class)
  public static final class InternalMapping {
    @Head(method = "getBytes(Ljava/nio/charset/Charset;)[B")
    void getBytes(Charset charset) {
      if (charset == CHARSET) {
        Op.returnOp(CrossClassLoaderTest.getBytes());
      }
    }
  }

  @Test
  void testInternal() throws ClassNotFoundException {
    REDEFINER.transformClass(InternalMapping.class);
    assertNull(" ".getBytes(CHARSET));
  }

  @Mapping(targetClass = Array.class)
  public static final class ArrayMapping {
    @Head(method = "newInstance(Ljava/lang/Class;[I)Ljava/lang/Object;")
    static void newInstance(Class<?> componentType, int... length) {
      if (componentType == null) {
        Op.returnOp(getBytes());
      }
    }
  }

  @Test
  void testDifferent() throws ClassNotFoundException {
    assertThrows(NullPointerException.class, () -> Array.newInstance(null, 0, 0));

    REDEFINER.transformClass(ArrayMapping.class);

    assertEquals(Array.newInstance(null, 0, 0), getBytes());
  }

  private static final class FakeClassLoader extends URLClassLoader {
    public FakeClassLoader(String name, URL klass, ClassLoader parent) throws Throwable {
      super(makeJar(name, klass), parent);
    }

    private static URL[] makeJar(String name, URL klass) throws Throwable {
      Path path = Files.createTempFile("class-redefiner-test", ".jar");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          Files.deleteIfExists(path);
        } catch (IOException e) {
          throw new IllegalStateException("failed to remove temp file", e);
        }
      }));

      try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(path))) {
        jos.putNextEntry(new JarEntry(name.replace('.', '/') + ".class"));
        klass.openConnection().getInputStream().transferTo(jos);
      }

      return new URL[] { path.toUri().toURL() };
    }
  }
}
