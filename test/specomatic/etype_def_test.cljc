(ns specomatic.etype-def-test
  (:require
   [clojure.test         :refer [deftest is]]
   [schema]
   [specomatic.core      :as sc]
   [specomatic.etype-def :as se]
   [specomatic.field-def :as-alias sf]
   [specomatic.spec      :as sp]))

(deftest field-def-test
  (is (= {:kind     ::sf/simple
          :dispatch #{:hillbilly :marmande :moneymaker}}
         (se/field-def {:field-defs      {:tomato/serial-number {:kind     ::sf/simple
                                                                 :dispatch ::sp/integer}
                                          :tomato/variety       {:kind     ::sf/simple
                                                                 :dispatch #{:hillbilly :marmande :moneymaker}}
                                          :tomato/weight        {:kind     ::sf/simple
                                                                 :dispatch ::sp/integer}}
                        :id-field        :tomato/serial-number
                        :required-fields #{}}
                       :tomato/variety))
      "field-def should return the definition for the field"))

(deftest id-field
  (is (= :tomato/serial-number
         (se/id-field {:field-defs      {:tomato/serial-number {:kind     ::sf/simple
                                                                :dispatch ::sp/integer}
                                         :tomato/variety       {:kind     ::sf/simple
                                                                :dispatch #{:hillbilly :marmande :moneymaker}}
                                         :tomato/weight        {:kind     ::sf/simple
                                                                :dispatch ::sp/integer}}
                       :id-field        :tomato/serial-number
                       :required-fields #{}}))
      "id-field should return the id field for the entity type"))

(deftest display-name-fields-test
  (is (= #{:director/name}
         (se/display-name-fields (sc/etype-def schema/full-cinema-schema
                                               :schema/director)))
      "display-name-fields should return the fields that are part of the display name of the entity type"))
