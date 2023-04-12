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

package kk.redefiner;

import kk.redefiner.annotation.Mapping;
import kk.redefiner.instrumentation.JavaAgent;
import kk.redefiner.op.Op;
import kk.redefiner.registry.AnnotationHandler;
import kk.redefiner.registry.AnnotationRegistry;
import kk.redefiner.throwable.RedefineFailedException;
import kk.redefiner.transformer.ClassTransformWrapper;
import kk.redefiner.transformer.ClassTransformer;
import kk.redefiner.util.asm.AnnotationValues;
import kk.redefiner.util.asm.ClassIO;
import kk.redefiner.util.asm.CodeGenerator;
import kk.redefiner.util.asm.info.MethodInfo;
import kk.redefiner.util.asm.mapper.ClassMapping;
import kk.redefiner.util.asm.mapper.MappingRemapper;
import kk.redefiner.util.asm.node.ClassFile;
import kk.redefiner.util.asm.node.ClassMethod;
import kk.redefiner.util.asm.node.Insts;
import kk.redefiner.util.asm.node.immutable.ImmutableInsnNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

public class ClassRedefiner {
  protected static final Instrumentation INSTRUMENTATION = JavaAgent.prepareAgent().createInstrmentation();
  protected static final String OP_TYPE = Type.getInternalName(Op.class);

  static {
    INSTRUMENTATION.addTransformer(ClassTransformer.I, true);
  }

  private final AnnotationRegistry registry;

  public ClassRedefiner(AnnotationRegistry registry) {
    Objects.requireNonNull(registry, "registry is null");

    this.registry = registry;
  }

  public void applyMapping(Class<?> mapping) throws RedefineFailedException {
    applyMapping(mapping, mapping.getClassLoader());
  }

  public void applyMapping(Class<?> mapping, ClassLoader classLoader) throws RedefineFailedException {
    Mapping mappingAnnotation = mapping.getAnnotation(Mapping.class);
    Class<?> targetClass = findMappingTarget(mappingAnnotation, mapping, classLoader);
    String targetType = Type.getInternalName(targetClass);

    ClassMapping classMapping = new ClassMapping();
    classMapping.classMapping(mapping, targetType);

    ClassFile mappingType = new ClassFile();
    ClassIO.fromClass(mapping).accept(
       new ClassRemapper(mappingType, new MappingRemapper(classMapping))
    );

    List<ClassTransformWrapper> wrappers = new ArrayList<>();
    for (ClassMethod method : mappingType.methods()) {
      method.insts(replaceOpWithCode(method.insts()));

      List<AnnotationNode> annotations = method.visibleAnnotations;
      if (annotations != null) {
        for (int i = 0; i < annotations.size(); i++) {
          AnnotationNode annotationNode = annotations.get(i);
          AnnotationHandler handler = this.registry.getHandler(annotationNode.desc);
          if (handler == null) {
            continue;
          }

          annotations.remove(i--);

          ClassTransformWrapper wrapper = ClassTransformer.I.enqueue(
             targetType,
             newCodeTransformer(
                handler, method, annotationNode,
                targetClass, mapping, mappingType
             )
          );

          wrapper.setVerify(mappingAnnotation.verifyCode());
          wrappers.add(wrapper);
        }
      }
    }

    try {
      INSTRUMENTATION.retransformClasses(targetClass);
      handleExceptions(mapping, wrappers);
    } catch (RedefineFailedException throwable) {
      throw throwable;
    } catch (Throwable throwable) {
      handleExceptions(mapping, wrappers);
      throw new RedefineFailedException("failed to apply " + mapping.getName(), throwable);
    }
  }

  protected void handleExceptions(Class<?> mapping, List<ClassTransformWrapper> wrappers) throws RedefineFailedException {
    Set<Throwable> throwables = null;
    for (ClassTransformWrapper wrapper : wrappers) {
      if (wrapper.getThrowable() != null) {
        if (throwables == null) {
          throwables = Collections.newSetFromMap(new IdentityHashMap<>());
        }

        throwables.add(wrapper.getThrowable());
      }
    }

    if (throwables != null) {
      RedefineFailedException exception = new RedefineFailedException("failed to apply " + mapping.getName());
      for (Throwable throwable : throwables) {
        exception.addSuppressed(throwable);
      }
      throw exception;
    }
  }

  protected BiConsumer<ClassFile, CodeGenerator> newCodeTransformer(
     AnnotationHandler handler, ClassMethod targetMethod, AnnotationNode annotationNode,
     Class<?> targetJavaClass, Class<?> mappingJavaClass, ClassFile mappingAsmClass
  ) {
    return (targetAsmClass, codeGenerator) -> {
      List<AnnotationNode>[] parameterAnnotations = targetMethod.visibleParameterAnnotations;
      if (parameterAnnotations != null) {
        Type[] mappingArgs = Type.getArgumentTypes(targetMethod.desc);

        MethodInfo methodInfo = MethodInfo.parse(targetMethod, new AnnotationValues(annotationNode));
        ClassMethod ownerMethod = methodInfo.findMethodOrThrow(targetAsmClass);

        for (int arg = 0; arg < mappingArgs.length; arg++) {
          List<AnnotationNode> paramArgs = parameterAnnotations[arg];
          if (paramArgs != null) {
            for (AnnotationNode paramArg : paramArgs) {
              AnnotationHandler paramHandler = this.registry.getHandler(paramArg.desc);
              if (paramHandler != null) {
                paramHandler.handleParameter(
                   targetAsmClass, ownerMethod, mappingAsmClass, targetMethod,
                   new AnnotationValues(paramArg), arg, mappingArgs
                );
              }
            }
          }
        }
      }

      handler.handleMethod(
         codeGenerator, new AnnotationValues(annotationNode),targetJavaClass,
         targetAsmClass, mappingJavaClass, mappingAsmClass, targetMethod
      );
    };
  }

  protected Insts replaceOpWithCode(Insts insts) {
    for (AbstractInsnNode instruction : insts) {
      if (instruction.getOpcode() == Opcodes.INVOKESTATIC) {
        MethodInsnNode methodNode = (MethodInsnNode) instruction;
        if (methodNode.owner.equals(OP_TYPE) && methodNode.name.equals("returnValue")) {
          int opcode = methodNode.desc.startsWith("()") ? RETURN :
                       Type.getArgumentTypes(methodNode.desc)[0].getOpcode(IRETURN);

          insts.set(instruction, new ImmutableInsnNode(opcode));
        }
      }
    }

    return insts;
  }

  protected Class<?> findMappingTarget(Mapping mappingAnnotation, Class<?> mapping, ClassLoader classLoader) {
    if (mappingAnnotation == null) {
      throw new IllegalStateException("class " + mapping.getName() + " should has @Mapping annotation");
    }

    if (Mapping.DEFAULT.equals(mappingAnnotation.raw())) {
      return mappingAnnotation.value();
    } else {
      try {
        return Class.forName(mappingAnnotation.raw(), false, classLoader);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("class " + mappingAnnotation.raw() + " not found", e);
      }
    }
  }

  public AnnotationRegistry getRegistry() {
    return this.registry;
  }
}
