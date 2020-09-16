//  Copyright (c) Zach Tellman, Rich Hickey and contributors. All rights reserved.
//  The use and distribution terms for this software are covered by the
//  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
//  which can be found in the file epl-v10.html at the root of this distribution.
//  By using this software in any fashion, you are agreeing to be bound by
//  the terms of this license.
//  You must not remove this notice, or any other, from this software.

package clojure.data.int_map;

import clojure.lang.AFn;
import clojure.lang.MapEntry;

import java.util.*;

public class IntSet implements ISet {

  public class BitSetContainer implements ISet {
    public final long epoch;
    public final BitSet bitSet;

    public BitSetContainer(long epoch, BitSet bitSet) {
      this.epoch = epoch;
      this.bitSet = bitSet;
    }

    public ISet add(long epoch, long val) {
      if (epoch == this.epoch) {
        bitSet.set((short) val);
        return this;
      } else {
        BitSet bitSet = (BitSet) this.bitSet.clone();
        bitSet.set((short) val);
        return new BitSetContainer(epoch, bitSet);
      }
    }

    public ISet remove(long epoch, long val) {
      if (epoch == this.epoch) {
        bitSet.set((short) val, false);
        return this;
      } else {
        BitSet bitSet = (BitSet) this.bitSet.clone();
        bitSet.set((short) val, false);
        return new BitSetContainer(epoch, bitSet);
      }
    }

    public boolean contains(long val) {
      return bitSet.get((short) val);
    }

    public ISet range(long epoch, long min, long max) {
      BitSet bitSet = (BitSet) this.bitSet.clone();

      int size = bitSet.size();
      bitSet.set(0, Math.max((short)min, 0), false);
      if (max < size) {
        bitSet.set(Math.min((short)max+1, size), size, false);
      }
      return new BitSetContainer(epoch, bitSet);
    }

    public Iterator elements(long offset, boolean reverse) {
      List<Long> ns = new ArrayList<Long>(bitSet.cardinality());
      int idx = 0;
      while (idx < bitSet.length()) {
        idx = bitSet.nextSetBit(idx);
        ns.add(offset + idx);
        idx++;
      }
      if (reverse) {
        Collections.reverse(ns);
      }
      return ns.iterator();
    }

    public long count() {
      return bitSet.cardinality();
    }

    public BitSet toBitSet() {
      return bitSet;
    }

    public ISet intersection(long epoch, ISet val) {
      BitSet bitSet = (BitSet) this.bitSet.clone();
      bitSet.and(val.toBitSet());
      return new BitSetContainer(epoch, bitSet);
    }

    public ISet union(long epoch, ISet val) {
      BitSet bitSet = (BitSet) this.bitSet.clone();
      bitSet.or(val.toBitSet());
      return new BitSetContainer(epoch, bitSet);
    }

    public ISet difference(long epoch, ISet val) {
      BitSet bitSet = (BitSet) this.bitSet.clone();
      bitSet.andNot(val.toBitSet());
      return new BitSetContainer(epoch, bitSet);
    }
  }

  public class SingleContainer implements ISet {
    public final short val;

    public SingleContainer(short val) {
      this.val = val;
    }

    public ISet add(long epoch, long val) {
      if (val == this.val) {
        return this;
      } else {
        BitSet bitSet = new BitSet(Math.max((short) val, this.val));
        bitSet.set((short) val);
        bitSet.set(this.val);
        return new BitSetContainer(epoch, bitSet);
      }
    }

    public ISet remove(long epoch, long val) {
      return val == this.val ? null : this;
    }

    public boolean contains(long val) {
      return val == this.val;
    }

    public ISet range(long epoch, long min, long max) {
      return (min <= val && max >= val) ? this : null;
    }

    public long count() {
      return 1;
    }

