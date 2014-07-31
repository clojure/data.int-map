(ns clojure.data.int-map-test
  (:use
    [clojure.test])
  (:require
    [clojure.set :as set]
    [clojure.core.reducers :as r]
    [clojure.data.int-map :as i]
    [collection-check :as check]
    [criterium.core :as c]
    [simple-check.generators :as gen]
    [simple-check.properties :as prop]
    [simple-check.clojure-test :as ct :refer (defspec)])
  (:import
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

(defspec equivalent-merge 1e3
  (prop/for-all [a int-map-generator, b int-map-generator]
    (= (merge-with + a b) (i/merge-with + a b))))

;;;

(def n (long 1e6))

(def ordered-entries (vec (map vector (range n) (range n))))
(def entries (vec (shuffle ordered-entries)))

(deftest ^:benchmark benchmark-maps

  (println "into {} unordered")
  (c/quick-bench
    (into {} entries))

  (println "into {} ordered")
  (c/quick-bench
    (into {} ordered-entries))

  (println "into (sorted-map) unordered")
  (c/quick-bench
    (into (sorted-map) entries))

  (println "into (sorted-map) ordered")
  (c/quick-bench
    (into (sorted-map) ordered-entries))

  (println "into (int-map) unordered")
  (c/quick-bench
    (into (i/int-map) entries))

  (println "into (int-map) ordered")
  (c/quick-bench
    (into (i/int-map) ordered-entries))

  (println "into (int-map) fold/merge unordered")
  (c/quick-bench
    (r/fold i/merge conj entries))

  (println "into (int-map) fold/merge ordered")
  (c/quick-bench
    (r/fold i/merge conj ordered-entries))

  (let [m (into {} entries)]
    (println "get {}")
    (c/quick-bench
      (get m 1e3)))

  (let [m (into (i/int-map) entries)]
    (println "get (int-map)")
    (c/quick-bench
      (get m 1e3)))

  (let [m (into (sorted-map) entries)]
    (println "get (sorted-map)")
    (c/quick-bench
      (get m 1e3))))

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

(deftest ^:benchmark benchmark-modify-set
  (println "sparse bitset into 1e3")
  (c/quick-bench
    (into (i/int-set) (range 1e3)))
  (println "dense bitset into 1e3")
  (c/quick-bench
    (into (i/dense-int-set) (range 1e3)))
  (println "normal set into 1e3")
  (c/quick-bench
    (into #{} (range 1e3)))
  (println "mutable bitset add 1e3")
  (c/quick-bench
    (let [^BitSet bitset (BitSet. 1e3)]
      (dotimes [idx 1e3]
        (.set bitset idx true)))))

(deftest ^:benchmark benchmark-check-set
  (println "check sparse bitset")
  (let [b (into (i/int-set) (range 1e3))]
    (c/quick-bench
      (contains? b 123)))
  (println "check dense bitset")
  (let [b (into (i/dense-int-set) (range 1e3))]
    (c/quick-bench
      (contains? b 123)))
  (println "check normal set")
  (let [s (into #{} (range 1e3))]
    (c/quick-bench
      (contains? s 123)))
  (println "mutable bitset lookup")
  (let [b (BitSet. 1e3)]
    (c/quick-bench
      (.get b 123))))
