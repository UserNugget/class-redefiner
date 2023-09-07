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

package io.github.usernugget.redefiner.agent;

import java.io.Closeable;
import java.util.function.BiFunction;

public abstract class AbstractAgent implements Closeable {
  public static final int CHANGE_CODE = 1 << 0;

  public static final int ADD_FIELD = 1 << 1;
  public static final int REMOVE_FIELD  = 1 << 2;
  public static final int CHANGE_FIELD  = 1 << 3;

  public static final int ADD_METHOD = 1 << 4;
  public static final int REMOVE_METHOD = 1 << 5;
  public static final int CHANGE_METHOD = 1 << 6;

  public static final int ADD_INTERACE = 1 << 7;
  public static final int REMOVE_INTERACE = 1 << 8;

  public static final int CHANGE_SUPERCLASS = 1 << 9;

  public abstract boolean isAccessible(Class<?> klass);
  public abstract void rewriteClass(Class<?> klass, BiFunction<byte[], ClassLoader, byte[]> modifier);
  public abstract byte[] dumpClass(Class<?> klass);
  public abstract int getCapabilities();

  public boolean can(int option) {
    return (getCapabilities() & option) != 0;
  }

  public void close() { }
}