    public Iterator elements(long offset, boolean reverse) {
      final long val = this.val + offset;
      return new Iterator() {

        private boolean isDone = false;

        public boolean hasNext() {
          return !isDone;
        }

        public Object next() {
          if (isDone) throw new NoSuchElementException();
          isDone = true;
          return val;
        }

        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    public BitSet toBitSet() {
      BitSet bitSet = new BitSet(val);
      bitSet.set(val);
      return bitSet;
    }

    public ISet intersection(long epoch, ISet sv) {
      return sv == null
          ? null
          : sv.contains(val)
          ? this
          : null;
    }

    public ISet union(long epoch, ISet sv) {
      return sv == null
          ? this
          : sv.contains(val)
          ? sv
          : sv.add(epoch, val);
    }

    public ISet difference(long epoch, ISet sv) {
      return sv == null
          ? this
          : sv.contains(val)
          ? null
          : this;
    }

  }

  public final INode map;
  public final short leafSize, log2LeafSize;
  public volatile int count = -1;

  public IntSet(short leafSize) {
    this.leafSize = leafSize;
    this.log2LeafSize = (short) Nodes.bitLog2(leafSize);
    map = Nodes.Empty.EMPTY;
  }

  IntSet(short leafSize, short log2LeafSize, INode map) {
    this.leafSize = leafSize;
    this.log2LeafSize = log2LeafSize;
    this.map = map;
  }

  public int leafSize() {
    return this.leafSize;
  }

  private long mapKey(long val) {
    return val >> log2LeafSize;
  }

  private short leafOffset(long val) {
    return (short) (val & (leafSize - 1));
  }

  public ISet add(final long epoch, final long val) {
    INode mapPrime = map.update(mapKey(val), epoch,
            new AFn() {
              public Object invoke(Object v) {
                ISet s = (ISet) v;
                return s == null ? new SingleContainer(leafOffset(val)) : s.add(epoch, leafOffset(val));
              }
            });
    if (mapPrime == map) {
      count = -1;
      return this;
    } else {
      return new IntSet(leafSize, log2LeafSize, mapPrime);
    }
  }

  public ISet remove(final long epoch, final long val) {
    INode mapPrime = map.update(mapKey(val), epoch,
            new AFn() {
              public Object invoke(Object v) {
                ISet s = (ISet) v;
                return s == null ? null : s.remove(epoch, leafOffset(val));
              }
            });
    if (mapPrime == map) {
      count = -1;
      return this;
    } else {
      return new IntSet(leafSize, log2LeafSize, mapPrime);
    }
  }

  public boolean contains(long val) {
    ISet s = (ISet) map.get(mapKey(val), null);
    return s != null && s.contains(leafOffset(val));
  }

  public ISet range(final long epoch, final long min, final long max) {

    if (max < min) {
      return new IntSet(leafSize);
    }

    if (mapKey(min) == mapKey(max)) {
      ISet set = (ISet) map.get(mapKey(min), null);
      set = set == null ? null : set.range(epoch, leafOffset(min), leafOffset(max));

      return set == null
              ? new IntSet(leafSize)
              : new IntSet(leafSize, log2LeafSize, Nodes.Empty.EMPTY.assoc(mapKey(min), epoch, null, set));
    }

    INode mapPrime = map.range(mapKey(min), mapKey(max));
    mapPrime = mapPrime == null
            ? Nodes.Empty.EMPTY
            : mapPrime
            .update(mapKey(min), epoch,
                    new AFn() {
                      public Object invoke(Object v) {
                        return v != null ? ((ISet) v).range(epoch, leafOffset(min), leafSize) : null;
                      }
                    })
            .update(mapKey(max), epoch,
                    new AFn() {
                      public Object invoke(Object v) {
                        return v != null ? ((ISet)v).range(epoch, 0, leafOffset(max)) : null;
                      }
                    });

    return new IntSet(leafSize, log2LeafSize, mapPrime);
  }

  public Iterator elements(final long offset, final boolean reverse) {
    final Iterator it = map.iterator(INode.IterationType.ENTRIES, reverse);
    return new Iterator() {

      private Iterator parentIterator = it;
      private Iterator iterator = null;

      private void tryAdvance() {
        while ((iterator == null || !iterator.hasNext()) && parentIterator.hasNext()) {
          MapEntry entry = (MapEntry) parentIterator.next();
          ISet set = (ISet) entry.val();
          long fullOffset = offset + ((Long)entry.key()) << log2LeafSize;
          iterator = set == null ? null : set.elements(fullOffset, reverse);
        }
      }

      public boolean hasNext() {
        tryAdvance();
        return iterator == null ? false : iterator.hasNext();
      }

      public Object next() {
        tryAdvance();
        return iterator.next();
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public long count() {
    if (count >= 0) {
      return count;
    }

    long cnt = 0;
    Iterator i =  map.iterator(INode.IterationType.VALS, false);
    while (i.hasNext()) {
      ISet s = (ISet) i.next();
      if (s != null) cnt += s.count();
    }
    return cnt;
  }

  public BitSet toBitSet() {
    throw new UnsupportedOperationException();
  }

  public ISet intersection(final long epoch, ISet sv) {
    IntSet s = (IntSet) sv;
    Iterator i1 = map.iterator(INode.IterationType.ENTRIES, false);
    Iterator i2 = s.map.iterator(INode.IterationType.ENTRIES, false);

    // one is empty, so is the intersection
    if (!i1.hasNext() || !i2.hasNext()) {
      return new IntSet(leafSize);
    }

    INode node = Nodes.Empty.EMPTY;

    MapEntry e1 = (MapEntry) i1.next();
    MapEntry e2 = (MapEntry) i2.next();
    while (true) {
      long k1 = (Long) e1.key();
      long k2 = (Long) e2.key();
      if (k1 == k2 && e1.val() != null && e2.val() != null) {
        node = node.assoc(k1, epoch, null, ((ISet)e1.val()).intersection(epoch, (ISet)e2.val()));
        if (!i1.hasNext() || !i2.hasNext()) break;
        e1 = (MapEntry) i1.next();
        e2 = (MapEntry) i2.next();
      } else if (k1 < k2) {
        if (!i1.hasNext()) break;
        e1 = (MapEntry) i1.next();
      } else {
        if (!i2.hasNext()) break;
        e2 = (MapEntry) i2.next();
      }
    }

    return new IntSet(leafSize, log2LeafSize, node);
  }

  public ISet union(final long epoch, ISet sv) {
    IntSet s = (IntSet) sv;
    if (s.leafSize != leafSize) {
      throw new IllegalArgumentException("Cannot merge int-sets of different density.");
    }
    return new IntSet(leafSize, log2LeafSize,
            map.merge(s.map, epoch,
                    new AFn() {
                      public Object invoke(Object a, Object b) {
                        if (a == null) return b;
                        if (b == null) return a;
                        return ((ISet) a).union(epoch, (ISet) b);
                      }
                    }));
  }

  public ISet difference(final long epoch, ISet sv) {
    IntSet s = (IntSet) sv;
    Iterator i1 = map.iterator(INode.IterationType.ENTRIES, false);
    Iterator i2 = s.map.iterator(INode.IterationType.ENTRIES, false);

    if (!i1.hasNext() || !i2.hasNext()) {
      return this;
    }

    INode node = Nodes.Empty.EMPTY;

    MapEntry e1 = (MapEntry) i1.next();
    MapEntry e2 = (MapEntry) i2.next();
    while (true) {
      long k1 = (Long) e1.key();
      long k2 = (Long) e2.key();

      if (k1 == k2 && e1.val() != null && e2.val() != null) {
        node = node.assoc(k1, epoch, null, ((ISet)e1.val()).difference(epoch, (ISet) e2.val()));
        if (!i1.hasNext() || !i2.hasNext()) break;
        e1 = (MapEntry) i1.next();
        e2 = (MapEntry) i2.next();
      } else if (k1 <= k2 && e1.val() != null) {
        node = node.assoc(k1, epoch, null, e1.val());
        if (!i1.hasNext()) break;
        e1 = (MapEntry) i1.next();
      } else {
        if (!i2.hasNext()) {
          node = node.assoc(k1, epoch, null, e1.val());
          break;
        }
        e2 = (MapEntry) i2.next();
      }
    }

    while (i1.hasNext()) {
      e1 = (MapEntry) i1.next();
      node = node.assoc((Long)e1.key(), epoch, null, e1.val());
    }

    return new IntSet(leafSize, log2LeafSize, node);
  }

}
