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

package io.github.usernugget.redefiner.transformer;

import io.github.usernugget.redefiner.util.asm.CodeGenerator;
import io.github.usernugget.redefiner.util.asm.node.ClassFile;
import io.github.usernugget.redefiner.util.asm.node.ClassMethod;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

public class ClassTransformWrapper {
  private final BiConsumer<ClassFile, CodeGenerator> transformer;
  private ClassLoader classLoader;
  private boolean verify;
  private Throwable throwable;

  public ClassTransformWrapper(BiConsumer<ClassFile, CodeGenerator> transformer) {
    this.transformer = transformer;
  }

  public void transform(ClassFile classFile, CodeGenerator codeGenerator) {
    try {
      this.transformer.accept(classFile, codeGenerator);

      if (this.verify) {
        SimpleVerifier verifier = new SimpleVerifier(
           Type.getObjectType(classFile.name),
           Type.getObjectType(classFile.superName),
           classFile.interfaces == null ? null : classFile.interfaces.stream().map(Type::getObjectType).collect(Collectors.toList()),
           classFile.isInterface()
        );
        verifier.setClassLoader(this.classLoader);

        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        for(ClassMethod method : classFile.methods()) {
          try {
            analyzer.analyzeAndComputeMaxs(classFile.name, method);
          } catch (Throwable throwable) {
            throw new IllegalStateException("bytecode verification failed: " + method.toBytecodeString(), throwable);
          }
        }
      }
    } catch (Throwable throwable) {
      this.throwable = throwable;
    }
  }

  public boolean isVerify() {
    return this.verify;
  }

  public void setVerify(boolean verify) {
    this.verify = verify;
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public Throwable getThrowable() {
    return this.throwable;
  }

  public void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }
}
