package clojure.data.int_map;

import java.util.List;
import clojure.lang.IFn;

public interface INode {
    long count();
    INode merge(INode node, long epoch, IFn f);
    void entries(List accumulator);
    INode assoc(long k, long epoch, IFn f, Object v);
    INode dissoc(long k, long epoch);
    INode update(long k, long epoch, IFn f);
    Object get(long k, Object defaultVal);

    Object reduce(IFn f, Object init);
    // Object fold(long n, IFn combiner, IFn reducer, IFn joiner);
}
