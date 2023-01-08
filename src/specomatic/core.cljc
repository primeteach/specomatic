(ns specomatic.core
  "Functions working with the specomatic schema."
  (:require
   [nedap.speced.def     :as sd]
   [specomatic.etype-def :as se]
   [specomatic.field-def :as sf]
   [specomatic.spec      :as sp]))

(sd/defn etypes
  "Given `schema`, returns a sequence of all entity types defined in it."
  ^::sp/etypes [^::sp/schema schema]
  (keys schema))

(sd/defn etype-def
  "Given `schema` and entity type `etype`, returns a definition of the entity type."
  ^::sd/nilable ^::sp/etype-def [^::sp/schema schema ^::sp/etype etype]
  (etype schema))

(sd/defn field-defs
  "Given `schema` and entity type `etype`, returns a map of fields (keywords) to their definitions."
  ^::sd/nilable ^::sp/field-defs [^::sp/schema schema ^::sp/etype etype]
  (some-> (etype-def schema etype)
          se/field-defs))

(sd/defn field-defs-including-etype
  "Given `schema` and entity type `etype`, returns a map of fields (keywords) to their definitions, with the entity type assoc'd to the :etype key."
  ^::sd/nilable ^::sp/field-defs [^::sp/schema schema ^::sp/etype etype]
  (reduce-kv #(assoc % %2 (assoc %3 :etype etype)) {} (field-defs schema etype)))

(sd/defn id-field
  "Returns the id keyword for the entity type `etype` using the `schema`."
  ^::sp/field [^::sp/schema schema ^::sp/etype etype]
  (get-in schema [etype :id-field]))

(sd/defn display-name-fields
  "Given the entity type `etype`, returns a set of fields (keywords) that are part of the display name of the entity type."
  [^::sp/schema schema ^::sp/etype etype]
  (get-in schema [etype :display-name-fields]))

(sd/defn required-fields
  "Given `schema` and the entity type `etype`, returns the set of required fields."
  ^::sp/fields [^::sp/schema schema ^::sp/etype etype]
  (-> schema
      (etype-def etype)
      se/required-fields))

(sd/defn dispatch
  "Given `schema`, entity type `etype` and `field`, returns the spec for the field if it is a keyword, or a description thereof if it is not."
  [^::sp/schema schema ^::sp/etype etype ^::sp/field field]
  (-> schema
      (field-defs etype)
      field
      :dispatch))

(sd/defn reference-colls-by-owning-etype
  "Returns a map of entity types to a map of reference collections owmed by them to their field definitions."
  ^map? [^::sp/schema schema]
  (into {}
        (for [etype (etypes schema)
              :let  [reference-coll-defs
                     (for [[k my-field-def] (field-defs schema etype)
                           :when            (and (sf/reference-coll? my-field-def)
                                                 (not (sf/inverse? my-field-def)))]
                       [k my-field-def])]
              :when (not-empty reference-coll-defs)]
          [etype (into {} reference-coll-defs)])))

(sd/defn all-field-defs-including-etype*
  "Returns a map of fields to field definitions, with the entity type assoc'd to the :etype key, in `schema`."
  ^:private ^::sp/field-defs [^::sp/schema schema]
  (->> schema
       etypes
       (map #(field-defs-including-etype schema %))
       (apply merge)))

(def all-field-defs-including-etype
  "Returns a map of fields to field definitions, with the entity type assoc'd to the :etype key, in `schema` (memoized)."
  (memoize all-field-defs-including-etype*))

(sd/defn field-def
  "Given `schema` and `field`, returns the definition of the field."
  ^::sd/nilable ^::sp/field-def [^::sp/schema schema ^::sp/field field]
  (-> schema
      all-field-defs-including-etype
      field
      (dissoc :etype)))

(sd/defn etype-from-field
  "Given `schema` and `field`, returns the entity type of the field"
  ^::sd/nilable ^::sp/etype [^::sp/schema schema ^::sp/field field]
  (-> schema
      all-field-defs-including-etype
      field
      :etype))

(sd/defn all-fields*
  "Returns all fields defined in `schema`."
  ^:private ^::sp/fields [^::sp/schema schema]
  (keys (all-field-defs-including-etype schema)))

(def all-fields
  "Returns all fields defined in `schema` (memoized)."
  (memoize all-fields*))

(sd/defn field-defined?
  "Checks if `field` is defined in `schema`."
  ^boolean? [^::sp/schema schema ^::sp/field field]
  (contains? (all-field-defs-including-etype schema) field))

(sd/defn defaults-schema
  "Given a `schema`, returns a (partial) schema containing defaults."
  [^::sp/schema schema]
  (reduce-kv #(assoc % %2 (se/defaults %3)) {} schema))

(defn- inverse-field-info
  [schema]
  (into {}
        (for [my-etype             (etypes schema)
              [field my-field-def] (field-defs schema my-etype)
              :let                 [[inverse-etype inverse-field inverse-field-def] (sf/inverse-field-def my-etype
                                                                                                          field
                                                                                                          my-field-def)]
              :when                inverse-field-def]
          [(sf/inverse-of inverse-field-def)
           [inverse-etype inverse-field inverse-field-def]])))

(defn- field-by-inverse-of
  [overrides]
  (->> (vals overrides)
       (map :field-defs)
       (apply merge)
       (map (fn [[override-field override-field-def]] [(:inverse-of
                                                        override-field-def)
                                                       override-field]))
       (into {})))

(sd/defn inverse-schema
  "Given a `schema` and `overrides` to override inverse field keywords, returns a (partial) schema containing inverse field definitions."
  [^::sp/schema schema ^::sd/nilable ^map? overrides]
  (let [all-field-def-overrides (apply merge (map :field-defs (vals overrides)))
        my-field-by-inverse-of  (field-by-inverse-of overrides)]
    (->>
     (for [[inverse-of [etype field my-field-def]] (inverse-field-info schema)
           :let                                    [overridden-field     (inverse-of my-field-by-inverse-of)
                                                    overridden-field-def (when overridden-field
                                                                           (overridden-field all-field-def-overrides))]]
       [etype
        [(or overridden-field field)
         (if overridden-field
           (merge overridden-field-def
                  (select-keys my-field-def [:via :target])
                  (sf/defaults overridden-field overridden-field-def))
           my-field-def)]])
     (group-by first)
     (reduce-kv #(assoc % %2 {:field-defs (into {} (map second %3))}) {}))))

(sd/defn full-schema
  "Given a `schema` returned from `specomatic.registry/schema` spec and optionally `overrides` to override defaults,
  returns an schema enriched with defaults and inverse fields."
  ^::sp/schema [^::sp/schema schema ^::sd/nilable ^map? overrides]
  (merge-with se/merge-etype-defs
              schema
              (defaults-schema schema)
              (inverse-schema schema overrides)
              overrides))
