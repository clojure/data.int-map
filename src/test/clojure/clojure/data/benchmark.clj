(ns clojure.data.benchmark
  (:use
    [clojure.test])
  (:require
    [clojure.core.reducers :as r]
    [clojure.data.int-map :as i]
    [criterium.core :as c])
  (:import
    [java.util
     BitSet]))

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
