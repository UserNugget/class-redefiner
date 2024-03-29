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

package io.github.usernugget.redefiner.handlers;

public class Op {
  public static void returnOp() { }

  public static void returnOp(boolean value) { }
  public static void returnOp(byte value) { }
  public static void returnOp(short value) { }
  public static void returnOp(char value) { }
  public static void returnOp(int value) { }
  public static void returnOp(float value) { }
  public static void returnOp(long value) { }
  public static void returnOp(double value) { }
  public static void returnOp(Object value) { }
}
