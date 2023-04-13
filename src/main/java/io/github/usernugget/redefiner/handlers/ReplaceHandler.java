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

import io.github.usernugget.redefiner.registry.AnnotationHandler;
import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.info.MethodInfo;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import org.objectweb.asm.Type;

public class ReplaceHandler implements AnnotationHandler {
  @Override
  public void handleMethod(
     CodeGenerator codeGenerator, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass,
     Class<?> mappingJavaClass, ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    ClassMethod method = MethodInfo.parse(mappingMethod, args)
       .findMethodOrThrow(targetClass);

    Type targetReturnType = Type.getReturnType(method.desc);
    Type mappingReturnType = Type.getReturnType(mappingMethod.desc);
    if(targetReturnType.getSort() != mappingReturnType.getSort()) {
      throw new IllegalStateException(
         "mapping method " + mappingMethod.name + " should return " +
         targetReturnType.getClassName() + " or something, that 'extends' or 'implements' it"
      );
    }

    method.insts(mappingMethod.insts());
  }
}
