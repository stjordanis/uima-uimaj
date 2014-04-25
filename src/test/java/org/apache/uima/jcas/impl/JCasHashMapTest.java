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

package org.apache.uima.jcas.impl;

import java.util.Random;

import junit.framework.TestCase;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * 
 *
 */
public class JCasHashMapTest extends TestCase {
  static private class FakeTopType extends TOP_Type {
    public FakeTopType() {
      super();
    }    
  }
  
  static final TOP_Type FAKE_TOP_TYPE_INSTANCE = new FakeTopType(); 
  static final int SIZE = 20000;  // set > 2 million for cache avoidance timing tests
  static final long SEED = 12345;
  static Random r = new Random(SEED);
  static private int[] addrs = new int[SIZE];
  static int prev = 0;
  
  static {   
    for (int i = 0; i < SIZE; i++) { 
      addrs[i] = prev = prev + r.nextInt(14) + 1;
    }
    for (int i = SIZE - 1; i >= 1; i--) {
      int ir = r.nextInt(i+1);
      int temp = addrs[i];
      addrs[i] = addrs[ir];
      addrs[ir] = temp;
    }
  }

  public void testWithPerf()  {
    
    for (int i = 0; i <  5; i++ ) {
      arun(SIZE);
    }
    
    arunCk(SIZE);

//    for (int i = 0; i < 50; i++ ) {
//      arun2(2000000);
//    }

  }
  
  public void testMultiThread() throws Exception {
    final Random random = new Random();
    int numberOfThreads = MultiThreadUtils.PROCESSORS;    
    System.out.format("test JCasHashMap with %d threads", numberOfThreads);
    
    final JCasHashMap m = new JCasHashMap(200, true); // true = do use cache 

    MultiThreadUtils.Run2isb run2isb = new MultiThreadUtils.Run2isb() {
      
      public void call(int threadNumber, int repeatNumber, StringBuilder sb) {
        for (int k = 0; k < 4; k++) {
          for (int i = 0; i < SIZE / 4; i++) {
            final int key = addrs[random.nextInt(SIZE / 16)];
            FeatureStructureImpl fs = m.getReserve(key);
            if (null == fs) {
              m.put(new TOP(key, FAKE_TOP_TYPE_INSTANCE));
            }
          }
          try {
            Thread.sleep(0, random.nextInt(1000));
          } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
//        System.out.println(sb.toString());
      }
    };  
    MultiThreadUtils.tstMultiThread("JCasHashMapTest",  numberOfThreads,  10, run2isb);
  }

  /**
   * Create situation
   *   make a set of indexed fs instances, no JCas
   *   on multiple threads, simultaneously, attempt to get the jcas cover object for this
   *     one getReserve should succeed, but reserve, and the others should "wait".
   *     then put
   *     then the others should "wakeup" and return the same instance 
   *   
   * @throws Exception
   */
  public void testMultiThreadCollide() throws Exception {
    int numberOfThreads = MultiThreadUtils.PROCESSORS;
    if (numberOfThreads < 2) {
      return;
    }
    System.out.format("test JCasHashMap collide with %d threads", numberOfThreads);
    final Random r = new Random();
    final JCasHashMap m = new JCasHashMap(200, true); // true = do use cache 
    final int hashKey = 15;
    final TOP fs = new TOP(hashKey, FAKE_TOP_TYPE_INSTANCE);
    
    final Thread[] threads = new Thread[numberOfThreads];
    final FeatureStructureImpl[] found = new FeatureStructureImpl[numberOfThreads];
    
    for (int i = 0; i < numberOfThreads; i++) {
      final int finalI = i;
      threads[i] = new Thread(new Runnable() {
        public void run() {
          try {
            Thread.sleep(r.nextInt(5));
          } catch (InterruptedException e) {
          }
           found[finalI] = m.getReserve(hashKey);
        }
      });
      threads[i].start();
    }
    Thread.sleep(100); 
    // verify that one thread finished, others are waiting
    int numberWaiting = 0;
    int threadFinished = -1;
    for (int i = 0; i < numberOfThreads; i++) {
      if (threads[i].isAlive()) {
        numberWaiting ++;
      } else {
        threadFinished = i;
      }
    }
    
    assertEquals(numberOfThreads - 1, numberWaiting);
    m.put(fs);
    found[threadFinished] = fs;
    Thread.sleep(10);  
    
    numberWaiting = 0;
    for (int i = 0; i < numberOfThreads; i++) {
      if (threads[i].isAlive()) {
        numberWaiting ++;
      }
    }
    assertEquals(0, numberWaiting);
    System.out.format("JCasHashMapTest collide,  found = %s%n", intList(found));
    for (FeatureStructureImpl f : found) {
      if (f != fs) {
        System.err.format("JCasHashMapTest miscompare fs = %s,  f = %s%n", fs, (f == null) ? "null" : f);
      }
      assertTrue(f == fs);
    }
  }


  
//  private void arun2(int n) {
//    JCasHashMap2 m = new JCasHashMap2(200, true); 
//    assertTrue(m.size() == 0);
//    assertTrue(m.getbitsMask() == 0x000000ff);
//    
//    JCas jcas = null;
//    
//    long start = System.currentTimeMillis();
//    for (int i = 0; i < n; i++) {
//      TOP fs = new TOP(7 * i, NULL_TOP_TYPE_INSTANCE);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.putAtLastProbeAddr(fs);
//      }
//    }
//    System.out.format("time for v2 %,d is %,d ms%n",
//        n, System.currentTimeMillis() - start);
//    m.showHistogram();
//
//  }
   
  private void arun(int n) {
    JCasHashMap m = new JCasHashMap(200, true); // true = do use cache 
    assertTrue(m.size() == 0);
       
    long start = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.get(7 * i);
        m.put(fs);
//      }
    }
    System.out.format("time for v1 %,d is %,d ms%n",
        n, System.currentTimeMillis() - start);
    m.showHistogram();

  }
  
