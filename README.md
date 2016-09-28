```clj
[org.clojure/data.int-map "0.2.4"]
```

This library has special implementations of immutable maps and sets, optimized for integer keys.  They are both faster in both updates and lookups than normal Clojure data structures, but also more memory efficient, sometimes significantly.

## Maps

```clj
> (require '[clojure.data.int-map :as i])
nil
> (i/int-map)
{}
```

These maps support transient/persistent semantics, and also provide special `merge`, `merge-with`, `update`, and `update!` methods that provide significantly faster performance than their normal Clojure counterparts.  The elements must be in the range `[Long/MIN_VALUE, Long/MAX_VALUE]`.  They can be used to represent normal maps which have integral keys, or sparse vectors.

The fact that int-maps are mergeable means that they can be used very effectively with Clojure's [reducer](http://clojure.com/blog/2012/05/08/reducers-a-library-and-model-for-collection-processing.html) mechanism.  For instance, consider populating a data structure.  Typically, we'd use `into`:

```clj
> (into {} [[1 2] [3 4]])
{1 2, 3 4}
```

Under the covers, `into` looks something like this:

```clj
> (persistent!
    (reduce conj!
      (transient {})
      [[1 2] [3 4]]))
```

This makes use of Clojure's transients, but is still inherently sequential.  This is because merging together standard Clojure maps is an O(N) operation, so any parallel work would still result in a linear walk of the map's entries.  However, int-maps merges are typically much faster, which means we can build sub-maps on parallel threads, and then cheaply merge them together.  We can use the `fold` method in `clojure.core.reducers` to easily express this:

```clj
> (require '[clojure.core.reducers :as r])
nil
> (r/fold i/merge conj entries)
...
```

If `entries` is a data structure that `fold` can split, such as a vector or hash-map, the performance benefits of this are significant.  Consider this table, which gives the times on a four-core system for populating a map with a million entries, with all values in milliseconds:

| | unsorted entries | sorted entries |
|----|------------------|-------------|
| `(into {} ...)` | 630 | 500 |
| `(into (sorted-map) ...)` | 2035 | 1080 |
| `(into (i/int-map) ...)` | 529 | 187 |
| `(fold i/merge conj ...)` | 273 | 53 |

As we can see, the int-map implementation is faster in all cases, and an entire order of magnitdue faster when using `fold` on ordered entries.

## Sets

```clj
> (require '[clojure.data.int-map :as i])
nil
> (i/int-set)
#{}
> (i/dense-int-set)
#{}
```

There are special `union`, `intersection` and `difference` operators for these sets, which are significantly faster than those in `clojure.set`.  The elements must be in the range `[Long/MIN_VALUE, Long/MAX_VALUE]`.

`dense-int-set` behaves the same as `int-set`, the difference is only in their memory efficiency.  Consider a case where we create a set of all numbers between one and one million:

```clj
(def s (range 1e6))

(into #{} s)              ; ~100mb
(into (int-set) s)        ; ~1mb
(into (dense-int-set) s)  ; ~150kb
```

Both of these are significantly smaller than the standard set, but the dense int-set is almost an order of magnitude smaller than the normal int-set.  This is because the dense int-set allocates larger contiguous chunks, which is great if the numbers are densely clustered.  However, if the numbers are sparse:

```clj
(def s (map (partial * 1e6) (range 1e6)))

(into #{} s)               ; ~100mb
(into (int-set) s)         ; ~130mb
(into (dense-int-set) s)   ; ~670mb
```

In this case, the dense int-set is much less efficient than the standard set, while the normal int-set is equivalently large.  So as a rule of thumb, use `dense-int-set` where the elements are densely clustered (each element has multiple elements within +/- 1000), and `int-set` for everything else.

## Developer information

data.int-map is being developed as a Clojure Contrib project, see the
[What is Clojure Contrib](http://dev.clojure.org/pages/viewpage.action?pageId=5767464)
page for details. Patches will only be accepted from developers who
have signed the Clojure Contributor Agreement.

* [GitHub project](https://github.com/clojure/data.int-map)

* [Bug Tracker](http://dev.clojure.org/jira/browse/DIMAP)

* [Continuous Integration](http://build.clojure.org/job/data.int-map/)

* [Compatibility Test Matrix](http://build.clojure.org/job/data.int-map-test-matrix/)

## License

Copyright © 2015 Zach Tellman, Rich Hickey and contributors

Distributed under the Eclipse Public License, the same as Clojure.
