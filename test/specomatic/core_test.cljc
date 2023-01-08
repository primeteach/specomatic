(ns specomatic.core-test
  (:require
   [clojure.test         :refer [deftest testing is]]
   [schema]
   [specomatic.core      :as sc]
   [specomatic.field-def :as sf]
   [specomatic.spec      :as sp]))

(deftest etypes-test
  (is (= #{:schema/movie :schema/director :schema/actor :schema/review :schema/user}
         (set (sc/etypes schema/cinema-schema)))
      "etypes should return all entity types"))

(deftest etype-def-test
  (is (= {:field-defs      #:actor{:id   {:kind     ::sf/simple
                                          :dispatch ::sp/integer}
                            :name {:kind     ::sf/simple
                                          :dispatch 'string?}}
          :required-fields #{:actor/name}}
         (sc/etype-def schema/cinema-schema :schema/actor))
      "etype-def should return the definition of the entity type")
  (is (spec-assertion-thrown?
       ::sp/schema
       (sc/etypes {:some/thing {:vaguely {:kind :of
                                          :like "a-schema"}}}))
      "etype-def should refuse an invalid schema"))

(deftest field-defs-test
  (is (= {:potato/id      {:kind     ::sf/simple
                           :dispatch ::sp/integer}
          :potato/variety {:kind     ::sf/simple
                           :dispatch #{:agata :charlotte :desiree :jazzy}}
          :potato/weight  {:kind     ::sf/simple
                           :dispatch ::sp/integer}}
         (sc/field-defs schema/potato-schema :schema/potato))
      "entity-fields should return the fields defined for the entity type in the schema")
  (is (nil? (sc/field-defs schema/potato-schema :schema/tomato))
      "entity-fields should return nil if the entity type is not defined in the schema"))

(deftest field-defs-including-etype-test
  (is
   (= {:potato/id      {:etype    :schema/potato
                        :kind     ::sf/simple
                        :dispatch ::sp/integer}
       :potato/variety {:etype    :schema/potato
                        :kind     ::sf/simple
                        :dispatch #{:agata :charlotte :desiree :jazzy}}
       :potato/weight  {:etype    :schema/potato
                        :kind     ::sf/simple
                        :dispatch ::sp/integer}}
      (sc/field-defs-including-etype schema/potato-schema :schema/potato))
   "entity-fields should return the fields defined for the entity type in the schema, with the entity type assoc'd to
   the :etype key."))

(deftest id-field-test
  (testing "Valid id field for entity type"
    (is (= :tomato/serial-number
           (sc/id-field schema/tomato-schema :schema/tomato))
        "id-field should return given id field keyword."))
  (testing "Invalid id field for entity type"
    (is (spec-assertion-thrown?
         ::sp/schema
         (sc/id-field {:schema/potato {:id-field "Gobbledygook"}}
                      :schema/potato)))))

(deftest display-name-fields-test
  (is (= #{:actor/name}
         (sc/display-name-fields schema/full-cinema-schema :schema/actor))
      "display-name-fields should return the parts of the display name of the entity type."))

(deftest required-fields-test
  (is (= #{:movie/title :movie/release-year}
         (sc/required-fields schema/full-cinema-schema :schema/movie))
      "required-fields should return the fields required for the entity type."))

(deftest dispatch-test
  (is (= ::sp/integer
         (sc/dispatch schema/potato-schema :schema/potato :potato/id))
      "dispatch should return the field's spec if it is a keyword")
  (is (= #{:agata :charlotte :desiree :jazzy}
         (sc/dispatch schema/potato-schema :schema/potato :potato/variety))
      "dispatch should return a description of the field's spec if it is not a keyword"))

(deftest reference-colls-by-owning-etype-test
  (is (= #:schema{:movie #:movie{:actors    {:kind           :specomatic.field-def/reference-coll
                                             :target         :schema/actor
                                             :reference-type :has-many-through}
                          :directors {:kind           :specomatic.field-def/reference-coll
                                             :target         :schema/director
                                             :reference-type :has-many-through}}}
         (sc/reference-colls-by-owning-etype schema/full-cinema-schema))))

(deftest all-fields-including-etype-test
  (is
   (= {:movie/actors       {:kind   :specomatic.field-def/reference-coll
                            :target :schema/actor
                            :etype  :schema/movie}
       :user/username      {:kind     :specomatic.field-def/simple
                            :dispatch 'string?
                            :etype    :schema/user}
       :review/movie       {:kind   :specomatic.field-def/reference
                            :target :schema/movie
                            :etype  :schema/review}
       :movie/id           {:kind     :specomatic.field-def/simple
                            :dispatch :specomatic.spec/integer
                            :etype    :schema/movie}
       :movie/release-year {:kind     :specomatic.field-def/simple
                            :dispatch 'integer?
                            :etype    :schema/movie}
       :movie/directors    {:kind   :specomatic.field-def/reference-coll
                            :target :schema/director
                            :etype  :schema/movie}
       :director/name      {:kind     :specomatic.field-def/simple
                            :dispatch 'string?
                            :etype    :schema/director}
       :review/title       {:kind     :specomatic.field-def/simple
                            :dispatch 'string?
                            :etype    :schema/review}
       :director/id        {:kind     :specomatic.field-def/simple
                            :dispatch :specomatic.spec/integer
                            :etype    :schema/director}
       :user/id            {:kind     :specomatic.field-def/simple
                            :dispatch :specomatic.spec/integer
                            :etype    :schema/user}
       :actor/name         {:kind     :specomatic.field-def/simple
                            :dispatch 'string?
                            :etype    :schema/actor}
       :review/id          {:kind     :specomatic.field-def/simple
                            :dispatch :specomatic.spec/integer
                            :etype    :schema/review}
       :actor/id           {:kind     :specomatic.field-def/simple
                            :dispatch :specomatic.spec/integer
                            :etype    :schema/actor}
       :movie/title        {:kind     :specomatic.field-def/simple
                            :dispatch 'string?
                            :etype    :schema/movie}
       :user/director      {:kind   :specomatic.field-def/reference
                            :target :schema/director
                            :etype  :schema/user}}
      (sc/all-field-defs-including-etype schema/cinema-schema))
   "all-fields-including-etype should return a map of fields to field definitions, with the entity type assoc'd to the :etype key"))

(deftest all-fields-test
  (is (= #{:movie/actors :user/username :review/movie :movie/id :movie/release-year :movie/directors :director/name
           :review/title :movie/reviews :director/id :actor/movies :user/id :director/user :director/movies :actor/name
           :review/id :actor/id :movie/title :user/director}
         (set (sc/all-fields schema/full-cinema-schema)))
      "all-fields should return a sequence of all fields defined in the schema"))

(deftest field-def-test
  (is (nil? (sc/field-def schema/potato-schema :potato/color))
      "field-def should return nil if the field is not defined in the schema")
  (is (= {:kind     :specomatic.field-def/simple
          :dispatch :specomatic.spec/integer}
         (sc/field-def schema/potato-schema :potato/weight))
      "field-def should return the field definition"))

(deftest etype-from-field-test
  (is (= :schema/potato
         (sc/etype-from-field schema/potato-schema :potato/variety))
      "etype-from-field should return the entity type of the field")
  (is
   (nil? (sc/field-def schema/potato-schema :giraffe/height))
   "etype-from-field should return nil if the field is not defined in the schema"))

(deftest field-defined?-test
  (is (false? (sc/field-defined? schema/potato-schema :potato/color))
      "field-defined? should return false if the field is not defined in the schema")
  (is (true? (sc/field-defined? schema/potato-schema :potato/weight))
      "field-defined? should return true if the field is defined in the schema"))

(deftest defaults-schema-test
  (is (= #:schema{:movie    {:id-field   :movie/id
                             :field-defs #:movie{:actors    {:reference-type :has-many-through}
                                          :directors {:reference-type :has-many-through}}}
          :director {:id-field            :director/id
                             :display-name-fields #{:director/name}}
          :actor    {:id-field            :actor/id
                             :display-name-fields #{:actor/name}}
          :review   {:id-field   :review/id
                             :field-defs #:review{:movie {:reference-type :has-one}}}
          :user     {:id-field   :user/id
                             :field-defs #:user{:director {:reference-type :has-one}}}}
         (sc/defaults-schema schema/cinema-schema))
      "defaults-schema should return a partial schema containing inferred defaults."))

(deftest inverse-schema-test
  (is (= #:schema{:director {:field-defs #:director{:users  {:inverse-of     :user/director
                                                             :kind           :specomatic.field-def/reference-coll
                                                             :reference-type :has-many
                                                             :target         :schema/user
                                                             :via            :user/director}
                                          :movies {:inverse-of     :movie/directors
                                                             :kind           :specomatic.field-def/reference-coll
                                                             :reference-type :has-many-through
                                                             :target         :schema/movie}}}
          :movie    {:field-defs #:movie{:reviews {:inverse-of     :review/movie
                                                           :kind           :specomatic.field-def/reference-coll
                                                           :reference-type :has-many
                                                           :target         :schema/review
                                                           :via            :review/movie}}}
          :actor    {:field-defs #:actor{:movies {:inverse-of     :movie/actors
                                                          :kind           :specomatic.field-def/reference-coll
                                                          :reference-type :has-many-through
                                                          :target         :schema/movie}}}}
         (sc/inverse-schema schema/cinema-schema nil))
      "inverse-schema should return a partial schema containing inferred inverse field definitions.")
  (is
   (= #:schema{:director {:field-defs #:director{:user   {:inverse-of     :user/director
                                                          :kind           :specomatic.field-def/reference
                                                          :via            :user/director
                                                          :target         :schema/user
                                                          :reference-type :has-one}
                                       :movies {:inverse-of     :movie/directors
                                                          :kind           :specomatic.field-def/reference-coll
                                                          :reference-type :has-many-through
                                                          :target         :schema/movie}}}
       :movie    {:field-defs #:movie{:reviews {:inverse-of     :review/movie
                                                        :kind           :specomatic.field-def/reference-coll
                                                        :reference-type :has-many
                                                        :target         :schema/review
                                                        :via            :review/movie}}}
       :actor    {:field-defs #:actor{:movies {:inverse-of     :movie/actors
                                                       :kind           :specomatic.field-def/reference-coll
                                                       :reference-type :has-many-through
                                                       :target         :schema/movie}}}}
      (sc/inverse-schema schema/cinema-schema schema/cinema-schema-overrides))
   "inverse-schema should return a partial schema containing inferred inverse field definitions, merged with overrides."))

(deftest full-schema-test
  (is
   (= schema/full-cinema-schema
      (sc/full-schema schema/cinema-schema schema/cinema-schema-overrides))
   "full-schema should return the schema enriched with defaults and inverse fields, overriden by overrides"))
