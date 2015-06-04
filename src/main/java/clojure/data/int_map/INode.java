package clojure.data.int_map;

import java.util.Iterator;
import java.util.List;
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
