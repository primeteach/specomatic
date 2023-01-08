(ns specomatic.registry-test
  (:require
   [clojure.spec.alpha  :as s]
   [clojure.test        :refer [deftest is testing]]
   [schema]
   [specomatic.registry :as sr]
   [specomatic.spec     :as sp]))

(s/def ::weight ::sp/integer)

(s/def :potato/variety #{:agata :charlotte :desiree :jazzy})

(s/def :tomato/variety #{:hillbilly :marmande :moneymaker})

(sr/defent :schema/potato :opt [:variety :weight])

(sr/defent :schema/tomato :id :serial-number :opt [:variety :weight])

(s/def ::name string?)

(s/def ::title string?)

(s/def ::release-year integer?)

(sr/defent :schema/actor
           :req
           [:name])

(sr/defent :schema/director
           :req
           [:name])

(s/def :movie/actors (sp/references :schema/actor))

(s/def :movie/directors (sp/references :schema/director))

(sr/defent :schema/movie
           :req [:title :release-year]
           :opt [:actors
                 :directors])

(s/def :review/movie (sp/reference :schema/movie))

(sr/defent :schema/review
           :req
           [:title :movie])

(s/def ::username string?)

(s/def :user/director (sp/reference :schema/director))

(sr/defent :schema/user
           :req
           [:username]
           :opt
           [:director])

(deftest defent-and-schema-test
  (is
   (= (sr/schema ['schema])
      (merge schema/cinema-schema schema/potato-schema schema/tomato-schema))
   "The entity-schema derived from the spec definitions above should be equal to the explicitely defined schema in the schema namespace"))

(deftest derive-test
  (testing "Check if attributes with a keyword spec derive from it"
    (is (isa? :tomato/weight ::sp/integer)
        "Tomato weight should derive from integer spec keyword")))
