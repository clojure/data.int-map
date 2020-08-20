{:namespaces
 ({:doc
   "An adaptation of Okasaki and Gill's \"Fast Mergeable Integer Maps`\",\nwhich can be found at http://ittc.ku.edu/~andygill/papers/IntMap98.pdf",
   :name "clojure.data.int-map",
   :wiki-url "https://clojure.github.io/data.int-map/index.html",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj"}),
 :vars
 ({:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "->PersistentIntMap",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L43",
   :line 43,
   :var-type "function",
   :arglists ([root epoch meta]),
   :doc
   "Positional factory function for class clojure.data.int_map.PersistentIntMap.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/->PersistentIntMap"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "->PersistentIntSet",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L448",
   :line 448,
   :var-type "function",
   :arglists ([int-set epoch meta]),
   :doc
   "Positional factory function for class clojure.data.int_map.PersistentIntSet.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/->PersistentIntSet"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "->TransientIntMap",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L291",
   :line 291,
   :var-type "function",
   :arglists ([root epoch meta]),
   :doc
   "Positional factory function for class clojure.data.int_map.TransientIntMap.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/->TransientIntMap"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "->TransientIntSet",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L527",
   :line 527,
   :var-type "function",
   :arglists ([int-set epoch meta]),
   :doc
   "Positional factory function for class clojure.data.int_map.TransientIntSet.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/->TransientIntSet"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "dense-int-set",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L581",
   :line 581,
   :var-type "function",
   :arglists ([] [s]),
   :doc
   "Creates an immutable set which can only store integral values.  This should be used only if elements are densely\nclustered (each element has multiple elements within +/- 1000).",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/dense-int-set"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "difference",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L607",
   :line 607,
   :var-type "function",
   :arglists ([a b]),
   :doc "Returns the difference between two bitsets.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/difference"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "int-map",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L391",
   :line 391,
   :var-type "function",
   :arglists ([] [a b] [a b & rest]),
   :doc
   "Creates an integer map that can only have non-negative integers as keys.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/int-map"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "int-set",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L573",
   :line 573,
   :var-type "function",
   :arglists ([] [s]),
   :doc
   "Creates an immutable set which can only store integral values.  This should be used unless elements are densely\nclustered (each element has multiple elements within +/- 1000).",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/int-set"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "intersection",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L598",
   :line 598,
   :var-type "function",
   :arglists ([a b]),
   :doc "Returns the intersection of two bitsets.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/intersection"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "merge",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L415",
   :line 415,
   :var-type "function",
   :arglists ([] [a b] [a b & rest]),
   :doc
   "Merges together two int-maps, giving precedence to values from the right-most map.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/merge"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "merge-with",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L400",
   :line 400,
   :var-type "function",
   :arglists ([f] [f a b] [f a b & rest]),
   :doc
   "Merges together two int-maps, using `f` to resolve value conflicts.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/merge-with"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "range",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L439",
   :line 439,
   :var-type "function",
   :arglists ([x min max]),
   :doc
   "Returns a map or set representing all elements within [min, max], inclusive.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/range"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "union",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L589",
   :line 589,
   :var-type "function",
   :arglists ([a b]),
   :doc "Returns the union of two bitsets.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/union"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "update",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L424",
   :line 424,
   :var-type "function",
   :arglists ([m k f] [m k f & args]),
   :doc
   "Updates the value associated with the given key.  If no such key exist, `f` is invoked\nwith `nil`.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/update"}
  {:raw-source-url
   "https://github.com/clojure/data.int-map/raw/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj",
   :name "update!",
   :file "src/main/clojure/clojure/data/int_map.clj",
   :source-url
   "https://github.com/clojure/data.int-map/blob/2d5d0e540dc2c3fa021ba95a593539c26e482d3d/src/main/clojure/clojure/data/int_map.clj#L432",
   :line 432,
   :var-type "function",
   :arglists ([m k f] [m k f & args]),
   :doc "A transient variant of `update`.",
   :namespace "clojure.data.int-map",
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/update!"}
  {:name "PersistentIntMap",
   :var-type "type",
   :namespace "clojure.data.int-map",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/PersistentIntMap",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "PersistentIntSet",
   :var-type "type",
   :namespace "clojure.data.int-map",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/PersistentIntSet",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "TransientIntMap",
   :var-type "type",
   :namespace "clojure.data.int-map",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/TransientIntMap",
   :source-url nil,
   :raw-source-url nil,
   :file nil}
  {:name "TransientIntSet",
   :var-type "type",
   :namespace "clojure.data.int-map",
   :arglists nil,
   :wiki-url
   "https://clojure.github.io/data.int-map//index.html#clojure.data.int-map/TransientIntSet",
   :source-url nil,
   :raw-source-url nil,
   :file nil})}
