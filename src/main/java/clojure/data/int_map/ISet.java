//  Copyright (c) Zach Tellman, Rich Hickey and contributors. All rights reserved.
//  The use and distribution terms for this software are covered by the
//  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
//  which can be found in the file epl-v10.html at the root of this distribution.
//  By using this software in any fashion, you are agreeing to be bound by
//  the terms of this license.
//  You must not remove this notice, or any other, from this software.

package clojure.data.int_map;

import java.util.BitSet;
import java.util.Iterator;

public interface ISet {
  ISet add(long epoch, long val);
  ISet remove(long epoch, long val);
  boolean contains(long val);

  ISet range(long epoch, long min, long max);
  Iterator elements(long offset, boolean reverse);
  long count();

  BitSet toBitSet();

  ISet intersection(long epoch, ISet sv);
  ISet union(long epoch, ISet sv);
  ISet difference(long epoch, ISet sv);
}
