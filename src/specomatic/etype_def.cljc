(ns specomatic.etype-def
  "Functions working with specomatic entity type definitions."
  (:require
   [nedap.speced.def     :as sd]
   [specomatic.field-def :as sf]
   [specomatic.spec      :as sp]))

(sd/defn field-defs
  "Given the entity type definition `etype-def`, returns a map of fields (keywords) to their field definition."
  ^::sd/nilable ^::sp/field-defs [^::sp/etype-def etype-def]
  (:field-defs etype-def))

(sd/defn field-def
  "Given the entity type definition `etype-def` and a `field`, returns the field definition for the field."
  ^::sp/field-def [^::sp/etype-def etype-def ^::sp/field field]
  (field (field-defs etype-def)))

(sd/defn id-field
  "Given the entity type definition `etype-def`, returns the id field."
  ^::sd/nilable ^::sp/field [^::sp/etype-def etype-def]
  (:id-field etype-def))

(sd/defn display-name-fields
  "Given the entity type definition `etype-def`, returns a set of fields (keywords) that are part of the display name of the entity type."
  ^::sd/nilable ^::sp/fields [^::sp/etype-def etype]
  (:display-name-fields etype))

(sd/defn required-fields
  "Given the entity type definition `etype-def`, returns the set of required fields."
  [^::sp/etype-def etype-def]
  (:required-fields etype-def))

(sd/defn defaults
  "Given entity type definition `etype-def`, returns a (partial) entity type definition containing defaults."
  [^::sp/etype-def etype-def]
  (merge
   (when-let [default-id-field (some #(when (= "id" (name %)) %)
                                     (keys (field-defs etype-def)))]
     {:id-field default-id-field})
   (when-let [default-name-field (some #(when (= "name" (name %)) %)
                                       (keys (field-defs etype-def)))]
     {:display-name-fields #{default-name-field}})
   (when-let [default-field-defs (reduce-kv #(when-let [my-defaults (not-empty (sf/defaults %2 %3))]
                                               (assoc % %2 my-defaults))
                                            {}
                                            (field-defs etype-def))]
     {:field-defs default-field-defs})))

(defn merge-etype-defs
  "Merge given entity type definitions `etype-defs`, merging their `:field-defs`."
  [& etype-defs]
  (reduce
   #(assoc (merge % %2)
           :field-defs
           (merge-with merge
                       (:field-defs %)
                       (:field-defs %2)))
   etype-defs))
