(ns circle.model.project
  (:use [circle.util.validation :only (validate defn-v)])
  (:use [clojure.core.incubator :only (-?>)])
  (:use [circle.util.model-validation :only (validate!)])
  (:use [circle.util.except :only (assert!)])
  (:require [circle.backend.github-url :as github])
  (:use [circle.util.model-validation-helpers :only (require-keys key-types col-predicate)])
  (:require [somnium.congomongo :as mongo]))

(def project-validation [(require-keys [:name
                                        :vcs_type
                                        :vcs_url
                                        :aws_credentials])
                         (col-predicate :vcs_url #(= :top (github/url-type %)) "github url must be https://github.com/foo/bar")])

(defmethod validate ::Project [tag obj]
  (validate! project-validation obj))

(defn project [& {:keys [name
                         vcs_type
                         vcs_url ;; the canonical url for the repo. Must match the URL that github will provide in the post-commit hook
                         aws_credentials
                         ssh_private_key ;; an SSH private key authorized to checkout code
                         ami-id
                         actions]
                  :as args}]
  (validate! project-validation args)
  args)

(defn-v insert! [^::Project p]
  (mongo/insert! :projects p))

(defn-v get-by-name [name]
  (mongo/fetch-one :projects :where {:name name}))

(defn-v get-by-url [url]
  (mongo/fetch-one :projects :where {:vcs_url url}))

(defn get-by-url! [url]
  (assert! (get-by-url url) "Project with url %s not found" url))

(defn ssh-key-for-url [url]
  (-?> url (get-by-url) :ssh_private_key))