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

import io.github.usernugget.redefiner.util.asm.ClassMethod;
import org.objectweb.asm.Type;

public class Checks {
  public static void checkFieldFormat(ClassMethod mapping, Type fieldDesc) {
    boolean formatViolation = mapping.returnType().getSort() != fieldDesc.getSort();
    if (!formatViolation) {
      Type[] args = Type.getType(mapping.desc).getArgumentTypes();
      formatViolation = args.length != 1 || args[0].getSort() != fieldDesc.getSort();
    }

    if (formatViolation) {
      String type = "<unknown>";
      switch (fieldDesc.getSort()) {
        case Type.BOOLEAN: { type = "boolean"; break; }
        case Type.CHAR:    { type = "char";    break; }
        case Type.BYTE:    { type = "byte";    break; }
        case Type.SHORT:   { type = "short";   break; }
        case Type.INT:     { type = "int";     break; }
        case Type.FLOAT:   { type = "float";   break; }
        case Type.LONG:    { type = "long";    break; }
        case Type.DOUBLE:  { type = "double";  break; }
        case Type.ARRAY:   { type = "array";   break; }
        case Type.OBJECT:  { type = "object";  break; }
      }

      throw new IllegalStateException(
        "mapping method " + mapping + " must be " + type + "(" + type + ")"
      );
    }
  }
}
