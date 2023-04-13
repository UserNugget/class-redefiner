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

package io.github.usernugget.redefiner.registry;

import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.op.Op;
import org.objectweb.asm.Type;

public interface AnnotationHandler {
  String OP_OBJECT_TYPE = org.objectweb.asm.Type.getInternalName(Op.class);
  String OP_OBJECT = org.objectweb.asm.Type.getDescriptor(Op.class);

  default void handleMethod(CodeGenerator codeGenerator, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass, Class<?> mappingJavaClass,
     ClassFile mappingClass, ClassMethod mappingMethod) { }

  default void handleParameter(ClassFile targetClass, ClassMethod targetMethod,
     ClassFile mappingClass, ClassMethod mappingMethod,
     AnnotationValues args, int argIndex, Type[] descArgs) { }

  default String desc(ClassFile classFile, ClassMethod classMethod) {
    return classFile.name + " " + classMethod.name + classMethod.desc;
  }

  default String desc(ClassFile classFile, ClassField classField) {
    return classFile.name + " " + classField.desc + " " + classField.name;
  }
}
