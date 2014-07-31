(ns clojure.data.int-map.nodes
  (:require
    [clojure.core #_primitive-math :as p]
    [clojure.core.reducers :as r]
    [clojure.core.protocols :as pr]))

(definterface INode
  (^long count [])
  (merge [node epoch f])
  (entries [acc])
  (assoc [^long k ^long epoch f v])
  (dissoc [^long k ^long epoch])
  (update [^long k ^long epoch f])
  (get [^long k default]))

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)

(definline off? [x m]
  `(p/== 0 (p/bit-and ~x ~m)))

(defmacro bit-mask [x m]
  `(p/bit-and (p/bit-or ~x (p/dec ~m)) (p/bit-not ~m)))

(defmacro match-prefix? [k p m]
  `(p/== (bit-mask ~p ~m) (bit-mask ~k ~m)))

(defmacro lowest-bit [x]
  `(p/bit-and ~x (p/bit-not (p/dec ~x))))

(defn highest-bit ^long [^long x ^long m]
  (loop [x (p/bit-and x (p/bit-not (p/dec m)))]
    (let [m (lowest-bit x)]
      (if (p/== x m)
        m
        (recur (p/- x m))))))

(defn branching-bit ^long [^long p0 ^long m0 ^long p1 ^long m1]
  (highest-bit (p/bit-xor p0 p1) (p/max 1 (p/* 2 (p/max m0 m1)))))

(deftype Tuple2 [a b])

(defmacro join [epoch p0 m0 t0 p1 m1 t1]
  `(let [m# (branching-bit ~p0 ~m0 ~p1 ~m1)]
     (if (off? ~p0 m#)
       (branch ~p0 m# ~epoch ~t0 ~t1)
       (branch ~p0 m# ~epoch ~t1 ~t0))))

;;;

(declare leaf branch-)

(defmacro branch* [prefix mask epoch left right]
  `(branch- ~prefix ~mask ~epoch (Tuple2. ~left ~right)))

(defmacro branch [prefix mask epoch left right]
  `(new ~'Branch
     (bit-mask ~prefix ~mask)
     ~mask
     ~epoch
     (p/+
       (.count ~(with-meta left {:tag "INode"}))
       (.count ~(with-meta right {:tag "INode"})))
     ~left
     ~right))

(deftype Empty []

  pr/CollReduce

  (coll-reduce [this f]
    (f))

  (coll-reduce [_ f init]
    init)

  INode
  (update [_ k epoch f]
    (leaf k epoch (f nil)))
  (merge [_ n epoch f]
    n)
  (count [_]
    0)
  (entries [_ acc]
    acc)
  (get [_ _ default]
    default)
  (dissoc [this _ _]
    this)
  (assoc [_ k epoch f v]
    (leaf k epoch v)))

;;;

(deftype Branch
  [^long prefix
   ^long mask
   ^long epoch
   ^long count
   ^INode left
   ^INode right]

  r/CollFold

  (coll-fold [this n combinef reducef]
    (if (p/< count (* 2 n))
      (reduce reducef (combinef) this)
      (#'r/fjinvoke
        (fn []
          (let [rt (#'r/fjfork
                     (r/fjtask
                       #(r/coll-fold right n combinef reducef)))]
            (combinef
              (r/coll-fold left n combinef reducef)
              (#'r/fjjoin rt)))))))

  clojure.core.protocols.CollReduce

  (coll-reduce [this f]
    (pr/coll-reduce this f (f)))

  (coll-reduce [_ f init]
    (if (reduced? init)
      init
      (let [init' (pr/coll-reduce left f init)]
        (if (reduced? init')
          init'
          (pr/coll-reduce right f init')))))

  INode

  (update [this k epoch' f]
    (if (p/<= k prefix)
      (let [left' (.update left k epoch' f)]
        (if (identical? left left')
          this
          (branch prefix mask epoch' left' right)))
      (let [right' (.update right k epoch' f)]
        (if (identical? right right')
          this
          (branch prefix mask epoch' left right')))))

  (merge [this n epoch' f]
    (if (instance? Branch n)

      (let [^Branch n n]
        (cond

          ;; merge subtrees
          (and
            (p/== (.prefix n) prefix)
            (p/== (.mask n) mask))
          (branch* prefix mask epoch'
            (.merge left (.left n) epoch' f)
            (.merge right (.right n) epoch' f))

          ;; we contain the other node
          (and (p/> mask (.mask n)) (match-prefix? (.prefix n) prefix mask))
          (if (off? (.prefix n) mask)
            (branch* prefix mask epoch' (.merge left n epoch' f) right)
            (branch* prefix mask epoch' left (.merge right n epoch' f)))

          ;; the other node contains us
          (and (p/< mask (.mask n)) (match-prefix? prefix (.prefix n) (.mask n)))
          (if (off? prefix (.mask n))
            (branch* (.prefix n) (.mask n) epoch' (.merge this (.left n) epoch' f) (.right n))
            (branch* (.prefix n) (.mask n) epoch' (.left n) (.merge this (.right n) epoch' f)))

          :else
          (join epoch'
            prefix mask this
            (.prefix n) (.mask n) n)))

      ;; not a branch, let the other node's logic handle it
      (.merge ^INode n this epoch' (fn [x y] (f y x)))))

  (count [_]
    count)

  (entries [_ acc]
    (let [acc (if left
                (.entries left acc)
                acc)
          acc (if right
                (.entries right acc)
                acc)]
      acc))

  (get [this k default]
    (if (p/<= k prefix)
      (.get left k default)
      (.get right k default)))

  (assoc [this k epoch' f v]
    (if (match-prefix? k prefix mask)

      (if (off? k mask)
        (let [left' (.assoc left k epoch' f v)]
          (if (identical? left left')
            this
            (branch prefix mask epoch' left' right)))
        (let [right' (.assoc right k epoch' f v)]
          (if (identical? right right')
            this
            (branch prefix mask epoch' left right'))))

      (join epoch'
        k mask (leaf k epoch v)
        prefix mask this)))

  (dissoc [this k epoch']
    (if (p/<= k prefix)
      (let [left' (.dissoc left k epoch')]
        (if (identical? left left')
          this
          (branch prefix mask epoch' left' right)))
      (let [right' (.dissoc right k epoch')]
        (if (identical? right right')
          this
          (branch prefix mask epoch' left right'))))))

(defn branch- [^long prefix ^long mask ^long epoch ^Tuple2 left+right]
  (let [^INode left (.a left+right)
        ^INode right (.b left+right)
        no-left? (instance? Empty left)
        no-right? (instance? Empty right)]
    (cond
      (and no-left? no-right?)
      (Empty.)

      no-left?
      right

      no-right?
      left

      :else
      (Branch.
        (bit-mask prefix mask)
        mask
        epoch
        (p/+ (.count left) (.count right))
        left
        right))))

;;;

(deftype Leaf
  [^long key
   ^long epoch
   ^:volatile-mutable value]

  pr/CollReduce

  (coll-reduce [this f]
    (f (f) (clojure.lang.MapEntry. key value)))

  (coll-reduce [_ f init]
    (if (reduced? init)
      init
      (f init (clojure.lang.MapEntry. key value))))

  INode
  (update [this k epoch' f]
    (if (p/== k key)
      (let [value' (f value)]
        (if (p/== epoch epoch')
          (do (set! value value') this)
          (Leaf. key epoch' value')))
      (.assoc this k epoch' nil (f nil))))
  (merge [this n epoch' f]
    (.assoc ^INode n key epoch' f value))
  (count [_]
    1)
  (entries [_ acc]
    (.add ^java.util.ArrayList acc (clojure.lang.MapEntry. key value))
    acc)
  (get [_ k default]
    (if (p/== key k)
      value
      default))
  (dissoc [this k epoch']
    (if (p/== key k)
      (Empty.)
      this))
  (assoc [this k epoch' f v]
    (if (p/== k key)
      (let [value' (f value v)]
        (if (p/== epoch epoch')
          (do
            (set! value value')
            this)
          (Leaf. k epoch' value')))
      (join epoch'
        k 0 (Leaf. k epoch' v)
        key 0 this))))

(defn leaf [^long key ^long epoch value]
  (Leaf. key epoch value))
