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

import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.info.MethodInfo;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import io.github.usernugget.redefiner.registry.AnnotationHandler;
import org.objectweb.asm.Type;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RawHandler implements AnnotationHandler {
  @Override
  public void handleMethod(
     CodeGenerator codeGenerator, AnnotationValues args,
     Class<?> targetJavaClass, ClassFile targetClass,
     Class<?> mappingJavaClass, ClassFile mappingClass, ClassMethod mappingMethod
  ) {
    if (!mappingMethod.isStatic()) {
      throw new IllegalStateException(
         "method " + mappingJavaClass.getName() + "#" + mappingMethod.name + " should be static"
      );
    }

    Type[] methodParameters = Type.getArgumentTypes(mappingMethod.desc);
    if(methodParameters.length != 2 ||
       !methodParameters[0].getClassName().equals(CodeGenerator.class.getName()) ||
       !methodParameters[1].getClassName().equals(Insts.class.getName())) {
      throw new IllegalStateException("mapping method " + mappingMethod.name + " should use (CodeGenerator, Insts) as arguments");
    }

    if(Type.getReturnType(mappingMethod.desc).getSort() != Type.VOID) {
      throw new IllegalStateException("mapping method " + mappingMethod.name + " should be void");
    }

    ClassMethod method = MethodInfo.parse(mappingMethod, args)
       .findMethodOrThrow(targetClass);

    for (Method javaMethod : mappingJavaClass.getMethods()) {
      if (javaMethod.getName().equals(mappingMethod.name) &&
          Type.getMethodDescriptor(javaMethod).equals(mappingMethod.desc)) {
        try {
          javaMethod.setAccessible(true);
          javaMethod.invoke(null, codeGenerator, method.insts());
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IllegalStateException(e);
        }
      }
    }
  }
}
