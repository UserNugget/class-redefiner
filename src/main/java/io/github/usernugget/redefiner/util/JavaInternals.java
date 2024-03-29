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

package io.github.usernugget.redefiner.util;

import io.github.usernugget.redefiner.util.asm.ClassFile;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class JavaInternals {
  public static final double JAVA_VERSION;
  public static final int CLASS_VERSION;
  public static final MethodHandles.Lookup TRUSTED;
  public static final Unsafe UNSAFE;

  private static final jdk.internal.misc.Unsafe INTERNAL_UNSAFE;

  static {
    try {
      JAVA_VERSION = Double.parseDouble(System.getProperty("java.vm.specification.version"));
      CLASS_VERSION = (int) Double.parseDouble(System.getProperty("java.class.version"));

      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);

      Field trustedLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      TRUSTED = (MethodHandles.Lookup) UNSAFE.getObject(
         UNSAFE.staticFieldBase(trustedLookup),
         UNSAFE.staticFieldOffset(trustedLookup)
      );

      Class<?> unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
      Jigsaw.implAddExports(unsafeClass, JavaInternals.class.getModule());

      INTERNAL_UNSAFE = (jdk.internal.misc.Unsafe) TRUSTED.findStatic(
        unsafeClass, "getUnsafe", MethodType.methodType(unsafeClass)
      ).invoke();
    } catch (Throwable throwable) {
      throw new ExceptionInInitializerError(throwable);
    }
  }

  public static String getJvmInfo() {
    return "version: " + System.getProperty("java.vm.version") + ", " +
           "vendor: " + System.getProperty("java.vm.vendor") + ", " +
           "os: " + System.getProperty("os.name");
  }

  public static Class<?> defineClass(
     ClassFile classFile, ClassLoader classLoader, byte[] classBytes
  ) {
    try {
      return INTERNAL_UNSAFE.defineClass(
         classFile.name.replace('/', '.'),
         classBytes, 0, classBytes.length,
         classLoader, null
      );
    } catch (Throwable e) {
      throw new IllegalStateException("failed to define class " + classFile.name + ":\n" + classFile.toReadableBytecode(), e);
    }
  }
}
