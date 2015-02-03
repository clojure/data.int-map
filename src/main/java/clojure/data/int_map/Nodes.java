package clojure.data.int_map;

import clojure.lang.IFn;
import clojure.lang.AFn;
import clojure.lang.RT;
import java.util.List;
import java.util.concurrent.Callable;

public class Nodes {

    static class InvertFn extends AFn {
        IFn f;

        public InvertFn(IFn f) {
            this.f = f;
        }

        public Object invoke(Object x, Object y) {
            return f.invoke(y, x);
        }
    }

    static public IFn invert(IFn f) {
        if (f instanceof InvertFn) {
            return ((InvertFn)f).f;
        }
        return new InvertFn(f);
    }

    // bitwise helper functions

    public static boolean isOff(long n, long mask) {
        return (n & mask) == 0;
    }

    public static long bitMask(long prefix, long mask) {
        return (prefix | (mask - 1)) & ~mask;
    }

    public static boolean isMatch(long key, long prefix, long mask) {
        long decMask = mask - 1;
        long notMask = ~mask;

        return ((prefix | decMask) & notMask) == ((key | decMask) & notMask);
    }

    public static long lowestBit(long n) {
        return n & ~(n - 1);
    }

    public static long highestBit(long n, long estimate) {
        long x = n & ~(estimate - 1);
        long m;
        while (true) {
            m = lowestBit(x);
            if (x == m) return m;
            x -= m;
        }
    }

    public static long branchingBit(long prefix0, long mask0, long prefix1, long mask1) {
        return highestBit(prefix0 ^ prefix1, Math.max(1, 2*Math.max(mask0, mask1)));
    }

    public static Branch join(long epoch, long prefix0, long mask0, INode n0, long prefix1, long mask1, INode n1) {
        long mask = branchingBit(prefix0, mask0, prefix1, mask1);
        if (isOff(prefix0, mask)) {
            return new Branch(prefix0, mask, epoch, n0, n1);
        } else {
            return new Branch(prefix0, mask, epoch, n1, n0);
        }
    }

    // branch node

    public static class Branch implements INode {
        public final long prefix, mask, epoch;
        long count;
        public INode left, right;

        public Branch(long prefix, long mask, long epoch, long count, INode left, INode right) {
            this.prefix = bitMask(prefix, mask);
            this.mask = mask;
            this.epoch = epoch;
            this.count = count;
            this.left = left;
            this.right = right;
        }

        public Branch(long prefix, long mask, long epoch, INode left, INode right) {
            this.prefix = bitMask(prefix, mask);
            this.mask = mask;
            this.epoch = epoch;
            this.count = -1;
            this.left = left;
            this.right = right;
        }

        public Object reduce(IFn f, Object init) {
            if (RT.isReduced(init)) return init;
            init = left.reduce(f, init);
            if (RT.isReduced(init)) return init;
            return right.reduce(f, init);
        }

        public Object fold(long n, IFn combiner, IFn reducer, IFn fjinvoke, IFn fjtask, IFn fjfork, IFn fjjoin) {
            if (count() < 2*n) {
                return reduce(reducer, combiner.invoke());
            } else {
                /*Callable c =
                    new Callable {
                        public Object call() throws Exception {

                        }
                    }
                    fjinvoke.invoke*/
                return null;
            }

        }
        public long count() {
            if (count == -1) {
                count = left.count() + right.count();
            }
            return count;
        }

        public INode update(long k, long epoch, IFn f) {
            if (k <= prefix) {
                INode l = left.update(k, epoch, f);
                if (l == left) return this;
                return new Branch(prefix, mask, epoch, count, l, right);
            } else {
                INode r = right.update(k, epoch, f);
                if (r == right) return this;
                return new Branch(prefix, mask, epoch, count, left, r);
            }
        }

