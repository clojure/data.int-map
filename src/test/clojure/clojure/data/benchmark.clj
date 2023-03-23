(ns clojure.data.benchmark
  (:use
    [clojure.test])
  (:require
    [clojure.core.reducers :as r]
    [clojure.data.int-map :as i]
    [criterium.core :as c])
  (:import
    [java.util
     BitSet]
    [clojure.data.int_map
     PersistentIntMap]))

(def n (long 1e6))

(def ordered-entries (vec (map vector (range n) (range n))))
(def entries (vec (shuffle ordered-entries)))

(deftest ^:benchmark benchmark-maps

  (println "\ninto {} unordered")
  (c/quick-bench
    (into {} entries))

  (println "\ninto {} ordered")
  (c/quick-bench
    (into {} ordered-entries))

  (println "\ninto (sorted-map) unordered")
  (c/quick-bench
    (into (sorted-map) entries))

  (println "\ninto (sorted-map) ordered")
  (c/quick-bench
    (into (sorted-map) ordered-entries))

  (println "\ninto (int-map) unordered")
  (c/quick-bench
    (into (i/int-map) entries))

  (println "\ninto (int-map) ordered")
  (c/quick-bench
    (into (i/int-map) ordered-entries))

  (println "\ninto (int-map) fold/merge unordered")
  (c/quick-bench
    (r/fold i/merge conj entries))

  (println "\ninto (int-map) fold/merge ordered")
  (c/quick-bench
    (r/fold i/merge conj ordered-entries))

  (let [m (into {} entries)
        r (java.util.Random.)]
    (println "\nget {}")
    (c/quick-bench
      (get m 1000)))

  (let [m (into (i/int-map) entries)
        r (java.util.Random.)]
    (println "\nget (int-map)")
    (c/quick-bench
      (get m 1000)))

  (let [m (into (sorted-map) entries)
        r (java.util.Random.)]
    (println "\nget (sorted-map)")
    (c/quick-bench
      (get m 1000)))

  (let [m1 (into (i/int-map) entries)
        m2 (into (i/int-map) entries)
        r (java.util.Random.)]
    (println "\n= (int-map, int-map)")
    (c/quick-bench
     (= m1 m2)))

  (let [m1 (into (i/int-map) entries)
        m2 (into (hash-map) entries)
        r (java.util.Random.)]
    (println "\n= (int-map, hash-map)")
    (c/quick-bench
     (= m1 m2)))

  (let [m1 (into (hash-map) entries)
        m2 (into (hash-map) entries)
        r (java.util.Random.)]
    (println "\n= (hash-map, hash-map)")
    (c/quick-bench
     (= m1 m2))))

;;;

(deftest ^:benchmark benchmark-modify-set
  (println "\nsparse bitset into 1e3")
  (c/quick-bench
    (into (i/int-set) (range 1e3)))
  (println "\ndense bitset into 1e3")
  (c/quick-bench
    (into (i/dense-int-set) (range 1e3)))
  (println "\nnormal set into 1e3")
  (c/quick-bench
    (into #{} (range 1e3)))
  (println "\nmutable bitset add 1e3")
  (c/quick-bench
    (let [^BitSet bitset (BitSet. 1e3)]
      (dotimes [idx 1e3]
        (.set bitset idx true)))))

(deftest ^:benchmark benchmark-check-set
  (println "\ncheck sparse bitset")
  (let [b (into (i/int-set) (range 1e3))]
    (c/quick-bench
      (contains? b 123)))
  (println "\ncheck dense bitset")
  (let [b (into (i/dense-int-set) (range 1e3))]
    (c/quick-bench
      (contains? b 123)))
  (println "\ncheck normal set")
  (let [s (into #{} (range 1e3))]
    (c/quick-bench
      (contains? s 123)))
  (println "\nmutable bitset lookup")
  (let [b (BitSet. 1e3)]
    (c/quick-bench
      (.get b 123))))

(deftest ^:benchmark benchmark-int-map-range
  (println "\ncheck int-map range")
  (let [^PersistentIntMap im (->> (range 0 50000)
                                  (map (fn [x] [x x]))
                                  (into (i/int-map)))]
    (c/quick-bench
      (do
        (.range im -20000 20000)
        (.range im 20000 20100)
        (.range im -10000 -100)
        (.range im 20000 100000)
        (.range im -1000000 100000)))
    ))