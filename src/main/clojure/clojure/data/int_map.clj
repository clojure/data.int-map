(ns
  ^{:doc "A straightforward port of Okasaki and Gill's \"Fast Mergeable Integer Maps`\",
          which can be found at http://ittc.ku.edu/~andygill/papers/IntMap98.pdf"}
  clojure.data.int-map
  (:refer-clojure
    :exclude [merge merge-with update])
  (:require
    [clojure.core.reducers :as r])
  (:import
    [java.util
     BitSet]
    [clojure.data.int_map
     INode
     Nodes$Empty]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

;;;

(defn ^:private default-merge [_ x]
  x)

(definterface IRadix
  (mergeWith [b f])
  (update [k f]))

(defmacro ^:private compile-if [test then else]
  (if (eval test)
    then
    else))

(declare ->transient-int-map)

(deftype PersistentIntMap
  [^INode root
   ^long epoch
   meta]

  IRadix
  (mergeWith [_ b f]
    (let [^PersistentIntMap b b
          epoch' (inc (Math/max (.epoch b) epoch))]
      (PersistentIntMap.
        (.merge root (.root b) epoch' f)
        epoch'
        meta)))

  (update [_ k f]
    (let [epoch' (inc epoch)
          root' (.update root k epoch' f)]
      (PersistentIntMap. root' epoch' meta)))

  clojure.lang.IObj
  (meta [_] meta)
  (withMeta [_ m] (PersistentIntMap. root epoch m))

  clojure.lang.MapEquivalence

  clojure.lang.Counted
  (count [this]
    (.count root))

  clojure.lang.IPersistentCollection

  (equiv [this x]
    (and (map? x) (= x (into {} this))))

  (cons [this o]
    (if (map? o)
      (reduce #(apply assoc %1 %2) this o)
      (.assoc this (nth o 0) (nth o 1))))

  clojure.lang.Seqable
  (seq [this]
    (let [acc (java.util.ArrayList.)]
      (.entries root acc)
      (seq acc)))

  r/CollFold

  (coll-fold [this n combinef reducef]
    (#'r/fjinvoke #(try (.fold root n combinef reducef #'r/fjtask #'r/fjfork #'r/fjjoin) (catch Throwable e (.printStackTrace e)))))

  clojure.core.protocols.CollReduce

  (coll-reduce
    [this f]
    (let [x (.reduce root f (f))]
      (if (reduced? x)
        @x
        x)))

  (coll-reduce
    [this f val]
    (let [x (.reduce root f val)]
      (if (reduced? x)
        @x
        x)))

  clojure.core.protocols.IKVReduce
  (kv-reduce
    [this f val]
    (let [x (.kvreduce root f val)]
      (if (reduced? x)
        @x
        x)))

  Object
  (hashCode [this]
    (reduce
      (fn [acc [k v]]
        (unchecked-add acc (bit-xor (.hashCode k) (.hashCode v))))
      0
      (seq this)))

  clojure.lang.IHashEq
  (hasheq [this]
    (compile-if (resolve 'clojure.core/hash-unordered-coll)
      (hash-unordered-coll this)
      (.hashCode this)))

  (equals [this x]
    (or (identical? this x)
      (and
        (map? x)
        (= x (into {} this)))))

  (toString [this]
    (str (into {} this)))

  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k default]
    (.get root (long k) default))

  clojure.lang.Associative
  (containsKey [this k]
    (not (identical? ::not-found (.valAt this k ::not-found))))

  (entryAt [this k]
    (let [v (.valAt this k ::not-found)]
      (when (not= v ::not-found)
        (clojure.lang.MapEntry. k v))))

  (assoc [this k v]
    (let [k (long k)
          epoch' (inc epoch)]
      (PersistentIntMap.
        (.assoc root (long k) epoch' default-merge v)
        epoch'
        meta)))

  (empty [this]
    (PersistentIntMap. (Nodes$Empty.) 0 nil))

  clojure.lang.IEditableCollection
  (asTransient [this]
    (->transient-int-map root (inc epoch) meta))

  java.util.Map
  (get [this k]
    (.valAt this k))
  (isEmpty [this]
    (empty? (seq this)))
  (size [this]
    (count this))
  (keySet [_]
    (->> (.entries root [])
      (map key)
      set))
  (put [_ _ _]
    (throw (UnsupportedOperationException.)))
  (putAll [_ _]
    (throw (UnsupportedOperationException.)))
  (clear [_]
    (throw (UnsupportedOperationException.)))
  (remove [_ _]
    (throw (UnsupportedOperationException.)))
  (values [this]
    (->> this seq (map second)))
  (entrySet [this]
    (->> this seq set))
  (iterator [this]
    (clojure.lang.SeqIterator. (seq this)))

  clojure.lang.IPersistentMap
  (assocEx [this k v]
    (if (contains? this k)
      (throw (Exception. "Key or value already present"))
      (assoc this k v)))
  (without [this k]
    (let [k (long k)
          epoch' (inc epoch)]
      (PersistentIntMap.
        (.dissoc root k epoch')
        epoch'
        meta)))

  clojure.lang.IFn

  (invoke [this k]
    (.valAt this k))

  (invoke [this k default]
    (.valAt this k default)))

(deftype TransientIntMap
  [^INode root
   ^long epoch
   meta]

  IRadix
  (mergeWith [this b f]
    (throw (IllegalArgumentException. "Cannot call `merge-with` on transient int-map.")))

  (update [this k f]
    (let [root' (.update root k epoch f)]
      (if (identical? root root')
        this
        (TransientIntMap. root' epoch meta))))

  clojure.lang.IObj
  (meta [_] meta)
  (withMeta [_ m] (TransientIntMap. root (inc epoch) meta))

  clojure.lang.Counted
  (count [this]
    (.count root))

  clojure.lang.MapEquivalence

  (equiv [this x]
    (and (map? x) (= x (into {} this))))

  clojure.lang.Seqable
  (seq [this]
    (let [acc (java.util.ArrayList.)]
      (.entries root acc)
      (seq acc)))

  Object
  (hashCode [this]
    (reduce
      (fn [acc [k v]]
        (unchecked-add acc (bit-xor (hash k) (hash v))))
      0
      (seq this)))

  (equals [this x]
    (or (identical? this x)
      (and
        (map? x)
        (= x (into {} this)))))

  (toString [this]
    (str (into {} this)))

  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k default]
    (.get root k default))

  clojure.lang.Associative
  (containsKey [this k]
    (not (identical? ::not-found (.valAt this k ::not-found))))

  (entryAt [this k]
    (let [v (.valAt this k ::not-found)]
      (when (not= v ::not-found)
        (clojure.lang.MapEntry. k v))))

  clojure.lang.ITransientMap

  (assoc [this k v]
    (let [k (long k)
          root' (.assoc root (long k) epoch default-merge v)]
      (if (identical? root' root)
        this
        (TransientIntMap. root' epoch meta))))

  (conj [this o]
    (if (map? o)
      (reduce #(apply assoc! %1 %2) this o)
      (.assoc this (nth o 0) (nth o 1))))

  (persistent [_]
    (PersistentIntMap. root (inc epoch) meta))

  (without [this k]
    (let [root' (.dissoc root (long k) epoch)]
      (if (identical? root' root)
        this
        (TransientIntMap. root' epoch meta))))

  clojure.lang.IFn

  (invoke [this k]
    (.valAt this k))

  (invoke [this k default]
    (.valAt this k default)))

(defn- ->transient-int-map [root ^long epoch meta]
  (TransientIntMap. root epoch meta))

;;;

(defn int-map
  "Creates an integer map that can only have non-negative integers as keys."
  ([]
     (PersistentIntMap. (Nodes$Empty.) 0 nil))
  ([a b]
     (assoc (int-map) a b))
  ([a b & rest]
     (apply assoc (int-map) a b rest)))

(defn merge-with
  "Merges together two int-maps, using `f` to resolve value conflicts."
  ([f]
     (int-map))
  ([f a b]
     (let [a' (if (instance? TransientIntMap a)
                (persistent! a)
                a)
           b'  (if (instance? TransientIntMap b)
                 (persistent! b)
                 b)]
       (.mergeWith ^IRadix a' b' f)))
  ([f a b & rest]
     (reduce #(merge-with f %1 %2) (list* a b rest))))

(defn merge
  "Merges together two int-maps, giving precedence to values from the right-most map."
  ([]
     (int-map))
  ([a b]
     (merge-with (fn [_ b] b) a b))
  ([a b & rest]
     (apply merge-with (fn [_ b] b) a b rest)))

(defn update
  "Updates the value associated with the given key.  If no such key exist, `f` is invoked
   with `nil`."
  ([m k f]
     (.update ^PersistentIntMap m k f))
  ([m k f & args]
     (update m k #(apply f % args))))

(defn update!
  "A transient variant of `update`."
  ([m k f]
     (.update ^TransientIntMap m k f))
  ([m k f & args]
     (update! m k #(apply f % args))))

;;;

(definline ^:private >> [x n]
  `(bit-shift-right ~x ~n))

(definline ^:private << [x n]
  `(bit-shift-left ~x ~n))

(definline ^:private >>> [x n]
  `(unsigned-bit-shift-right ~x ~n))

(deftype Chunk
  [^int generation
   ^BitSet bitset])

(defn- ^Chunk bitset-chunk [^long generation log2-chunk-size]
  (Chunk. generation (BitSet. (<< 1 log2-chunk-size))))

(defn- bit-seq [^java.util.BitSet bitset ^long offset]
  (let [cnt (long (.cardinality bitset))
        ^longs ary (long-array cnt)]
    (loop [ary-idx 0, set-idx 0]
      (when (< ary-idx cnt)
        (let [set-idx (.nextSetBit bitset set-idx)]
          (aset ary ary-idx (+ offset set-idx))
          (recur (inc ary-idx) (inc set-idx)))))
    (seq ary)))

(declare bitset ->persistent-int-set ->transient-int-set)

(defmacro ^:private assoc-bitset [x & {:as fields}]
  (let [type (-> &env ^clojure.lang.Compiler$LocalBinding (get x) .getJavaClass)
        field-names [:log2-chunk-size :generation :cnt :m :meta]]
    `(new ~type
       ~@(map
           (fn [field-name]
             (get fields field-name
               `(~(symbol (str "." (name field-name))) ~x)))
           field-names))))

(definline ^:private chunk-idx [n bit-shift]
  `(>> ~n ~bit-shift))

(definline ^:private idx-within-chunk [n bit-shift]
  `(bit-and ~n (-> 1 (<< ~bit-shift) dec)))

(definline ^:private dec-cnt [cnt]
  `(let [cnt# (unchecked-long ~cnt)]
     (if (== cnt# -1)
       cnt#
       (dec cnt#))))

(definline ^:private inc-cnt [cnt]
  `(let [cnt# (unchecked-long ~cnt)]
     (if (== cnt# -1)
       cnt#
       (inc cnt#))))

(defmacro ^:private compile-if [test then else]
  (if (eval test)
    then
    else))

(deftype PersistentIntSet
  [^byte log2-chunk-size
   ^int generation
   ^:volatile-mutable ^int cnt
   ^PersistentIntMap m
   meta]

  java.lang.Object
  (hashCode [this]
    (if (zero? cnt)
      0
      (->> this
        (map #(bit-xor (long %) (>>> (long %) 32)))
        (reduce #(+ (long %1) (long %2))))))
  (equals [this x] (.equiv this x))

  clojure.lang.IHashEq
  (hasheq [this]
    (compile-if (resolve 'clojure.core/hash-unordered-coll)
      (hash-unordered-coll this)
      (.hashCode this)))

  java.util.Set
  (size [this] (count this))
  (isEmpty [this] (zero? (count this)))
  (iterator [this] (clojure.lang.SeqIterator. (seq this)))
  (containsAll [this s] (every? #(contains? this %) s))

  clojure.lang.IObj
  (meta [_] meta)
  (withMeta [this meta] (assoc-bitset this :meta meta))

  clojure.lang.IEditableCollection
  (asTransient [this] (->transient-int-set this cnt))

  clojure.lang.Seqable
  (seq [_]
    (when-not (zero? cnt)
      (mapcat
        (fn [[slot ^Chunk v]]
          (bit-seq (.bitset v) (<< (long slot) log2-chunk-size)))
        m)))

  clojure.lang.IFn
  (invoke [this idx]
    (when (contains? this idx)
      idx))

  clojure.lang.IPersistentSet
  (equiv [this x]
    (and
      (set? x)
      (= (count this) (count x))
      (every?
        #(contains? x %)
        (seq this))))
  (count [_]
    (when (== cnt -1)
      (set! cnt (->> m
                  vals
                  (map #(.cardinality ^BitSet (.bitset ^Chunk %)))
                  (reduce +)
                  int)))
    cnt)
  (empty [_]
    (PersistentIntSet. log2-chunk-size 0 0 (int-map) nil))
  (contains [_ n]
    (let [n (long n)
          slot (chunk-idx n log2-chunk-size)]
      (if-let [^Chunk chunk (.valAt m slot nil)]
        (let [idx (idx-within-chunk n log2-chunk-size)]
          (.get ^BitSet (.bitset chunk) idx))
        false)))
  (disjoin [this n]
    (let [n (long n)
          slot (chunk-idx n log2-chunk-size)]
      (if-let [^Chunk chunk (.valAt m slot nil)]
        (let [idx (idx-within-chunk n log2-chunk-size)]
          (if (.get ^BitSet (.bitset chunk) idx)
            (assoc-bitset this
              :cnt (dec-cnt cnt)
              :m (assoc m slot
                   (Chunk. generation
                     (doto ^BitSet (.clone ^BitSet (.bitset chunk))
                       (.set idx false)))))
            this))
        this)))
  (cons [this n]
    (let [n (long n)
          slot (chunk-idx n log2-chunk-size)
          idx (idx-within-chunk n log2-chunk-size)]
      (if-let [^Chunk chunk (get m slot)]
        (if-not (.get ^BitSet (.bitset chunk) idx)
          (assoc-bitset this
            :cnt (inc-cnt cnt)
            :m (assoc m slot
                 (Chunk. generation
                   (doto ^BitSet (.clone ^BitSet (.bitset chunk))
                     (.set idx true)))))
          this)
        (assoc-bitset this
          :cnt (inc-cnt cnt)
          :m (let [^Chunk chunk (bitset-chunk generation log2-chunk-size)]
               (.set ^BitSet (.bitset chunk) idx true)
               (assoc m slot chunk)))))))

(deftype TransientIntSet
  [^byte log2-chunk-size
   ^int generation
   ^:volatile-mutable ^int cnt
   ^TransientIntMap m
   meta]

  clojure.lang.IObj
  (meta [_] meta)
  (withMeta [this meta] (assoc-bitset this :meta meta))

  clojure.lang.ITransientSet
  (count [_]
    (when (== cnt -1)
      (set! cnt (->> m
                  vals
                  (map #(.cardinality ^BitSet (.bitset ^Chunk %)))
                  (reduce +)
                  int)))
    cnt)
  (persistent [this] (->persistent-int-set this cnt))
  (contains [_ n]
    (let [n (long n)
          slot (chunk-idx n log2-chunk-size)]
      (if-let [^Chunk chunk (.valAt m slot nil)]
        (let [idx (idx-within-chunk n log2-chunk-size)]
          (.get ^BitSet (.bitset chunk) idx))
        false)))
  (disjoin [this n]
    (let [n (long n)
          slot (chunk-idx n log2-chunk-size)]
      (if-let [^Chunk chunk (.valAt m slot nil)]
        (let [idx (idx-within-chunk n log2-chunk-size)]
          (if (.get ^BitSet (.bitset chunk) idx)
            (if (== (.generation chunk) generation)
              (do
                (.set ^BitSet (.bitset chunk) idx false)
                (assoc-bitset this :cnt (dec-cnt cnt)))
              (assoc-bitset this
                :cnt (dec-cnt cnt)
                :m (let [^BitSet bitset (.clone ^BitSet (.bitset chunk))]
                     (.set bitset idx false)
                     (assoc! m slot (Chunk. generation bitset)))))
            this))
        this)))
  (conj [this n]
    (let [n (long n)
          slot (chunk-idx n log2-chunk-size)
          idx (idx-within-chunk n log2-chunk-size)]
      (if-let [^Chunk chunk (.valAt m slot nil)]
        (if-not (.get ^BitSet (.bitset chunk) idx)
          (if (== (.generation chunk) generation)
            (do
              (.set ^BitSet (.bitset chunk) idx true)
              (assoc-bitset this :cnt (inc-cnt cnt)))
            (assoc-bitset this
              :cnt (inc-cnt cnt)
              :m (let [^BitSet bitset (.clone ^BitSet (.bitset chunk))]
                   (.set bitset idx true)
                   (assoc! m slot (Chunk. generation bitset)))))
          this)
        (assoc-bitset this
          :cnt (inc-cnt cnt)
          :m (let [^Chunk chunk (bitset-chunk generation log2-chunk-size)]
               (.set ^BitSet (.bitset chunk) idx true)
               (assoc! m slot chunk)))))))

(defn- ->persistent-int-set [^TransientIntSet bitset ^long cnt]
  (PersistentIntSet.
    (.log2-chunk-size bitset)
    (.generation bitset)
    cnt
    (persistent! (.m bitset))
    (.meta bitset)))

(defn- ->transient-int-set [^PersistentIntSet bitset ^long cnt]
  (TransientIntSet.
    (.log2-chunk-size bitset)
    (inc (.generation bitset))
    cnt
    (transient (.m bitset))
    (.meta bitset)))

;;;

(defn int-set
  "Creates an immutable set which can only store integral values.  This should be used unless elements are densely
   clustered (each element has multiple elements within +/- 1000)."
  ([]
     ;; 128 bits per chunk
     (PersistentIntSet. 7 0 0 (int-map) nil))
  ([s]
     (into (int-set) s)))

(defn dense-int-set
  "Creates an immutable set which can only store integral values.  This should be used only if elements are densely
   clustered (each element has multiple elements within +/- 1000)."
  ([]
     ;; 4096 bits per chunk
     (PersistentIntSet. 12 0 0 (int-map) nil))
  ([s]
     (into (dense-int-set) s)))

;;;

(defn- merge-bit-op [bit-set-fn keys-fn ^PersistentIntSet a ^PersistentIntSet b]
  (assert (= (.log2-chunk-size a) (.log2-chunk-size b)))
  (let [log2-chunk-size (.log2-chunk-size a)
        generation (inc (long (Math/max (.generation a) (.generation b))))
        m-a (.m a)
        m-b (.m b)
        ks (keys-fn m-a m-b)
        m (persistent!
            (reduce
              (fn [m k]
                (assoc! m k
                  (let [^Chunk a (get m-a k)
                        ^Chunk b (get m-b k)]
                    (if (and a b)
                      (let [^Chunk chunk (Chunk. generation (.clone ^BitSet (.bitset a)))
                            ^BitSet b-a (.bitset chunk)
                            ^BitSet b-b (.bitset b)]
                        (bit-set-fn b-a b-b)
                        chunk)
                      (or a b (throw (IllegalStateException.)))))))
              (transient (int-map))
              ks))]
    (PersistentIntSet.
      log2-chunk-size
      generation
      -1
      m
      nil)))

(defn union
  "Returns the union of two bitsets."
  [^PersistentIntSet a ^PersistentIntSet b]
  (assert (= (.log2-chunk-size a) (.log2-chunk-size b)))
  (let [generation (inc (long (Math/max (.generation a) (.generation b))))]
    (PersistentIntSet.
      (.log2-chunk-size a)
      generation
      -1
      (merge-with
        (fn [^Chunk a ^Chunk b]
          (let [chunk (Chunk. generation (.clone ^BitSet (.bitset a)))]
            (.or ^BitSet (.bitset chunk) (.bitset b))
            chunk))
       (.m a) (.m b))
     nil)))

(defn intersection
  "Returns the intersection of two bitsets."
  [a b]
  (merge-bit-op
    #(.and ^BitSet %1 %2)
    (fn [a b]
      (filter #(contains? b %) (keys a)))
    a
    b))

(defn difference
  "Returns the difference between two bitsets."
  [a b]
  (merge-bit-op
    #(.andNot ^BitSet %1 %2)
    (fn [a b] (keys a))
    a
    b))