  private void arunCk(int n) {
    JCasHashMap m = new JCasHashMap(200, true); // true = do use cache
    
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
//      FeatureStructureImpl v = m.get(fs.getAddress());
//      if (null == v) {
//        m.get(7 * i);
//        m.findEmptySlot(key);
        m.put(fs);
//      }
    }
    
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = (TOP) m.getReserve(key);
      if (fs == null) {  // for debugging
        System.out.println("stop");
      }
      assertTrue(null != fs);
    }

  }
  
  public void testGrowth() {
    System.out.println("JCasHashMapTest growth");
    int cores = Runtime.getRuntime().availableProcessors();
    double loadfactor = .6;
    int sub_capacity = 64;
    int agg_capacity = cores * sub_capacity;
    JCasHashMap m = new JCasHashMap(agg_capacity, true); // true = do use cache 
    assertTrue(m.size() == 0);
     
    int switchpoint = (int)Math.floor(agg_capacity * loadfactor);
    fill(switchpoint, m);
    assertTrue(checkSubsCapacity(m, 64, 128));
    System.out.println("JCasHashMapTest growth - adding 1 past switchpoint");
    m.put(new TOP(addrs[switchpoint + 1], null));
    assertTrue(checkSubsCapacity(m, 64, 128));
    
    m.clear();
    assertTrue(checkSubsCapacity(m, 64, 128));


    fill(switchpoint, m);
    assertTrue(checkSubsCapacity(m, 64, 128));
    m.put(new TOP(addrs[switchpoint + 1], null));
    assertTrue(checkSubsCapacity(m, 64, 128));

    m.clear();  // size is above switchpoint, so no shrinkage
    assertTrue(checkSubsCapacity(m, 64, 128));
    m.clear();  // size is 0, so first time shrinkage a possibility
    assertTrue(checkSubsCapacity(m, 64, 128)); // but we don't shrink on first time
    m.clear(); 
    assertTrue(checkSubsCapacity(m, 64, 64));  // but we do on second time
    m.clear(); 
    assertTrue(checkSubsCapacity(m, 64, 64));
    m.clear(); 
    assertTrue(checkSubsCapacity(m, 64, 64));  // don't shrink below minimum
  }

  private boolean checkSubsCapacity(JCasHashMap m, int v, int v2) {
    int[] caps = m.getCapacities();
    for (int i : caps) {
      if (i == v || i == v2 ) {
        continue;
      }
      System.err.format("JCasHashMapTest: expected %d or %d, but got %s%n", v, v2, intList(caps));
      return false;
    }
    System.out.format("JCasHashMapTest: capacities are: %s%n", intList(caps));
    return true;
  }
  
  private String intList(int[] a) {
    StringBuilder sb = new StringBuilder();
    for (int i : a) {
      sb.append(i).append(", ");
    }
    return sb.toString();
  }
  
  private String intList(FeatureStructureImpl[] a) {
    StringBuilder sb = new StringBuilder();
    for (FeatureStructureImpl i : a) {
      sb.append(i == null ? "null" : i.getAddress()).append(", ");
    }
    return sb.toString();
  }
  
  private void fill (int n, JCasHashMap m) {
    for (int i = 0; i < n; i++) {
      final int key = addrs[i];
      TOP fs = new TOP(key, FAKE_TOP_TYPE_INSTANCE);
      m.put(fs);
//      System.out.format("JCasHashMapTest fill %s%n",  intList(m.getCapacities()));
    }
  }
}
