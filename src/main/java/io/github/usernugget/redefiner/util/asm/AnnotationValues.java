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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationValues {
  private final Map<String, Object> objects;

  public AnnotationValues() {
    this(new HashMap<>());
  }

  public AnnotationValues(Map<String, Object> objects) {
    this.objects = objects;
  }

  public AnnotationValues(AnnotationNode annotationNode) {
    this.objects = parseList(annotationNode.values);
  }

  private static Object parseParameter(Object o) {
    if (o instanceof AnnotationNode) {
      return parseList(((AnnotationNode) o).values);
    } else if (o instanceof Object[]) {
      return parseList(Arrays.asList((Object[]) o));
    }

    return o;
  }

  private static Map<String, Object> parseList(List<Object> params) {
    Map<String, Object> arguments = new HashMap<>();
    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        if (i % 2 == 1) {
          arguments.put(params.get(i - 1).toString(), parseParameter(params.get(i)));
        }
      }
    }

    return arguments;
  }

  public AnnotationValues getArguments(String name) {
    return new AnnotationValues((Map<String, Object>) get(name));
  }

  public String getStringByType(Class<?> type) {
    return getString(Type.getDescriptor(type));
  }

  public String getString(String name) {
    return get(name);
  }

  public <T> T get(String name) {
    return (T) this.objects.get(name);
  }

  public <T> T getOrDefault(String name, T defaultValue) {
    return (T) this.objects.getOrDefault(name, defaultValue);
  }

  @Override
  public String toString() {
    return this.objects.toString();
  }
}
