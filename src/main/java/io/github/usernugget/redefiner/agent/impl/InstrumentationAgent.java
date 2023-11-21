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

package io.github.usernugget.redefiner.agent.impl;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.agent.AbstractAgent;
import io.github.usernugget.redefiner.throwables.InitializationException;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.Ops;
import io.github.usernugget.redefiner.util.asm.io.ClassSerializer;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import static java.util.Objects.requireNonNull;

public class InstrumentationAgent extends AbstractAgent implements ClassFileTransformer {
  private final Instrumentation instrumentation;

  private static final class Transformation {
    private final ClassLoader classLoader;
    private final String className;
    private final BiFunction<byte[], ClassLoader, byte[]> handler;
    private Throwable throwable;

    public Transformation(
      ClassLoader classLoader, String className,
      BiFunction<byte[], ClassLoader, byte[]> handler
    ) {
      requireNonNull(className, "className == null");
      requireNonNull(handler, "handler == null");
      this.classLoader = classLoader;
      this.className = className;
      this.handler = handler;
    }
  }

  private final Queue<Transformation> transformations = new LinkedList<>();
  private final int capatibilities;

  public InstrumentationAgent(Instrumentation instrumentation, ClassRedefiner redefiner) throws InitializationException {
    if (instrumentation == null) {
      throw new InitializationException("instrumentation is null");
    }

    this.instrumentation = instrumentation;
    this.instrumentation.addTransformer(this, true);

    this.capatibilities = lookupCapatibilities(redefiner);
  }

  private interface TmpInterface { }
  private static final class TmpSuperclass { }
  private static final class Tmp implements Supplier<Object> {
    private static int toModify = 1;
    private static Object toRemove;

    private static int toModify(int a) { return 1; }
    private static void toRemove() { }

    @Override public Object get() { return null; }
  }

