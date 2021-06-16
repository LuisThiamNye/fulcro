(ns com.fulcrologic.fulcro.macros.defmutation-spec
  (:require
    [com.fulcrologic.fulcro.mutations :as m]
    [fulcro-spec.core :refer [specification assertions component]]
    [clojure.test :refer :all]
    [com.fulcrologic.fulcro.algorithms.lookup :as ah]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.raw.components :as rc]))

(declare =>)

(specification "defmutation Macro"
  (component "Defining a mutation into a different namespace"
    (let [actual (m/defmutation* {}
                   '(other/boo [params]
                      (action [env] (swap! state))))]
      (assertions
        "Emits a defmethod with the proper symbol, action, and default result action."
        actual => `(defmethod com.fulcrologic.fulcro.mutations/mutate 'other/boo [~'fulcro-mutation-env-symbol]
                     (let [~'params (-> ~'fulcro-mutation-env-symbol :ast :params)]
                       {:result-action (fn [~'env]
                                         (binding [rc/*after-render* true]
                                           (when-let [~'default-action (ah/app-algorithm (:app ~'env) :default-result-action!)]
                                             (~'default-action ~'env))))
                        :action        (fn ~'action [~'env]
                                         (clojure.core/binding [com.fulcrologic.fulcro.raw.components/*after-render* true]
                                           (~'swap! ~'state)) nil)})))))
  (component "Overridden result action"
    (let [actual (m/defmutation* {}
                   '(other/boo [params]
                      (result-action [env] (print "Hi"))))]
      (assertions
        "Uses the user-supplied version of default action"
        actual => `(defmethod com.fulcrologic.fulcro.mutations/mutate 'other/boo [~'fulcro-mutation-env-symbol]
                     (let [~'params (-> ~'fulcro-mutation-env-symbol :ast :params)]
                       {:result-action (fn ~'result-action [~'env]
                                         (clojure.core/binding [com.fulcrologic.fulcro.raw.components/*after-render* true]
                                           (~'print "Hi")) nil)})))))
  (component "Mutation remotes"
    (let [actual (m/defmutation* {}
                   '(boo [params]
                      (action [env] (swap! state))
                      (remote [env] true)
                      (rest [env] true)))
          method (nth actual 2)
          body   (nth method 4)]
      (assertions
        "Converts all sections to lambdas of a defmethod"
        (first method) => `defmethod
        body => `(let [~'params (-> ~'fulcro-mutation-env-symbol :ast :params)]
                   {:result-action (fn [~'env]
                                     (binding [rc/*after-render* true]
                                       (when-let [~'default-action (ah/app-algorithm (:app ~'env) :default-result-action!)]
                                         (~'default-action ~'env))))
                    :remote        (fn ~'remote [~'env] true)
                    :rest          (fn ~'rest [~'env] true)
                    :action        (fn ~'action [~'env]
                                     (binding [rc/*after-render* true]
                                       (~'swap! ~'state)) nil)})))))
