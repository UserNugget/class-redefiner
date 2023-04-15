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

package io.github.usernugget.redefiner.util.asm;

import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableInsnNode;
import io.github.usernugget.redefiner.util.cache.ClassCache;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Type.VOID;

public class Asm {
  public static int varOp(Type type, int opcode) {
    return type.getSort() == VOID ? -1 : type.getOpcode(opcode);
  }

  public static int loadToStore(int loadOpcode) {
    return ISTORE + (loadOpcode - ILOAD);
  }

  public static int storeToLoad(int storeOpcode) {
    return ILOAD + (storeOpcode - ISTORE);
  }

  public static boolean isReturn(int opcode) {
    return opcode >= IRETURN && opcode <= RETURN;
  }

  public static boolean isUnmoddedReturn(AbstractInsnNode insnNode) {
    return isReturn(insnNode.getOpcode()) && !(insnNode instanceof ImmutableInsnNode);
  }

  public static void fixClassLoaders(Class<?> mapping, Class<?> target, ClassMethod method) {
    ClassCache classCache = new ClassCache();
    Set<ClassLoader> classLoaders = Collections.newSetFromMap(new IdentityHashMap<>());

    ClassLoader targetClassLoader = target.getClassLoader();
    ClassLoader mappingClassLoader = mapping.getClassLoader();

    CodeGenerator crossGenerator = new CodeGenerator(true);

    Insts mappingInsts = method.insts();
    for(AbstractInsnNode i : mappingInsts) {
      boolean isMethod = i instanceof MethodInsnNode;
      boolean isField = i instanceof FieldInsnNode;

      if(isMethod || isField) {
        try {
          String owner = isMethod ? ((MethodInsnNode) i).owner : ((FieldInsnNode) i).owner;

          Class<?> javaClass = Class.forName(owner.replace("/", "."), false, mappingClassLoader);
          if(isInDifferentClassLoader(classLoaders, targetClassLoader, javaClass.getClassLoader())) {
            classCache.setClassLoader(javaClass.getClassLoader());
            ClassFile ownerClass = classCache.findClass(javaClass);

            if(isMethod) {
              MethodInsnNode methodNode = (MethodInsnNode) i;
              ClassMethod classMethod = ownerClass.findMethod(classCache, methodNode.name, methodNode.desc);
              if (classMethod == null) {
                continue;
              }

              crossGenerator.invoke(mappingInsts, i, crossGenerator.methodInvoker(ownerClass, classMethod));
            } else {
              FieldInsnNode fieldNode = (FieldInsnNode) i;
              ClassField field = ownerClass.findDeclaredField(fieldNode.name, fieldNode.desc);
              if (field == null) {
                continue;
              }

              ClassMethod fieldOp;
              if(i.getOpcode() == GETSTATIC || i.getOpcode() == GETFIELD) {
                fieldOp = crossGenerator.fieldGetter(ownerClass, field);
              } else {
                fieldOp = crossGenerator.fieldSetter(ownerClass, field);
              }

              crossGenerator.invoke(mappingInsts, i, fieldOp);
            }
          }
        } catch (Throwable throwable) {
          throw new IllegalStateException(
             "failed to inject code", throwable
          );
        }
      }
    }

    crossGenerator.define(targetClassLoader, mappingClassLoader);
  }

  public static boolean isInDifferentClassLoader(
     Set<ClassLoader> cache, ClassLoader first, ClassLoader second
  ) {
    cache.clear();

    ClassLoader classLoader = first;
    while(classLoader != null) {
      cache.add(classLoader);
      classLoader = classLoader.getParent();
    }

    return !cache.contains(second);
  }
}
