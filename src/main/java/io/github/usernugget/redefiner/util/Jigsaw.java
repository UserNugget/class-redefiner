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
import java.lang.invoke.MethodType;

public class Jigsaw {
  private static final MethodHandle MH_implAddReads;
  private static final MethodHandle MH_implAddExports;

  static {
    try {
      Class<?> moduleClass = Class.forName("java.lang.Module");
      MH_implAddReads = JavaInternals.TRUSTED.findVirtual(
        moduleClass,
        "implAddReads",
        MethodType.methodType(void.class, moduleClass)
      );
      MH_implAddExports = JavaInternals.TRUSTED.findVirtual(
        moduleClass,
        "implAddExports",
        MethodType.methodType(void.class, String.class, moduleClass)
      );
    } catch (Throwable throwable) {
      throw new IllegalStateException(
        "failed to get jigsaw methods, jvm: " + JavaInternals.getJvmInfo(),
        throwable
      );
    }
  }

  public static void implAddReads(Class<?> klass, Object module) {
    if (MH_implAddReads != null) {
      try {
        MH_implAddReads.invoke(klass.getModule(), module);
      } catch (Throwable throwable) {
        throw new Error(
          "failed to invoke Module::implAddReads, jvm: " + JavaInternals.getJvmInfo(),
          throwable
        );
      }
    }
  }

  public static void implAddExports(Class<?> klass, Object module) {
    if (MH_implAddExports != null) {
      try {
        MH_implAddExports.invoke(klass.getModule(), klass.getPackageName(), module);
      } catch (Throwable throwable) {
        throw new Error(
          "failed to invoke Module::implAddExports, jvm: " + JavaInternals.getJvmInfo(),
          throwable
        );
      }
    }
  }
}
