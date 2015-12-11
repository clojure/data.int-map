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

(def set-int
  (gen/fmap (fn [[x y]] (+ (* 128 x) y)) (gen/tuple gen/int gen/int)))

(deftest test-map-like
  (check/assert-map-like 1e3 (i/int-map) gen/int gen/int))

(deftest test-set-like
  (check/assert-set-like 1e3 (i/int-set) gen/int))

(def int-map-generator
  (gen/fmap
    (fn [ks]
      (into (i/int-map) ks))
    (gen/list (gen/tuple gen/int gen/int))))

(def int-set-generator
  (gen/fmap
    (fn [ks]
      (into (i/int-set) ks))
    (gen/list set-int)))

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

;;;

(defn diff-equals? [set0 set1]
  (prn (i/difference (i/int-set set0)
       (i/int-set set1))

    (set/difference set0
      set1)))

(defn all-set-algebra-operators-equivalent?
  [generator]
  (prop/for-all [a (gen/vector set-int) b (gen/vector set-int)]
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
