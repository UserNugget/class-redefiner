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

package io.github.usernugget.redefiner;

import io.github.usernugget.redefiner.annotation.Mapping;
import io.github.usernugget.redefiner.instrumentation.JavaAgent;
import io.github.usernugget.redefiner.op.Op;
import io.github.usernugget.redefiner.registry.AnnotationHandler;
import io.github.usernugget.redefiner.registry.AnnotationRegistry;
import io.github.usernugget.redefiner.throwable.RedefineFailedException;
import io.github.usernugget.redefiner.transformer.ClassTransformWrapper;
import io.github.usernugget.redefiner.transformer.ClassTransformer;
import io.github.usernugget.redefiner.util.asm.AnnotationValues;
import io.github.usernugget.redefiner.util.asm.Asm;
import io.github.usernugget.redefiner.util.asm.ClassIO;
import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.info.MethodInfo;
import io.github.usernugget.redefiner.util.asm.mapper.ClassMapping;
import io.github.usernugget.redefiner.util.asm.mapper.MappingRemapper;
import io.github.usernugget.redefiner.util.asm.node.ClassField;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.asm.node.Insts;
import io.github.usernugget.redefiner.util.asm.node.immutable.ImmutableInsnNode;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
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

    sortFields(mappingType.fields());
    sortMethods(mappingType.methods());

    for (ClassField field : mappingType.fields()) {
      List<AnnotationNode> visibleAnnotations = field.visibleAnnotations;
      if (visibleAnnotations != null) {
        for (int i = 0; i < visibleAnnotations.size(); i++) {
          AnnotationNode annotationNode = visibleAnnotations.get(i);
          AnnotationHandler handler = this.registry.getHandler(annotationNode.desc);
          if (handler == null) {
            continue;
          }

          visibleAnnotations.remove(i--);

          ClassTransformWrapper wrapper = ClassTransformer.I.enqueue(
             targetType,
             newFieldTransformer(
                handler, field, annotationNode,
                targetClass, mapping, mappingType
             )
          );

          wrapper.setVerify(mappingAnnotation.verifyCode());
          wrapper.setClassLoader(classLoader);
          wrappers.add(wrapper);
        }
      }
    }

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
                handler, annotationNode, classLoader,
                targetClass, mapping, mappingType, method
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
    } catch (VerifyError error) {
      if(error.getMessage() == null) {
        throw new RedefineFailedException(
           "failed to apply " + mapping.getName() + " with an unknown verification exception, " +
           "printing bytecode: \n" + wrappers.get(0).getTargetClass().toBytecodeString(), error);
      } else {
        handleExceptions(mapping, wrappers);
        throw new RedefineFailedException("failed to apply " + mapping.getName(), error);
      }
    } catch (Throwable throwable) {
      handleExceptions(mapping, wrappers);
      throw new RedefineFailedException("failed to apply " + mapping.getName(), throwable);
    }
  }

  protected void sortFields(List<ClassField> fields) {
    fields.sort((c1, c2) -> Integer.compare(
       sortOrder(c2), sortOrder(c1)
    ));
  }

  protected void sortMethods(List<ClassMethod> methods) {
    methods.sort((c1, c2) -> Integer.compare(
       sortOrder(c2), sortOrder(c1)
    ));
  }

  protected int sortOrder(ClassField field) {
    int priority = findAnnotationPriority(field.visibleAnnotations);
    if(priority != Integer.MAX_VALUE) {
      return priority;
    }

    return -1;
  }

  protected int sortOrder(ClassMethod method) {
    boolean foundParameter = false;
    int parameterPriority = Short.MAX_VALUE;

    List<AnnotationNode>[] visibleParameterAnnotations = method.visibleParameterAnnotations;
    if (visibleParameterAnnotations != null) {
      Type[] mappingArgs = Type.getArgumentTypes(method.desc);
      for (int arg = 0; arg < mappingArgs.length; arg++) {
        List<AnnotationNode> paramArgs = visibleParameterAnnotations[arg];
        if (paramArgs != null) {
          for (AnnotationNode paramArg : paramArgs) {
            AnnotationHandler paramHandler = this.registry.getHandler(paramArg.desc);
            if (paramHandler != null) {
              short priority = paramHandler.priority();
              if(priority < 0) {
                throw new IllegalStateException(paramHandler + " priority should be between 0-" + Short.MAX_VALUE);
              }

              foundParameter = true;
              parameterPriority = Math.min(parameterPriority, priority);
            }
          }
        }
      }
    }

    int value = 0;

    int annotationPriority = findAnnotationPriority(method.visibleAnnotations);
    if (annotationPriority != Integer.MAX_VALUE) {
      value += annotationPriority;
    }

    if (foundParameter) {
      value += Short.MAX_VALUE + parameterPriority;
    }

    return value;
  }

  protected int findAnnotationPriority(List<AnnotationNode> visibleAnnotations) {
    int annotationPriority = Integer.MAX_VALUE;
    if (visibleAnnotations != null) {
      for (AnnotationNode annotationNode : visibleAnnotations) {
        AnnotationHandler handler = this.registry.getHandler(annotationNode.desc);
        if (handler != null) {
          short priority = handler.priority();
          if(priority < 0) {
            throw new IllegalStateException(handler + " priority should be between 0-" + Short.MAX_VALUE);
          }

          annotationPriority = Math.min(annotationPriority, handler.priority());
        }
      }
    }

    return annotationPriority;
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

  protected BiConsumer<ClassFile, CodeGenerator> newFieldTransformer(
     AnnotationHandler handler, ClassField targetField, AnnotationNode annotationNode,
     Class<?> targetJavaClass, Class<?> mappingJavaClass, ClassFile mappingAsmClass
  ) {
    return (targetAsmClass, codeGenerator) -> handler.handleField(
       codeGenerator, new AnnotationValues(annotationNode), targetJavaClass,
       targetAsmClass, mappingJavaClass, mappingAsmClass, targetField
    );
  }

  protected BiConsumer<ClassFile, CodeGenerator> newCodeTransformer(
     AnnotationHandler handler, AnnotationNode annotationNode, ClassLoader classLoader,
     Class<?> targetJavaClass, Class<?> mappingJavaClass, ClassFile mappingAsmClass, ClassMethod targetMethod
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

      Asm.fixClassLoaders(targetJavaClass, classLoader, targetJavaClass.getClassLoader(), targetMethod);

      handler.handleMethod(
         codeGenerator, new AnnotationValues(annotationNode), targetJavaClass,
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
