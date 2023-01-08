(ns specomatic.spec
  "Specs and spec-returning macros for specomatic."
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::integer integer?)

(s/def ::field keyword?)

(s/def ::dispatch any?)

(s/def ::etype keyword?)

(s/def ::inverse-of ::field)

(s/def ::kind keyword?)

(s/def ::namespace symbol?)

(s/def ::reference-type keyword?)

(s/def ::target ::etype)

(s/def ::via ::field)

(s/def ::field-def
 (s/keys :req-un [::kind]
         :opt-un [::dispatch
                  ::inverse-of
                  ::reference-type
                  ::target
                  ::via]))

(s/def ::field-defs (s/map-of ::field ::field-def))

(s/def ::id-field ::field)

(s/def ::etype-def
 (s/keys :req-un [::field-defs]
         :opt-un [::id-field]))

(s/def ::fields (s/coll-of ::field))

(s/def ::etypes (s/coll-of ::etype))

(s/def ::schema (s/map-of ::etype ::etype-def))

(s/def ::namespaces (s/coll-of ::namespace))

#?(:clj (defmacro reference
          "Returns a spec for a reference to `etype`"
          [etype]
          `(s/or
            :id     integer?
            :entity ~etype)))

#?(:clj (defmacro references
          "Returns a spec for a collection of references to `etype`."
          [etype]
          `(s/coll-of
            (s/or
             :id     integer?
             :entity ~etype))))
