//  Copyright (c) Zach Tellman, Rich Hickey and contributors. All rights reserved.
//  The use and distribution terms for this software are covered by the
//  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
//  which can be found in the file epl-v10.html at the root of this distribution.
//  By using this software in any fashion, you are agreeing to be bound by
//  the terms of this license.
//  You must not remove this notice, or any other, from this software.

package clojure.data.int_map;

import java.util.Iterator;
import clojure.lang.IFn;

public interface INode {

    public enum IterationType {
      KEYS,
      VALS,
      ENTRIES
    }

    long count();
    Iterator iterator(IterationType type, boolean reverse);
    INode range(long min, long max);

    INode merge(INode node, long epoch, IFn f);
    INode assoc(long k, long epoch, IFn f, Object v);
    INode dissoc(long k, long epoch);
    INode update(long k, long epoch, IFn f);
    Object get(long k, Object defaultVal);

    Object kvreduce(IFn f, Object init);
    Object reduce(IFn f, Object init);
    Object fold(long n, IFn combiner, IFn reducer, IFn fjtask, IFn fjfork, IFn fjjoin);
}
