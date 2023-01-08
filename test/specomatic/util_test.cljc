(ns specomatic.util-test
  (:require
   [clojure.test    :refer [deftest is]]
   [specomatic.util :as su]))

(deftest concat-keywords-test
  (is (= :ns/ab (su/concat-keywords :ns/a :ns2/b)))
  (is (thrown? Exception (su/concat-keywords "a" "b"))
      "concat-keywords should only accept keywords as arguments"))

(deftest strip-ns-test
  (is (= :potato (su/strip-ns :vegetable/potato)))
  (is (= :potato (su/strip-ns :potato)))
  (is (thrown? Exception (su/strip-ns "potato"))))

(deftest qualify-test
  (is (= :food.vegetable/tomato (su/qualify :food.vegetable :tomato)))
  (is (= :vegetable/tomato (su/qualify :vegetable :tomato)))
  (is (= :edible/tomato (su/qualify :vegetable :edible/tomato))
      "The keyword should not be changed if it is already qualified"))
