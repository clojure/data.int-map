package clojure.data.int_map;

import java.util.BitSet;
import java.util.Collection;
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
