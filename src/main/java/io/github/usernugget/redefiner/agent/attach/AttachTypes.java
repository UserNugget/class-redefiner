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

package io.github.usernugget.redefiner.agent.attach;

import io.github.usernugget.redefiner.ClassRedefiner;
import io.github.usernugget.redefiner.util.collections.SortedList;

public class AttachTypes extends SortedList<AbstractAttach> {
  @Override
  public boolean add(int priority, AbstractAttach value) {
    if (value == null) {
      throw new NullPointerException("attach type is null");
    }

    return super.add(priority, value);
  }

  public AbstractAttach findSupported(ClassRedefiner redefiner) {
    for (AbstractAttach attach : getValues().values()) {
      if (attach.isSupported(redefiner)) {
        return attach;
      }
    }

    return null;
  }
}
