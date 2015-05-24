(ns clojure.data.int-map-test
  (:use
    [clojure.test])
  (:require
    [rhizome.viz :as v]
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
     Nodes
     Nodes$Leaf
     Nodes$Branch]
    [java.util
     BitSet]))

;;;

(deftest test-map-like
  (check/assert-map-like 1e3 (i/int-map) gen/pos-int gen/int))

(deftest test-set-like
  (check/assert-set-like 1e3 (i/int-set) gen/int))

(def int-map-generator
  (gen/fmap
    (fn [ks]
      (into (i/int-map) ks))
    (gen/list (gen/tuple gen/pos-int gen/int))))

(defspec equivalent-update 1e3
  (let [f #(if % (inc %) 1)]
    (prop/for-all [m int-map-generator k gen/pos-int]
      (= (i/update m k f)
        (assoc m k (f (get m k)))))))

(defspec equivalent-update! 1e3
  (prop/for-all [ks (gen/list gen/pos-int)]
    (persistent! (reduce #(i/update! %1 %2 (fn [_])) (transient (i/int-map)) ks))))

(defspec equivalent-merge 1e3
  (prop/for-all [a int-map-generator, b int-map-generator]
    (= (merge-with + a b) (i/merge-with + a b))))

(defspec equivalent-fold 1e3
  (prop/for-all [m int-map-generator]
    (= (reduce-kv (fn [n _ v] (+ n v)) 0 m)
      (r/fold 32 + (fn [n _ v] (+ n v)) m))))

;;;

(defn view-tree [m]
  (let [r (.root m)]
    (v/view-tree
      #(instance? Nodes$Branch %)
      #(vector (.left %) (.right %))
      r
      :node->descriptor (fn [n]
                          {:label (if (instance? Nodes$Leaf n)
                                    (str (.key n) "," (.value n))
                                    (str (.prefix n), "," (.mask n)))}))))

;;;

(defn all-set-algebra-operators-equivalent?
  [generator]
  (prop/for-all [a (gen/vector gen/int) b (gen/vector gen/int)]
    (let [sa (set a)
          sb (set b)
          isa (generator a)
          isb (generator b)]
      (and
        (= (set/difference sa sb) (i/difference isa isb))
        (= (set/difference sb sa) (i/difference isb isa))
        (= (set/union sa sb) (i/union isa isb) (i/union isb isa))
        (= (set/intersection sa sb) (i/intersection isa isb) (i/intersection isb isa))))))

(defspec prop-sparse-all-set-algebra-operators-equivalent 1000
  (all-set-algebra-operators-equivalent? i/int-set))

(defspec prop-dense-all-set-algebra-operators-equivalent 1000
  (all-set-algebra-operators-equivalent? i/dense-int-set))
