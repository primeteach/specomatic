(ns specomatic.registry
  "Functions and the macros that work with global clojure.spec registry to generate the specomatic schema."
  (:require
   [clojure.spec.alpha       :as s]
   [nedap.speced.def         :as sd]
   [specomatic.core          :as sc]
   [specomatic.field-def     :as sf]
   [specomatic.registry.impl :as impl]
   [specomatic.spec          :as sp]))

(defn dispatch
  "Given spec `spec`, returns the spec if it is a keyword, or a description thereof if it is not."
  [spec]
  (when-let [spec (s/get-spec spec)]
    (if (keyword? spec)
      spec
      (s/describe spec))))

#?(:clj
     (defmacro defent
       "Defines an entity. Takes the same arguments as s/keys and
         * :id to specify the id field for the entity (optional, default is :entity-type/id)
         * :field-ns for a namespace to use field specs from (optional, default is current namespace)

       The entity name is added as a namespace to any unqualified keywords.
       If no spec exists for a field, but there is one for a keyword in the current namespace (overridden by :ns)
       and with the same name as the field, it is used for the field."
       [etype & args]
       `(do
          ~@(impl/defent-defs etype args))))

(defn- spec-predicate-fn
  "Returns the spec predicate function from a `spec-form` if the form is a sequence, nil otherwise."
  [spec-form]
  (when (seq? spec-form)
    (first spec-form)))

(defmulti etype-spec-form?
  "Checks if this spec form defines an entity type, extensible to every spec predicate function."
  spec-predicate-fn)

(defmethod etype-spec-form? :default [_spec-fn] false)

(defmethod etype-spec-form? `s/keys [_spec-fn] true)

(defmulti etype-spec-form-fields
  "Returns the fields from an entity-defining spec form, preserving the order of the fields."
  spec-predicate-fn)

(defmethod etype-spec-form-fields `s/keys
  [[_spec-fn & {:keys [req opt]}]]
  (concat req opt))

(defmulti etype-spec-form-required-fields
  "Returns the required fields from an entity-defining spec form as a set"
  spec-predicate-fn)

(defmethod etype-spec-form-required-fields `s/keys
  [[_spec-fn & {:keys [req]}]]
  (set req))

(defmulti reference-entity-key?
  "Checks if the key refers to the entity part of a s/or reference spec."
  (fn [k] k))

(defmethod reference-entity-key? :default [_k] false)

(defmethod reference-entity-key? :entity [_k] true)

(defmulti reference-id-key?
  "Checks if the key refers to the id part of a s/or reference spec."
  (fn [k] k))

(defmethod reference-id-key? :default [_k] false)

(defmethod reference-id-key? :id [_k] true)

(defmulti reference-spec-form?
  "Checks if this spec form defines a reference. Extensible to every spec predicate function."
  spec-predicate-fn)

(defmethod reference-spec-form? :default [_spec] false)

(defmethod reference-spec-form? `s/or
  [spec]
  (let [args (apply hash-map (rest spec))]
    (if (= 2 (count args))
      (let [[k1 k2] (keys args)
            [v1 v2] (vals args)]
        (or
          (and (reference-id-key? k1) (reference-entity-key? k2) (etype-spec-form? (s/form v2)))
          (and (reference-entity-key? k1) (reference-id-key? k2) (etype-spec-form? (s/form v1)))))
      false)))

(defmulti reference-spec-form-referenced-etype
  "Returns the referenced entity type from the given reference spec form, extensible to every spec predicate function."
  spec-predicate-fn)

(defmethod reference-spec-form-referenced-etype :default [_spec] nil)

(defmethod reference-spec-form-referenced-etype `s/or
  [spec]
  (let [args (apply hash-map (rest spec))]
    (if (= 2 (count args))
      (let [[k1 k2] (keys args)
            [v1 v2] (vals args)]
        (cond
          (reference-entity-key? k1) v1
          (reference-entity-key? k2) v2))
      nil)))

