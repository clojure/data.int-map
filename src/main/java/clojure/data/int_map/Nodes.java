package clojure.data.int_map;

import clojure.lang.IFn;
import clojure.lang.AFn;
import clojure.lang.RT;
import clojure.lang.Util;

import java.util.ArrayList;
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
      return ((InvertFn) f).f;
    }
    return new InvertFn(f);
  }

  // bitwise helper functions

  public static long lowestBit(long n) {
    return n & -n;
  }

  private static final byte deBruijnIndex[] =
          new byte[]{0, 1, 2, 53, 3, 7, 54, 27, 4, 38, 41, 8, 34, 55, 48, 28,
                  62, 5, 39, 46, 44, 42, 22, 9, 24, 35, 59, 56, 49, 18, 29, 11,
                  63, 52, 6, 26, 37, 40, 33, 47, 61, 45, 43, 21, 23, 58, 17, 10,
                  51, 25, 36, 32, 60, 20, 57, 16, 50, 31, 19, 15, 30, 14, 13, 12};

  public static int bitLog2(long n) {
    return deBruijnIndex[0xFF & (int) ((n * 0x022fdd63cc95386dL) >>> 58)];
  }

  public static int offset(long a, long b) {
    return bitLog2(highestBit(a ^ b, 1)) & ~0x3;
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

  // 16-way branch node

  public static class Branch implements INode {
    public final long prefix, mask, epoch;
    public final int offset;
    long count;
    public final INode[] children;

    public Branch(long prefix, int offset, long epoch, long count, INode[] children) {
      this.prefix = prefix;
      this.offset = offset;
      this.epoch = epoch;
      this.mask = 0xf << offset;
      this.count = count;
      this.children = children;
    }

    public Branch(long prefix, int offset, long epoch, INode[] children) {
      this.prefix = prefix;
      this.offset = offset;
      this.epoch = epoch;
      this.mask = 0xf << offset;
      this.count = -1;
      this.children = children;
    }

    public int indexOf(long key) {
      return (int) (key & mask) >>> offset;
    }

    private INode[] arraycopy() {
      INode[] copy = new INode[16];
      System.arraycopy(children, 0, copy, 0, 16);
      return copy;
    }

    public Object get(long k, Object defaultVal) {
      INode n = children[indexOf(k)];
      return n == null ? defaultVal : n.get(k, defaultVal);
    }

    public long count() {
      int count = 0;
      for (int i = 0; i < 16; i++) {
        INode n = children[i];
        if (n != null) count += n.count();
      }
      this.count = count;
      return count;
    }

    public INode merge(INode node, long epoch, IFn f) {
      if (node instanceof Branch) {
        Branch branch = (Branch) node;

        // we contain the other node
        if (offset > branch.offset) {
          int idx = indexOf(branch.prefix);
          INode[] children = arraycopy();
          INode n = children[idx];
          children[idx] = n != null ? n.merge(node, epoch, f) : node;
          return new Branch(prefix, offset, epoch, children);

          // the other node contains us
        } else if (offset < branch.offset) {
          return branch.merge(this, epoch, invert(f));

          // same level, do a child-wise merge
        } else {
          INode[] children = new INode[16];
          INode[] branchChildren = branch.children;
          for (int i = 0; i < 16; i++) {
            INode n = this.children[i];
            INode nPrime = branchChildren[i];
            if (n == null) {
              children[i] = nPrime;
            } else if (nPrime == null) {
              children[i] = n;
            } else {
              children[i] = n.merge(nPrime, epoch, f);
            }
          }
          return new Branch(prefix, offset, epoch, children);
        }
      } else {
        return node.merge(this, epoch, invert(f));
      }
    }

    public void entries(List accumulator) {
        if (offset == 60) {
            if (children[15] != null) {
                children[15].entries(accumulator);
            }
            for (int i = 0; i < 15; i++) {
                INode n = children[i];
                if (n != null) n.entries(accumulator);
            }
        } else {
            for (int i = 0; i < 16; i++) {
                INode n = children[i];
                if (n != null) n.entries(accumulator);
            }
        }
    }

    public INode assoc(long k, long epoch, IFn f, Object v) {
      int offsetPrime = offset(k, prefix);

      // need a new branch above us both
      if (offsetPrime > this.offset) {
        return new Branch(k, offsetPrime, epoch, new INode[16])
                .merge(this, epoch, null)
                .assoc(k, epoch, f, v);

        // somewhere at or below our level
      } else {
        int idx = indexOf(k);
        INode n = children[idx];
        if (n == null) {
          if (epoch == this.epoch) {
            children[idx] = new Leaf(k, epoch, v);
            count = -1;
            return this;
          } else {
            INode[] children = arraycopy();
            children[idx] = new Leaf(k, epoch, v);
            return new Branch(prefix, offset, epoch, count, children);
          }
        } else {
          INode nPrime = n.assoc(k, epoch, f, v);
          if (nPrime == n) {
            count = -1;
            return this;
          } else {
            INode[] children = arraycopy();
            children[idx] = nPrime;
            return new Branch(prefix, offset, epoch, count, children);
          }
        }
      }
    }

    public INode dissoc(long k, long epoch) {
      int idx = indexOf(k);
      INode n = children[idx];
      if (n == null) {
        return this;
      } else {
        INode nPrime = n.dissoc(k, epoch);
        if (nPrime == n) {
          count = -1;
          return this;
        } else {
          INode[] children = arraycopy();
          children[idx] = nPrime;
          return new Branch(prefix, offset, epoch, count, children);
        }
      }
    }

    public INode update(long k, long epoch, IFn f) {
      int idx = indexOf(k);
      INode n = children[idx];
      if (n == null) {
        if (epoch == this.epoch) {
          children[idx] = new Leaf(k, epoch, f.invoke(null));
          count = -1;
          return this;
        } else {
          INode[] children = arraycopy();
          children[idx] = new Leaf(k, epoch, f.invoke(null));
          return new Branch(prefix, offset, epoch, count, children);
        }
      } else {
        INode nPrime = n.update(k, epoch, f);
        if (nPrime == n) {
          count = -1;
          return this;
        } else {
          INode[] children = arraycopy();
          children[idx] = nPrime;
          return new Branch(prefix, offset, epoch, count, children);
        }
      }
    }

    public Object kvreduce(IFn f, Object init) {
      for (int i = 0; i < 16; i++) {
        INode n = children[i];
        if (n != null) init = n.kvreduce(f, init);
        if (RT.isReduced(init)) break;
      }
      return init;
    }

    public Object reduce(IFn f, Object init) {
      for (int i = 0; i < 16; i++) {
        INode n = children[i];
        if (n != null) init = n.reduce(f, init);
        if (RT.isReduced(init)) break;
      }
      return init;
    }

    // adapted from the PersistentHashMap.ArrayNode implementation
    static public Object foldTasks(List<Callable> tasks, final IFn combiner, final IFn fjtask, final IFn fjfork, final IFn fjjoin) {

      if (tasks.isEmpty()) {
        return combiner.invoke();

        // just wait on the one value
      } else if (tasks.size() == 1) {
        try {
          return tasks.get(0).call();
        } catch (Exception e) {
          throw Util.sneakyThrow(e);
        }

        // divide and conquer
      } else {
        List<Callable> t1 = tasks.subList(0, tasks.size() / 2);
        final List<Callable> t2 = tasks.subList(tasks.size() / 2, tasks.size());

        Object forked = fjfork.invoke(fjtask.invoke(new Callable() {
          public Object call() throws Exception {
            return foldTasks(t2, combiner, fjtask, fjfork, fjjoin);
          }
        }));

        return combiner.invoke(foldTasks(t1, combiner, fjtask, fjfork, fjjoin), fjjoin.invoke(forked));
      }
    }

    public Object fold(final long n, final IFn combiner, final IFn reducer, final IFn fjtask, final IFn fjfork, final IFn fjjoin) {
      if (n > count()) {
        List<Callable> tasks = new ArrayList();
        for (int i = 0; i < 16; i++) {
          final INode node = children[i];
          if (node != null) {
            tasks.add(new Callable() {
              public Object call() throws Exception {
                return node.fold(n, combiner, reducer, fjtask, fjfork, fjjoin);
              }
            });
          }
        }
        return foldTasks(tasks, combiner, fjtask, fjfork, fjjoin);
      } else {
        return kvreduce(reducer, combiner.invoke());
      }
    }
  }

  // leaf node
  public static class Leaf implements INode {
    public final long key, epoch;
    public volatile Object value;

    public Leaf(long key, long epoch, Object value) {
      this.key = key;
      this.epoch = epoch;
      this.value = value;
    }

    public Object reduce(IFn f, Object init) {
      return f.invoke(init, new clojure.lang.MapEntry(key, value));
    }

    public Object kvreduce(IFn f, Object init) {
      return f.invoke(init, key, value);
    }

    public Object fold(long n, IFn combiner, IFn reducer, IFn fjtask, IFn fjfork, IFn fjjoin) {
      return kvreduce(reducer, combiner.invoke());
    }

    public long count() {
      return 1;
    }

    public INode merge(INode node, long epoch, IFn f) {
      return node.assoc(key, epoch, invert(f), value);
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
        return new Branch(k, offset(k, key), epoch, new INode[16])
                .assoc(key, epoch, f, value)
                .assoc(k, epoch, f, v);
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

    public Object kvreduce(IFn f, Object init) {
      return init;
    }

    public Object fold(long n, IFn combiner, IFn reducer, IFn fjtask, IFn fjfork, IFn fjjoin) {
      return combiner.invoke();
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