        public INode merge(INode node, long epoch, IFn f) {
            if (node instanceof Branch) {
                Branch b = (Branch) node;

                if (prefix == b.prefix && mask == b.mask) {
                    // recursively merge
                    return new Branch(prefix, mask, epoch, left.merge(b.left, epoch, f), right.merge(b.right, epoch, f));

                } else if (mask > b.mask && isMatch(b.prefix, prefix, mask)) {
                    // we contain the other node
                    if (isOff(b.prefix, mask)) {
                        return new Branch(prefix, mask, epoch, left.merge(b, epoch, f), right);
                    } else {
                        return new Branch(prefix, mask, epoch, left, right.merge(b, epoch, f));
                    }

                } else if (mask < b.mask && isMatch(prefix, b.prefix, b.mask)) {
                    // the other node contains us
                    if (isOff(prefix, b.mask)) {
                        return new Branch(b.prefix, b.mask, epoch, merge(b.left, epoch, f), b.right);
                    } else {
                        return new Branch(b.prefix, b.mask, epoch, b.left, merge(b.right, epoch, f));
                    }

                } else {
                    // create node that contains both of us
                    return join(epoch, prefix, mask, this, b.prefix, b.mask, b);
                }
            } else {
                return node.merge(this, epoch, invert(f));
            }
        }

        public void entries(List accumulator) {
            left.entries(accumulator);
            right.entries(accumulator);
        }

        public INode assoc(long k, long epoch, IFn f, Object v) {
            if (isMatch(k, prefix, mask)) {
                if (isOff(k, mask)) {
                    INode l = left.assoc(k, epoch, f, v);
                    if (l == left) {
                        return this;
                    } else if (epoch == this.epoch) {
                        left = l;
                        return this;
                    }
                    return new Branch(prefix, mask, epoch, l, right);
                } else {
                    INode r = right.assoc(k, epoch, f, v);
                    if (r == right) {
                        return this;
                    } else if (epoch == this.epoch) {
                        right = r;
                        return this;
                    }
                    return new Branch(prefix, mask, epoch, left, r);
                }
            } else {
                return join(epoch, k, mask, new Leaf(k, epoch, v), prefix, mask, this);
            }
        }

        public INode dissoc(long k, long epoch) {
            if (k <= prefix) {
                INode l = left.dissoc(k, epoch);
                if (l == left) return this;
                return new Branch(prefix, mask, epoch, l, right);
            } else {
                INode r = right.dissoc(k, epoch);
                if (r == right) return this;
                return new Branch(prefix, mask, epoch, left, r);
            }
        }

        public Object get(long k, Object defaultVal) {
            if (k <= prefix) {
                return left.get(k, defaultVal);
            } else {
                return right.get(k, defaultVal);
            }
        }
    }

    // leaf node
    public static class Leaf implements INode {
        public final long key, epoch;
        volatile Object value;

        public Leaf(long key, long epoch, Object value) {
            this.key = key;
            this.epoch = epoch;
            this.value = value;
        }

        public Object reduce(IFn f, Object init) {
            return f.invoke(init, new clojure.lang.MapEntry(key, value));
        }

        public long count() {
            return 1;
        }

        public INode merge(INode node, long epoch, IFn f) {
            return node.assoc(key, epoch, f, value);
        }

        public void entries(List accumulator) {
            accumulator.add(new clojure.lang.MapEntry(key, value));
        }

        public INode assoc(long k, long epoch, IFn f, Object v) {
            if (k == key) {
                v = f == null ? v : f.invoke(value, v);
                if (this.epoch == epoch) {
                    value = v;
                    return this;
                } else {
                    return new Leaf(k, epoch, v);
                }
            } else {
                return join(epoch, k, 0, new Leaf(k, epoch, v), key, 0, this);
            }
        }

        public INode dissoc(long k, long epoch) {
            if (key == k) {
                return new Empty();
            } else {
                return this;
            }
        }

        public INode update(long k, long epoch, IFn f) {
            if (k == key) {
                Object v = f.invoke(value);
                if (this.epoch == epoch) {
                    value = v;
                    return this;
                } else {
                    return new Leaf(k, epoch, v);
                }
            } else {
                return this.assoc(k, epoch, null, f.invoke(null));
            }
        }

        public Object get(long k, Object defaultVal) {
            if (k == key) return value;
            return defaultVal;
        }
    }

    // empty node
    public static class Empty implements INode {

        public Empty() {
        }

        public Object reduce(IFn f, Object init) {
            return init;
        }

        public long count() {
            return 0;
        }

        public INode merge(INode node, long epoch, IFn f) {
            return node;
        }

        public void entries(List accumulator) {
        }

        public INode assoc(long k, long epoch, IFn f, Object v) {
            return new Leaf(k, epoch, v);
        }

        public INode dissoc(long k, long epoch) {
            return this;
        }

        public INode update(long k, long epoch, IFn f) {
            return new Leaf(k, epoch, f.invoke(null));
        }

        public Object get(long k, Object defaultVal) {
            return defaultVal;
        }
    }
}
