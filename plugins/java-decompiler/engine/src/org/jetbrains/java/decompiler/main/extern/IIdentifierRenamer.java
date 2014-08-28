/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.java.decompiler.main.extern;


public interface IIdentifierRenamer {

  public static final int ELEMENT_CLASS = 1;

  public static final int ELEMENT_FIELD = 2;

  public static final int ELEMENT_METHOD = 3;


  public boolean toBeRenamed(int element_type, String classname, String element, String descriptor);

  public String getNextClassname(String fullname, String shortname);

  public String getNextFieldname(String classname, String field, String descriptor);

  public String getNextMethodname(String classname, String method, String descriptor);
}
