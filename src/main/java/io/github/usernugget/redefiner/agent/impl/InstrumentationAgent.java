/*
 * Copyright (C) 2024 UserNugget/class-redefiner
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
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiFunction;
import org.objectweb.asm.Type;
import static java.util.Objects.requireNonNull;

public class InstrumentationAgent extends AbstractAgent implements ClassFileTransformer {
  private final ClassRedefiner redefiner;
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

  public InstrumentationAgent(Instrumentation instrumentation, ClassRedefiner redefiner) throws InitializationException {
    if (instrumentation == null) {
      throw new InitializationException("instrumentation is null");
    }

    this.redefiner = redefiner;
    this.instrumentation = instrumentation;
    this.instrumentation.addTransformer(this, true);
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
    } catch (ClassFormatError e) {
      String bytecode = this.redefiner.getClassSerializer().readClass(data, 0).toReadableBytecode();
      throw throwable("verification failed due to bad class structure, bytecode:\n" + bytecode, null, e);
    } catch (VerifyError error) {
      String bytecode = this.redefiner.getClassSerializer().readClass(data, 0).toReadableBytecode();

      if (error.getMessage() == null) {
        throw throwable("verification failed with unknown reason, bytecode:\n" + bytecode, null, error);
      } else {
        throw throwable("verification failed, bytecode: \n" + bytecode, null, error);
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
    return CHANGE_CODE;
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
