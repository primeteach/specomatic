(ns specomatic.field-def
  "Functions working with specomatic field definitions."
  (:require
   [nedap.speced.def :as sd]
   [specomatic.spec  :as sp]))

(sd/defn kind
  "Given the field definition `field-def`, returns the kind."
  ^::sp/kind [^::sp/field-def field-def]
  (:kind field-def))

(sd/defn dispatch
  "Given the field definition `field-def`, returns the dispatch value."
  ^::sp/dispatch [^::sp/field-def field-def]
  (:dispatch field-def))

(sd/defn inverse-of
  "Given the field definition `field-def`, returns the inverse field."
  ^::sp/field [^::sp/field-def field-def]
  (:inverse-of field-def))

(sd/defn inverse?
  "Checks if field definition `field-def` is inverse."
  ^boolean? [^::sp/field-def field-def]
  (some? (:inverse-of field-def)))

(sd/defn reference-type
  "Given the field definition `field-def`, returns the reference type."
  [^::sp/field-def field-def]
  (:reference-type field-def))

(sd/defn target
  "Given the field definition `field-def`, returns the target entity type."
  ^::sp/etype [^::sp/field-def field-def]
  (:target field-def))

(sd/defn via
  "Given the reference field definition `field-def`, returns the reference field on the opposite side of the relation, if available."
  ^::sd/nilable ^::sp/field [^::sp/field-def field-def]
  (:via field-def))

(defmulti defaults
  "Given field `field` and field definition `field-def`
  returns a map of defaults for the field definition."
  (fn [_field field-def] (kind field-def)))

(defmethod defaults :default [_field _param-field-def] {})

(defmethod defaults ::reference
  [_field _field-def]
  {:reference-type :has-one})

(defmethod defaults ::reference-coll
  [_field _field-def]
  {:reference-type :has-many-through})

(defmulti inverse-field-def
  "Given entity type `etype`, `field` and field definition `field-def`,
  returns a tuple of entity type, inverse field and field definition
  or nil if none can be inferred."
  (fn [_etype _field field-def] (kind field-def)))

(defmethod inverse-field-def :default [_etype _field _field-def] nil)

(defmethod inverse-field-def ::reference
  [etype field field-def]
  (let [my-target (target field-def)]
    [my-target
     (keyword (name my-target) (str (name etype) "s"))
     {:inverse-of     field
      :kind           ::reference-coll
      :reference-type :has-many
      :target         etype
      :via            field}]))

(defmethod inverse-field-def ::reference-coll
  [etype field field-def]
  (let [my-target (target field-def)]
    [my-target
     (keyword (name my-target) (str (name etype) "s"))
     {:inverse-of     field
      :kind           ::reference-coll
      :reference-type :has-many-through
      :target         etype}]))

(defmulti reference?
  "Checks if the field definition `field-def` is a reference."
  (fn [field-def] (kind field-def)))

(defmethod reference? :default [_field-def] false)

(defmethod reference? ::reference [_field-def] true)

(defmulti reference-coll?
  "Checks if the field definition `field-def` is a reference collection."
  (fn [field-def] (kind field-def)))

(defmethod reference-coll? :default [_field-def] false)

(defmethod reference-coll? ::reference-coll [_field-def] true)

(defmulti relational?
  "Checks if the field definition `field-def` is relational."
  (fn [field-def] (kind field-def)))

(defmethod relational? :default [_field-def] false)

(defmethod relational? ::reference [_field-def] true)

(defmethod relational? ::reference-coll [_field-def] true)
