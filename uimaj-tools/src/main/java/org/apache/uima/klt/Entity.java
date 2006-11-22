/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.klt;

import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.impl.JCas;

/**
 * An atomic referent, e.g., a person, place, thing, event, idea. Updated by JCasGen Thu Apr 21
 * 11:20:08 EDT 2005 XML source: descriptors/types/hutt.xml
 * 
 * @generated
 */
public class Entity extends Referent {
  public final static String INDEX = "kltEntityIndex";

  public String toString() {
    return toString("ENTITY");
  }

  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCas.getNextIndex();

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected Entity() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public Entity(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public Entity(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

}
