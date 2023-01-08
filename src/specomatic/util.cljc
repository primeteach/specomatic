(ns specomatic.util
  "Specomatic utility functions."
  (:require
   [nedap.speced.def :as sd]))

(sd/defn concat-keywords
  "Returns a keyword with `k`'s namespace and a concatenation of both name parts as name."
  [^keyword? k ^keyword? l]
  (keyword (namespace k) (str (name k) (name l))))

(sd/defn strip-ns
  "Strips the namespace from keyword `k`."
  ^keyword? [^keyword? k]
  (keyword (name k)))

(sd/defn qualify
  "Returns a keyword with the name of `etype` as namespace part and the name of `k` as name part."
  ^keyword? [^keyword? etype ^keyword? k]
  (if (qualified-keyword? k)
    k
    (keyword (name etype) (name k))))
