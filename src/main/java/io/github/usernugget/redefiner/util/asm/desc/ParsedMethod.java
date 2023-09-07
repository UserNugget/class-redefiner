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

package io.github.usernugget.redefiner.util.asm.desc;

public class ParsedMethod {
  private String name;
  private String desc;

  public ParsedMethod(String descriptor) {
    int start = descriptor.indexOf('(');

    if (start != -1) {
      this.name = descriptor.substring(0, start);
      this.desc = descriptor.substring(start);
    } else {
      this.name = descriptor;
    }

  }

  public ParsedMethod(String name, String desc) {
    this.name = name;
    this.desc = desc;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDesc() {
    return this.desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }
}
