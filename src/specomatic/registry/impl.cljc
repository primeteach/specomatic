(ns ^:no-doc specomatic.registry.impl
  "This namespace provides implementation for the `specomatic.registry` namespace. This is not intended for direct use, anything here is subject to (undocumented) changes."
  (:require
   [clojure.spec.alpha :as s]
   [specomatic.spec    :as sp]
   [specomatic.util    :as su]))

(defonce
 ^{:doc     "This atom contains a map from entity type to id field."
   :private true}
 id-registry
 (atom {}))

(defn def-id
  "Defines `field` as id field for the entity type `etype`"
  [etype field]
  (swap! id-registry assoc etype field))

(defn get-id
  "Gets id field for the entity type `etype`"
  [etype]
  (get @id-registry etype))

(defn- expand-keys-args
  "Expand arguments for s/keys, applying defaults and qualifying unqualified keys with the entity type."
  [etype {:keys [gen id req opt]}]
  (let [id-key       (or id :id)
        req-expanded (mapv #(keyword (name etype) (name %)) req)
        opt-expanded (mapv #(keyword (name etype) (name %)) (into [id-key] opt))
        keys-args
        (merge {:opt opt-expanded}
               (when req
                 {:req req-expanded})
               (when gen
                 {:gen gen}))]
    (reduce into (seq keys-args))))

(defn- field-defs
  "Returns the code for specifying fields of an entity type."
  [field-ns etype id-key req opt]
  (for [field (concat [id-key] req opt)
        :let  [field-name        (name field)
               entity-field      (keyword (name etype) field-name)
               ns-field          (keyword (str (or field-ns *ns*)) field-name)
               entity-field-spec (s/get-spec entity-field)
               inherited-spec    (or (s/get-spec ns-field)
                                     (when (= field id-key) ::sp/integer))
               our-spec          (or entity-field-spec inherited-spec)]]
    (concat
     (when (and (nil? entity-field-spec) inherited-spec)
       (if (keyword? inherited-spec)
         [`(s/def ~entity-field ~inherited-spec)]
         (let [ns-field-spec-form (s/form ns-field)]
           [`(s/def ~entity-field ~ns-field-spec-form)])))
     (when (keyword? our-spec)
       [`(derive ~entity-field ~our-spec)]))))

(defn defent-defs
  "Returns definitions for the `defent` macro."
  [etype
   {:keys [field-ns id req opt]
    :as   args}]
  (let [id-key (or id :id)]
    (reduce into
            (into
             [[`(s/def ~etype
                 (s/keys ~@(expand-keys-args etype args)))]
              (when id
                [`(def-id ~etype (su/qualify ~etype ~id))])]
             (field-defs field-ns etype id-key req opt)))))
