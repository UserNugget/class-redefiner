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

package io.github.usernugget.redefiner.agent.attach.types;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.agent.AbstractAgent;
import io.github.usernugget.redefiner.agent.attach.AbstractAttach;
import io.github.usernugget.redefiner.agent.impl.InstrumentationAgent;
import io.github.usernugget.redefiner.throwables.InitializationException;
import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.instruction.Insns;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class HotspotAttach extends AbstractAttach {
  public boolean isSupported(ClassRedefiner redefiner) {
    try {
      Class.forName("sun.instrument.InstrumentationImpl");
      return true;
    } catch (ClassNotFoundException ignored) {
      return false;
    }
  }

  @Override
  public AbstractAgent createAgent(ClassRedefiner redefiner) throws InitializationException {
    String className = Type.getInternalName(HotspotAttach.class) + "$Agent$" + Long.toString(System.nanoTime(), 36);
    Class<?> agentClass = createAgentClass(redefiner, className);

    Field instrumentationField;
    try {
      instrumentationField = agentClass.getDeclaredField("I");
    } catch (NoSuchFieldException e) {
      throw new InitializationException("field 'I' not found", e);
    }

    attachAgent(className);

    Object instrumentation = JavaInternals.UNSAFE.getObject(
       JavaInternals.UNSAFE.staticFieldBase(instrumentationField),
       JavaInternals.UNSAFE.staticFieldOffset(instrumentationField)
    );

    if (instrumentation == null) {
      throw new InitializationException("attachAgent is called, but instrumentation is null");
    }

    return new InstrumentationAgent((Instrumentation) instrumentation, redefiner);
  }

  protected void attachAgent(String className) throws InitializationException {
    try {
      Path tmpFile = Files.createTempFile("javaagent-", ".zip");

      try {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();

        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Can-Retransform-Classes", "true");
        attributes.putValue("Can-Redefine-Classes", "true");

        attributes.putValue("Agent-Class", className.replace('/', '.'));

        // Used by InstrumentationImpl::loadAgent
        attributes.putValue("Launcher-Agent-Class", attributes.getValue("Agent-Class"));

        // some magic here
        new JarOutputStream(Files.newOutputStream(tmpFile), manifest).close();

        loadAgent(tmpFile);
      } finally {
        Files.deleteIfExists(tmpFile);
      }
    } catch (Throwable throwable) {
      throw new InitializationException("attach failed", throwable);
    }
  }

  protected void loadAgent(Path agentFile) throws Throwable {
    try {
      JavaInternals.TRUSTED.findStatic(
        Class.forName("sun.instrument.InstrumentationImpl"),
        "loadAgent",
        MethodType.methodType(void.class, String.class)
      ).invokeExact(agentFile.toString());
    } catch (Throwable throwable) {
      throw new InitializationException(
        "failed to invoke sun.instrument.InstrumentationImpl::loadAgent", throwable
      );
    }
  }

  protected Class<?> createAgentClass(ClassRedefiner redefiner, String className) {
    ClassFile classFile = new ClassFile(Opcodes.ACC_PUBLIC, className);
    ClassField field = classFile.visitField(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
      "I", "Ljava/lang/instrument/Instrumentation;"
    );

    Insns insns = classFile.visitMethod(
      Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
      "agentmain", "(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V"
    ).getInstructions();

    insns.varOp(Opcodes.ALOAD, 1);
    insns.fieldSetter(field);
    insns.op(Opcodes.RETURN);

    return redefiner.getClassSerializer()
      .defineClass(classFile, ClassLoader.getPlatformClassLoader());
  }
}
