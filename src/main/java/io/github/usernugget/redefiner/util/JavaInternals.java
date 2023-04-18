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

package io.github.usernugget.redefiner.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;
import sun.misc.Unsafe;

public final class JavaInternals {
  public static final int CLASS_MAJOR_VERSION, CLASS_MINOR_VERSION;

  public static final MethodHandles.Lookup TRUSTED_LOOKUP;
  public static final Unsafe UNSAFE;

  private static final Object UNSAFE_OBJECT;
  private static final MethodHandle DEFINE_CLASS;

  private static final MethodHandle GET_MODULE;
  private static final MethodHandle OPEN_PACKAGE;
  private static final MethodHandle READ_PACKAGE;

  static {
    MethodHandle getModule = null;
    MethodHandle openPackage = null;
    MethodHandle readPackage = null;

    try {
      String classVersion = System.getProperty("java.class.version");

      int index = classVersion.indexOf('.');
      CLASS_MAJOR_VERSION = Integer.parseInt(classVersion.substring(0, index));
      CLASS_MINOR_VERSION = Integer.parseInt(classVersion.substring(index + 1));

      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);

      Field trustedLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      TRUSTED_LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(
         UNSAFE.staticFieldBase(trustedLookupField),
         UNSAFE.staticFieldOffset(trustedLookupField)
      );

      if(CLASS_MAJOR_VERSION >= 53) {
        Class<?> moduleClass = Class.forName("java.lang.Module");
        getModule = TRUSTED_LOOKUP.findVirtual(
           Class.class, "getModule",
           MethodType.methodType(moduleClass)
        );
        openPackage = TRUSTED_LOOKUP.findVirtual(
           moduleClass, "implAddOpens",
           MethodType.methodType(void.class, String.class)
        );
        readPackage = TRUSTED_LOOKUP.findVirtual(
           moduleClass, "implAddReadsAllUnnamed",
           MethodType.methodType(void.class)
        );

        Class<?> internalUnsafe = Class.forName("jdk.internal.misc.Unsafe");
        UNSAFE_OBJECT = TRUSTED_LOOKUP.findStatic(
           internalUnsafe,
           "getUnsafe",
           MethodType.methodType(internalUnsafe)
        ).invoke();
      } else {
        UNSAFE_OBJECT = UNSAFE;
      }

      GET_MODULE = getModule;
      OPEN_PACKAGE = openPackage;
      READ_PACKAGE = readPackage;

      DEFINE_CLASS = TRUSTED_LOOKUP.findVirtual(
         UNSAFE_OBJECT.getClass(),
         "defineClass",
         MethodType.methodType(Class.class, String.class, byte[].class,
            int.class, int.class, ClassLoader.class, ProtectionDomain.class)
      );
    } catch (Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
  }

  public static void exposeModule(Class<?> targetClass, Class<?> classToAddToReads) {
    if(OPEN_PACKAGE == null || targetClass.isPrimitive()) {
      return;
    }

    if(targetClass.isArray()) {
      exposeModule(targetClass.componentType(), classToAddToReads);
      return;
    }

    try {
      Object module = GET_MODULE.invoke(targetClass);
      OPEN_PACKAGE.invoke(module, targetClass.getPackage().getName());
      if (classToAddToReads != null) {
        READ_PACKAGE.invoke(module);
      }
    } catch (Throwable throwable) {
      throw new Error(throwable);
    }
  }

  public static Class<?> defineClass(String className, byte[] code, int codeOffset, int codeLength,
     ClassLoader classLoader, ProtectionDomain protectionDomain) {
    try {
      return (Class<?>) DEFINE_CLASS.invoke(UNSAFE_OBJECT, className, code, codeOffset, codeLength, classLoader, protectionDomain);
    } catch (Throwable e) {
      throw new IllegalStateException("failed to define class " + className, e);
    }
  }
}