(defmulti reference-coll-spec-form?
  "Checks if this spec form defines a collection of references. Extensible to every spec predicate function."
  spec-predicate-fn)

(defmethod reference-coll-spec-form? :default [_spec] false)

(defmethod reference-coll-spec-form? `s/coll-of
  [spec]
  (reference-spec-form? (second spec)))

(defmulti reference-coll-spec-form-referenced-etype
  "Returns the referenced entity type from the given reference spec collection form, extensible to every spec predicate function."
  spec-predicate-fn)

(defmethod reference-coll-spec-form-referenced-etype :default [_spec] nil)

(defmethod reference-coll-spec-form-referenced-etype `s/coll-of
  [spec]
  (reference-spec-form-referenced-etype (second spec)))

(defn field-def
  "Given spec `spec`, returns a map containing information about the spec:
  {:kind              one of #{:simple :reference :reference-coll :part-coll}
   :dispatch          the spec if it is a keyword or a description thereof if it is not. Used for dispatch
   :target            the target entity type, only defined if type is #{:reference :reference-coll :part-coll}"
  [spec]
  (let [my-spec (s/get-spec spec)
        my-form (when my-spec (s/form my-spec))
        my-type (cond (reference-spec-form? my-form)
                      ::sf/reference
                      (reference-coll-spec-form? my-form)
                      ::sf/reference-coll
                      :else
                      ::sf/simple)]
    (merge
     {:kind my-type}
     (when (= ::sf/simple my-type)
       {:dispatch (dispatch spec)})
     (when-let [target (case my-type
                         ::sf/reference      (reference-spec-form-referenced-etype my-form)
                         ::sf/reference-coll (reference-coll-spec-form-referenced-etype my-form)
                         nil)]
       {:target target}))))

(defn- etypes-spec-forms
  "Returns a map of all entity types in the given `namespaces` to their spec."
  [namespaces]
  (into {}
        (comp (filter (fn [[k _]]
                        (some #{(-> k
                                    namespace
                                    symbol)}
                              namespaces)))
              (map (fn [[k v]] [k (s/form v)]))
              (filter (fn [[_ form]] (etype-spec-form? form))))
        (s/registry)))

(defn- etype-def
  "Given the entity type `etype` and its spec form `spec-form`, returns an entity definition from spec definitions."
  [etype spec-form]
  (merge
   {:field-defs      (into {}
                           (for [field (etype-spec-form-fields spec-form)]
                             [field
                              (field-def field)]))
    :required-fields (etype-spec-form-required-fields spec-form)}
   (when-let [id-field (impl/get-id etype)]
     {:id-field id-field})))

(sd/defn schema
  "Returns the entity schema for the `namespaces` as a map of entities to their schema.
  Optionally restricted to a sequence of entities `only`.
  Shape of result:
  {::my.namespace.entity  {:field-defs       map of fields to fields' specs
                           :id-field         id field for the entity
                           :required-fields  set of required fields}
  ::my.namespace.entity2 ... }"
  ([namespaces]
   (schema namespaces nil))
  ([namespaces only]
   (into {}
         (for [[etype spec-form] (etypes-spec-forms namespaces)
               :when             (or (nil? only)
                                     (some #{etype} only))]
           [etype (etype-def etype spec-form)]))))

(sd/defn full-schema
  "Returns the entity schema for the `namespaces` as a map of entities to their schema.
  Optionally restricted to a sequence of entities `only`.
  Specific entity and field definitions may by overridden by an `override` map in the same shape as the schema.
  Shape of result:
  {::my.namespace.entity  {:field-defs       map of fields to fields' specs
                           :id-field         id field for the entity
                           :required-fields  set of required fields}
  ::my.namespace.entity2 ... }"
  ([namespaces]
   (full-schema namespaces nil))
  ([namespaces override]
   (full-schema namespaces override nil))
  (^::sp/schema [^::sp/namespaces namespaces override only]
   (sc/full-schema (schema namespaces only) override)))
