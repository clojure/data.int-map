(ns clojure.data.int-map-test
  (:use
    [clojure.test])
  (:require
    [clojure.java.shell :as sh]
    #_[rhizome.viz :as v]
    [clojure.set :as set]
    [clojure.core.reducers :as r]
    [clojure.data.int-map :as i]
    [collection-check :as check]
    [clojure.string :as str]
    [clojure.test.check.generators :as gen]
    [clojure.test.check.properties :as prop]
    [clojure.test.check.clojure-test :as ct :refer (defspec)])
  (:import
    [clojure.data.int_map
     INode
     Nodes
     Nodes$Leaf
     Nodes$Empty
     Nodes$BinaryBranch
     Nodes$Branch]
    [java.util
     BitSet]))

(set! *warn-on-reflection* false)

;;;

(def map-int
  (->> (gen/tuple gen/int (gen/choose 0 63))
    (gen/fmap (fn [[x y]] (bit-shift-left x y)))))

(deftest test-map-like
  (check/assert-map-like 1e3 (i/int-map) map-int gen/int))

(deftest test-set-like
  (check/assert-set-like 1e3 (i/int-set) map-int))

(def int-map-generator
  (gen/fmap
    (fn [ks]
      (into (i/int-map) ks))
    (gen/list (gen/tuple map-int gen/int))))

(def int-set-generator
  (gen/fmap
    (fn [ks]
      (into (i/int-set) ks))
    (gen/list map-int)))

