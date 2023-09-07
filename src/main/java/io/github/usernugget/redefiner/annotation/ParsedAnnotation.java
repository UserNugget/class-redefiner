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

package io.github.usernugget.redefiner.annotation;

import io.github.usernugget.redefiner.util.asm.desc.ParsedField;
import io.github.usernugget.redefiner.util.asm.desc.ParsedMethod;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public class ParsedAnnotation {
  private final Map<String, Object> values;

  public ParsedAnnotation(Map<String, Object> values) {
    this.values = values;
  }

  public ParsedAnnotation(AnnotationNode annotation) { 
    this.values = parseAnnotation(annotation);
  }

  private static Map<String, Object> parseAnnotation(AnnotationNode annotation) {
    Map<String, Object> values = new HashMap<>();
    if (annotation.values == null) {
      return values;
    }
    
    if (annotation.values.size() % 2 != 0) {
      throw new IllegalStateException(
        "invalid annotation: " + annotation.values.size() + " % 2 != 0"
      );
    }

    for (int i = 0; i < annotation.values.size(); i += 2) {
      Object result = annotation.values.get(i + 1);
      if (result instanceof AnnotationNode) {
        result = parseAnnotation((AnnotationNode) result);
      }

      values.put(
        (String) annotation.values.get(i),
        result
      );
    }
    
    return values;
  }

  public ParsedMethod getMethod(String key) {
    return new ParsedMethod(this.getString(key));
  }

  public ParsedField getField(String key) {
    return new ParsedField(this.getString(key));
  }

  public Type getType(String key) {
    return Type.getObjectType(this.getString(key));
  }

  public String getString(String key) {
    return (String) this.get(key);
  }

  public Object get(String key) {
    return this.values.get(key);
  }
}
