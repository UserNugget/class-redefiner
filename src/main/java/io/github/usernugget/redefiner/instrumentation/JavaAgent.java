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

package io.github.usernugget.redefiner.instrumentation;

import com.sun.tools.attach.VirtualMachine;
import io.github.usernugget.redefiner.util.asm.ClassIO;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import io.github.usernugget.redefiner.util.JavaInternals;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

public enum JavaAgent {
  OPENJ9(() -> {
    if (Objects.equals(System.getProperty("java.vm.vendor"), "Eclipse OpenJ9")) {
      try {
        Class<?> vmClass = Class.forName("openj9.internal.tools.attach.target.AttachHandler");

        try {
          Field selfAttachField = vmClass.getDeclaredField("allowAttachSelf");
          JavaInternals.UNSAFE.putObject(
             JavaInternals.UNSAFE.staticFieldBase(selfAttachField),
             JavaInternals.UNSAFE.staticFieldOffset(selfAttachField),
             "true"
          );
        } catch (Throwable e) {
          throw new IllegalStateException("unable to modify allowAttachSelf", e);
        }
        return true;
      } catch (ClassNotFoundException ignored) { }
    }

    return false;
  }),
  HOTSPOT(() -> {
    try {
      Class<?> vmClass = Class.forName("sun.tools.attach.HotSpotVirtualMachine");

      try {
        Field selfAttachField = vmClass.getDeclaredField("ALLOW_ATTACH_SELF");
        JavaInternals.UNSAFE.putBoolean(
           JavaInternals.UNSAFE.staticFieldBase(selfAttachField),
           JavaInternals.UNSAFE.staticFieldOffset(selfAttachField),
           true
        );
      } catch (Throwable e) {
        throw new IllegalStateException("unable to modify ALLOW_ATTACH_SELF", e);
      }

      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  });

  private static final String CLASS_NAME = Type.getInternalName(JavaAgent.class) + "$" + Long.toString(System.nanoTime(), 36);

  private final BooleanSupplier injectionStatus;

  JavaAgent(BooleanSupplier injectionStatus) {
    this.injectionStatus = injectionStatus;
  }

  public static JavaAgent prepareAgent() {
    for (JavaAgent agent : values()) {
      if(agent.injectionStatus.getAsBoolean()) {
        return agent;
      }
    }

    throw new IllegalStateException("unknown jvm: " + System.getProperty("java.vm.vendor"));
  }

  private static String getProcessId() {
    if(JavaInternals.CLASS_MAJOR_VERSION >= 53) {
      return Long.toString(ProcessHandle.current().pid());
    } else {
      // TODO: Java 8 support
      throw new IllegalStateException();
    }
  }

  private void injectAgent() throws Throwable {
    Path tmpFile = Files.createTempFile("javaagent-", ".zip");

    try {
      Manifest manifest = new Manifest();
      Attributes attributes = manifest.getMainAttributes();

      attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
      attributes.putValue("Agent-Class", CLASS_NAME.replace('/', '.'));
      attributes.putValue("Can-Retransform-Classes", "true");
      attributes.putValue("Can-Redefine-Classes", "true");

      new JarOutputStream(Files.newOutputStream(tmpFile), manifest).close();

      VirtualMachine vm = VirtualMachine.attach(getProcessId());
      vm.loadAgent(tmpFile.toString());
      vm.detach();
    } finally {
      Files.deleteIfExists(tmpFile);
    }
  }

  private Class<?> defineAgentClass() {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

    ClassFile classFile = new ClassFile();
    classFile.visit(JavaInternals.CLASS_MAJOR_VERSION, ACC_PUBLIC, CLASS_NAME, null, "java/lang/Object", null);
    classFile.visitField(ACC_PUBLIC | ACC_STATIC, "I", "Ljava/lang/instrument/Instrumentation;", null, null);

    ClassMethod method = classFile.visitMethod(ACC_PUBLIC | ACC_STATIC, "agentmain", "(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V", null, null);
    method.visitVarInsn(ALOAD, 1);
    method.visitFieldInsn(PUTSTATIC, CLASS_NAME, "I", "Ljava/lang/instrument/Instrumentation;");
    method.visitInsn(RETURN);

    byte[] classBytes = ClassIO.writeClass(
       classFile,
       ClassLoader.getSystemClassLoader(),
       ClassWriter.COMPUTE_FRAMES
    );

    return JavaInternals.defineClass(
       CLASS_NAME, classBytes, 0, classBytes.length,
       ClassLoader.getSystemClassLoader(), null
    );
  }

  public Instrumentation createInstrmentation() {
    try {
      Class<?> agentClass = defineAgentClass();
      Field instrumentationField = agentClass.getDeclaredField("I");

      injectAgent();

      return (Instrumentation) JavaInternals.UNSAFE.getObject(
         JavaInternals.UNSAFE.staticFieldBase(instrumentationField),
         JavaInternals.UNSAFE.staticFieldOffset(instrumentationField)
      );
    } catch (Throwable throwable) {
      throw new IllegalStateException("failed to create Instrumentation", throwable);
    }
  }
}