  // TODO: think about method comparsion under obfuscation
  protected int lookupCapatibilities(ClassRedefiner redefiner) throws InitializationException {
    if (!this.instrumentation.isModifiableClass(Tmp.class)) {
      throw new InitializationException("cannot modify local class");
    }

    int capatibilities = 0;

    ClassSerializer serializer = redefiner.getClassSerializer();

    if (checkCompatibility(serializer, classFile -> {
      ClassMethod method = classFile.findMethod("toModify", "(I)I");
      method.instructions.set(
         method.instructions.getFirst(),
         Ops.op(Opcodes.ICONST_0)
      );
    })) {
      capatibilities |= CHANGE_CODE;
    }

    if (checkCompatibility(serializer, classFile -> {
      ClassMethod method = classFile.visitMethod(Opcodes.ACC_PUBLIC, "addedMethod", "()V");
      method.instructions.add(new InsnNode(Opcodes.RETURN));
    })) {
      capatibilities |= ADD_METHOD;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.methods.remove(classFile.findMethod("toRemove", "()V"));
    })) {
      capatibilities |= REMOVE_METHOD;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.findMethod("toModify", "(I)I").name = "modifiedMethod";
    })) {
      capatibilities |= CHANGE_METHOD;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.visitField(Opcodes.ACC_PUBLIC, "addedField", "Ljava/lang/Object;");
    })) {
      capatibilities |= ADD_FIELD;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.fields.remove(classFile.findField("toRemove", "Ljava/lang/Object;"));
    })) {
      capatibilities |= REMOVE_FIELD;
    }

    if (checkCompatibility(serializer, classFile -> {
      ClassField field = classFile.findField("toModify", "I");
      field.name = "modifiedField";
    })) {
      capatibilities |= CHANGE_FIELD;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.addInterface(Type.getInternalName(TmpInterface.class));
    })) {
      capatibilities |= ADD_INTERACE;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.removeInterface(Type.getInternalName(Consumer.class));
    })) {
      capatibilities |= REMOVE_INTERACE;
    }

    if (checkCompatibility(serializer, classFile -> {
      classFile.superName = Type.getInternalName(TmpSuperclass.class);
    })) {
      capatibilities |= CHANGE_SUPERCLASS;
    }

    return capatibilities;
  }

  private boolean checkCompatibility(ClassSerializer serializer, Consumer<ClassFile> consumer) {
    try {
      retransform(Tmp.class, (rawBytecode, classLoader) -> {
        ClassFile classFile = serializer.readClass(rawBytecode, ClassReader.SKIP_DEBUG);
        consumer.accept(classFile);
        return serializer.writeClass(
          classFile, classLoader, ClassWriter.COMPUTE_FRAMES
        );
      });

      return true;
    } catch (Throwable throwable) {
      return false;
    }
  }

  /*
   * You cannot modify primitive, array, hidden, continuation classes
   * https://github.com/openjdk/jdk/blob/6edd786bf6d8b1008a292b26fc0f901cbae1d03b/src/hotspot/share/prims/jvmtiRedefineClasses.cpp#L331-L351
   */
  @Override
  public boolean isAccessible(Class<?> klass) {
    return this.instrumentation.isModifiableClass(klass);
  }

  public byte[] dumpClass(Class<?> klass) {
    try {
      byte[][] classData = new byte[1][];
      retransform(klass, (classBytes, classLoader) -> {
        classData[0] = classBytes;
        return null;
      });

      return classData[0];
    } catch (Throwable throwable) {
      throw new IllegalStateException("failed to dump class", throwable);
    }
  }

  @Override
  public void rewriteClass(
    Class<?> klass, BiFunction<byte[], ClassLoader, byte[]> modifier
  ) {
    byte[] data = modifier.apply(this.dumpClass(klass), klass.getClassLoader());
    try {
      synchronized (this) {
        this.instrumentation.redefineClasses(new ClassDefinition(klass, data));
      }
    } catch (UnmodifiableClassException e) {
      throw throwable("tried to modify an unmodifiable class", null, e);
    } catch (VerifyError error) {
      if (error.getMessage() == null) {
        throw throwable("verification failed with unknown reason", null, error);
      } else {
        throw throwable("verification failed", null, error);
      }
    } catch (Throwable throwable) {
      throw throwable("failed to rewrite class", null, throwable);
    }
  }

  public synchronized void retransform(
    Class<?> klass, BiFunction<byte[], ClassLoader, byte[]> modifier
  ) {
    Transformation transform = new Transformation(
      klass.getClassLoader(),
      Type.getInternalName(klass),
      modifier
    );

    try {
      synchronized (this) {
        this.transformations.add(transform);
        this.instrumentation.retransformClasses(klass);
      }
    } catch (UnmodifiableClassException e) {
      throw throwable("tries to modify unmodifiable class", transform, e);
    } catch (VerifyError error) {
      if (error.getMessage() == null) {
        throw throwable("verification failed with unknown reason", transform, error);
      }

      throw throwable("verification failed", transform, error);
    } catch (Throwable throwable) {
      throw throwable("failed to rewrite class", transform, throwable);
    }

    if (transform.throwable != null) {
      throw new IllegalStateException(transform.throwable);
    }
  }

  protected IllegalStateException throwable(
    String message,
    Transformation transformation,
    Throwable throwable
  ) {
    IllegalStateException stateException = new IllegalStateException(message, throwable);
    if (transformation != null && transformation.throwable != null) {
      stateException.addSuppressed(transformation.throwable);
    }

    return stateException;
  }

  @Override
  public int getCapabilities() {
    return this.capatibilities;
  }

  @Override
  public byte[] transform(
     ClassLoader loader, String className, Class<?> classBeingRedefined,
     ProtectionDomain protectionDomain, byte[] classfileBuffer
  ) {
    if (this.transformations.isEmpty()) {
      return null;
    }

    Transformation transformation = null;

    try {
      synchronized (this.transformations) {
        for (Transformation t : this.transformations) {
          if (t.className.equals(className) &&
              t.classLoader == loader) {
            transformation = t;
            this.transformations.remove(t);
            break;
          }
        }
      }

      if (transformation != null) {
        return transformation.handler.apply(classfileBuffer, loader);
      }

      return null;
    } catch (Throwable throwable) {
      // Instrumentation (aka JPLIS (aka JVMTI wrapper)) will clear this exception,
      // so use it later
      if (transformation != null) {
        transformation.throwable = throwable;
      }
      return null;
    }
  }

  @Override
  public void close() {
    this.instrumentation.removeTransformer(this);
  }
}
