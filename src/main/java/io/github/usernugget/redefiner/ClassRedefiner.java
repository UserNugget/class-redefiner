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

import io.github.usernugget.redefiner.agent.AbstractAgent;
import io.github.usernugget.redefiner.agent.attach.AbstractAttach;
import io.github.usernugget.redefiner.agent.attach.AttachTypes;
import io.github.usernugget.redefiner.annotation.ParsedAnnotation;
import io.github.usernugget.redefiner.changes.ClassChange;
import io.github.usernugget.redefiner.changes.FieldChange;
import io.github.usernugget.redefiner.changes.MethodChange;
import io.github.usernugget.redefiner.handlers.Handler;
import io.github.usernugget.redefiner.handlers.HandlerTypes;
import io.github.usernugget.redefiner.throwables.InitializationException;
import io.github.usernugget.redefiner.util.JavaInternals;
import io.github.usernugget.redefiner.util.asm.ClassField;
import io.github.usernugget.redefiner.util.asm.ClassFile;
import io.github.usernugget.redefiner.util.asm.ClassMethod;
import io.github.usernugget.redefiner.util.asm.io.ClassSerializer;
import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import static java.util.Objects.requireNonNull;

public class ClassRedefiner implements Closeable {
  private final AttachTypes attachTypes;
  private final HandlerTypes handlerTypes;
  private ClassSerializer classSerializer = new ClassSerializer(this);

  private AbstractAgent agent;

  public ClassRedefiner(AttachTypes attachTypes, HandlerTypes handlerTypes) {
    this.attachTypes = attachTypes;
    this.handlerTypes = handlerTypes;
  }

  public void transformClass(Class<?> mapping) throws ClassNotFoundException {
    this.transformClass(mapping.getClassLoader(), mapping);
  }

  public void transformClass(ClassLoader classLoader, Class<?> mapping) throws ClassNotFoundException {
    this.transformClass(mapping.getAnnotation(Mapping.class), classLoader, mapping);
  }

  private void transformClass(Mapping mapping, ClassLoader classLoader, Class<?> klass)
      throws ClassNotFoundException {
    requireNonNull(mapping, "class '" + klass.getName() + "' should be annotated with @Mapping");
    requireNonNull(classLoader, "classLoader == null");
    requireNonNull(klass, "klass == null");

    Class<?> target;
    if (!mapping.targetClassName().isEmpty()) {
      target = Class.forName(mapping.targetClassName(), false, classLoader);
    } else {
      target = mapping.targetClass();
    }

    this.transformClass(classLoader, target, klass);
  }

  public void transformClass(Class<?> target, Class<?> mapping) {
    this.transformClass(mapping.getClassLoader(), target, mapping);
  }

  public void transformClass(ClassLoader classLoader, Class<?> target, Class<?> mapping) {
    requireNonNull(target, "target == null");
    requireNonNull(mapping, "mapping == null");

    try {
      ClassFile mappingClass = this.classSerializer.readClass(
        mapping, ClassReader.SKIP_FRAMES
      );

      this.agent.rewriteClass(target, (classData, targetLoader) -> {
        ClassFile targetClass = this.classSerializer.readClass(
          classData, ClassReader.SKIP_FRAMES
        );

        ClassChange classChange = new ClassChange(
          this,
          classLoader,
          mapping, target,
          mappingClass, targetClass
        );

        mappingClass.eachAnnotation(node ->
          this.handlerTypes.eachHandler(node.desc, desc ->
            desc.handler.handleClass(classChange))
        );

        for (MethodNode method : mappingClass.methods) {
          ClassMethod mappingMethod = (ClassMethod) method;

          mappingMethod.eachAnnotation(node ->
            this.handlerTypes.eachHandler(node.desc, desc ->
              desc.handler.handleMethod(new MethodChange(
                this,
                classLoader,
                mapping, target,
                mappingClass, targetClass,
                mappingMethod, new ParsedAnnotation(node)
              ))
            )
          );
        }

        for (FieldNode field : mappingClass.fields) {
          ClassField mappingField = (ClassField) field;

          mappingField.eachAnnotation(node ->
            this.handlerTypes.eachHandler(node.desc, desc ->
              desc.handler.handleField(new FieldChange(
                this,
                classLoader,
                mapping, target,
                mappingClass, targetClass,
                mappingField, new ParsedAnnotation(node)
              ))
            )
          );
        }

        return this.classSerializer.writeClass(
          targetClass, targetLoader, ClassWriter.COMPUTE_FRAMES
        );
      });
    } catch (IOException e) {
      throw new IllegalStateException("failed to apply mapping", e);
    }
  }

  public void initializeAgent() throws InitializationException {
    if (this.agent != null) {
      throw new InitializationException("ClassRedefiner already initialized");
    }

    AbstractAttach attach = this.attachTypes.findSupported(this);
    if (attach == null) {
      throw new InitializationException("unsupported jvm: " + JavaInternals.getJvmInfo());
    }

    this.agent = attach.createAgent(this);
  }

  public void addAttachType(int priority, AbstractAttach attacher) {
    this.attachTypes.add(priority, attacher);
  }

  public AttachTypes getAttachTypes() {
    return this.attachTypes;
  }

  public void addHandlerType(int priority, Class<Annotation> annotation, Handler handler) {
    this.handlerTypes.add(priority, Type.getType(annotation), handler);
  }

  public HandlerTypes getHandlerTypes() {
    return this.handlerTypes;
  }

  public ClassSerializer getClassSerializer() {
    return this.classSerializer;
  }

  public ClassRedefiner setClassSerializer(ClassSerializer serializer) {
    this.classSerializer = serializer;
    return this;
  }

  public AbstractAgent getAgent() {
    return this.agent;
  }

  public ClassRedefiner setAgent(AbstractAgent agent) {
    this.agent = agent;
    return this;
  }

  @Override
  public void close() {
    if (this.agent != null) {
      this.agent.close();
    }
  }
}
