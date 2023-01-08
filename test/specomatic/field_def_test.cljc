(ns specomatic.field-def-test
  (:require
   [clojure.test         :refer [deftest is testing]]
   [specomatic.core      :as sc]
   [specomatic.field-def :as sf]
   [specomatic.spec      :as sp]))

(deftest accessor-test
  (testing "Accessor functions"
    (is (= ::sf/simple (sf/kind {:kind ::sf/simple}))
        "Kind should be returned by accessor function")
    (is (= ::sc/integer
           (sf/dispatch {:kind     ::sf/simple
                         :dispatch ::sc/integer}))
        "Dispatch should be returned by accessor function")
    (is (= :vegetable/potato
           (sf/target {:kind   ::sf/reference
                       :target :vegetable/potato}))
        "Target should be returned by accessor function")
    (is (= :has-many
           (sf/reference-type {:inverse-of     :review/movie
                               :kind           ::sf/reference-coll
                               :reference-type :has-many
                               :target         :schema/review
                               :via            :review/movie}))
        "Reference type should be returned by accessor function")
    (is (= :review/movie
           (sf/via {:inverse-of     :review/movie
                    :kind           ::sf/reference-coll
                    :reference-type :has-many
                    :target         :schema/review
                    :via            :review/movie}))
        "Via should be returned by accessor function")
    (is (spec-assertion-thrown? ::sp/field-def (sf/kind "Not a field def"))
        "Invalid field-def should fail validation")
    (is (spec-assertion-thrown? ::sp/field-def (sf/dispatch {:kind 'not-a-kind}))
        "Invalid field-def should fail validation")
    (is (spec-assertion-thrown? ::sp/etype
                                (sf/target {:kind ::sf/reference})
                                "Missing target should fail validation"))))

(derive :my.ns/reference ::sf/reference)

(derive :my.ns/reference-coll ::sf/reference-coll)

(deftest field-kind-predicate-test
  (testing "Field kind predicate functions"
    (is (true? (sf/inverse? {:kind       ::sf/reference-coll
                             :inverse-of :potato/crate}))
        "inverse? should be true if there is an inverse-of")
    (is (true? (sf/reference? {:kind :my.ns/reference}))
        "reference? should return true if kind derives from :specomatic.core/reference")
    (is (true? (sf/reference? {:kind ::sf/reference}))
        "reference? should return true if kind equals :specomatic.core/reference")
    (is (true? (sf/reference-coll? {:kind :my.ns/reference-coll}))
        "reference-coll? should return true if kind derives from :specomatic.core/reference-coll")
    (is (true? (sf/reference-coll? {:kind ::sf/reference-coll}))
        "reference-coll? should return true if kind equals :specomatic.core/reference-coll")
    (is (true? (sf/relational? {:kind ::sf/reference-coll}))
        "relational? should return true if kind equals :sf/reference-coll")
    (is (true? (sf/relational? {:kind :my.ns/reference}))
        "relational? should return true if kind derives from :specomatic.core/reference")
    (is (false? (sf/relational? {:kind ::sf/simple}))
        "reference-coll? should return false if kind equals :specomatic.core/simple")))