(defspec equivalent-update 1e3
  (let [f #(if % (inc %) 1)]
    (prop/for-all [m int-map-generator k gen/int]
      (= (i/update m k f)
        (assoc m k (f (get m k)))))))

(defspec equivalent-update! 1e3
  (prop/for-all [ks (gen/list gen/int)]
    (persistent! (reduce #(i/update! %1 %2 (fn [_])) (transient (i/int-map)) ks))))

(defspec equivalent-merge 1e3
  (prop/for-all [a (gen/list (gen/tuple gen/int gen/int))
                 b (gen/list (gen/tuple gen/int gen/int))]
    (let [a (into (i/int-map) a)
          b (into (i/int-map) b)]
      (= (merge-with - a b) (i/merge-with - a b)))))

(defspec equivalent-fold 1e3
  (prop/for-all [m int-map-generator]
    (= (reduce-kv (fn [n _ v] (+ n v)) 0 m)
      (r/fold 8 + (fn [n _ v] (+ n v)) m))))

(defspec equivalent-map-order 1e4
  (prop/for-all [ks (gen/list gen/int)]
    (= (keys (reduce #(assoc %1 %2 nil) (i/int-map) ks))
      (seq (sort (distinct ks))))))

(defspec equivalent-reverse-map-order 1e4
  (prop/for-all [m int-map-generator]
    (= (seq (reverse m)) (rseq m))))

(defspec equivalent-reverse-set-order 1e4
  (prop/for-all [s int-set-generator]
    (= (seq (reverse s)) (rseq s))))

(defspec equivalent-set-order 1e4
  (prop/for-all [ks (gen/list gen/int)]
    (= (seq (reduce #(conj %1 %2) (i/int-set) ks))
      (seq (sort (distinct ks))))))

(defspec equivalent-map-range 1e4
  (prop/for-all [m int-map-generator
                 min gen/int
                 max gen/int]
    (= (i/range m min max)
      (select-keys m (->> m keys (filter #(<= min % max)))))))

(defspec equivalent-set-range 1e4
  (prop/for-all [s int-set-generator
                 min gen/int
                 max gen/int]
    (= (i/range s min max)
      (->> s (filter #(<= min % max)) set))))

(deftest test-contiguous-keys
  (is (== 1e7 (count (persistent! (reduce #(assoc! %1 %2 nil) (transient (i/int-map)) (range 1e7)))))))
;;;

#_(defn view-tree [m]
  (let [r (.root m)]
    (v/view-tree
      #(or (instance? Nodes$BinaryBranch %) (instance? Nodes$Branch %))
      #(if (instance? Nodes$BinaryBranch %)
         [(.a %) (.b %)]
         (remove nil? (.children %)))
      r
      :node->descriptor (fn [n]
                          {:label (cond
                                    (instance? Nodes$Leaf n)
                                    (str (.key n) "," (.value n))

                                    (instance? Nodes$Branch n)
                                    (str (.offset n))

                                    :else
                                    "")}))))

#_(defn view-set [s]
  (let [r (-> s .int-set .map)]
    (v/view-tree
      #(or (instance? Nodes$BinaryBranch %) (instance? Nodes$Branch %))
      #(if (instance? Nodes$BinaryBranch %)
         [(.a %) (.b %)]
         (remove nil? (.children %)))
      r
      :node->descriptor (fn [n]
                          {:label (cond
                                    (instance? Nodes$Leaf n)
                                    (str (.key n) "," (.value n))

                                    (instance? Nodes$Branch n)
                                    (str (.offset n))

                                    :else
                                    "")}))))

(defn get-field [field x]
  (let [field (name field)]
    (-> x
      class
      (.getDeclaredField field)
      (doto (.setAccessible true))
      (.get x))))

(defn tree-hierarchy [m]
  (condp instance? m
    clojure.lang.PersistentHashMap
    (->> m
      (get-field :root)
      tree-hierarchy)

    clojure.lang.PersistentHashMap$BitmapIndexedNode
    (let [ary (get-field :array m)]
      (concat
        (->> ary
          (partition 2)
          (map first)
          (remove nil?))
        (->> ary
          (partition 2)
          (filter #(nil? (first %)))
          (map second)
          (remove nil?)
          (map tree-hierarchy))))

    clojure.lang.PersistentHashMap$ArrayNode
    (->> m
      (get-field :array)
      (remove nil?)
      (map tree-hierarchy))

    clojure.lang.PersistentHashMap$HashCollisionNode
    (->> m
      (get-field :array)
      (partition 2)
      (map first))

    clojure.data.int_map.PersistentIntMap
    (->> m
      (get-field :root)
      tree-hierarchy)

    clojure.data.int_map.Nodes$BinaryBranch
    (->> [(get-field :left m) (get-field :right m)]
      (remove nil?)
      (map tree-hierarchy))

    clojure.data.int_map.Nodes$Branch
    (->> m
      (get-field :children)
      (remove nil?)
      (map tree-hierarchy))

    clojure.data.int_map.Nodes$Leaf
    (get-field :key m)

    m))

(defn singly-linked-nodes [tree]
  (concat
    (->> tree
      (filter #(and (seq? %) (= 1 (count %)))))
    (->> tree
      (filter #(and (seq? %) (not= 1 (count %))))
      (mapcat singly-linked-nodes))))

(defn depths
  ([tree]
   (depths 0 tree))
  ([d tree]
   (concat
     (->> tree
       (remove seq?)
       (map (constantly d)))
     (->> tree
       (filter seq?)
       (mapcat #(depths (inc d) %))))))

(defn mean [s]
  (double (/ (reduce + s) (count s))))

;;;

(defn diff-equals? [set0 set1]
  (prn (i/difference (i/int-set set0)
       (i/int-set set1))

    (set/difference set0
      set1)))

(defn all-set-algebra-operators-equivalent?
  [generator]
  (prop/for-all [a (gen/vector map-int) b (gen/vector map-int)]
    (let [sa (set a)
          sb (set b)
          isa (generator a)
          isb (generator b)]
      (and
        (= (set/difference sa sb) (i/difference isa isb))
        (= (set/difference sb sa) (i/difference isb isa))
        (= (set/union sa sb) (i/union isa isb) (i/union isb isa))
        (= (set/intersection sa sb) (i/intersection isa isb) (i/intersection isb isa))))))

(defspec prop-sparse-all-set-algebra-operators-equivalent 1e5
  (all-set-algebra-operators-equivalent? i/int-set))

(defspec prop-dense-all-set-algebra-operators-equivalent 1e5
  (all-set-algebra-operators-equivalent? i/dense-int-set))
