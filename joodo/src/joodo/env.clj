(ns ^{:doc "This namespace holds functions that read the current environment that the application is in (according to the system properties)."}
  joodo.env
  (:import
    [filecabinet FileSystem]))

(def ^{:dynamic true
       :doc "Holds information about the current environment. That data can be
  retrieved with the following syntax: (env :joodo-env)"}
  *env* {:joodo-env (or (System/getProperty "joodo.env") "development")})

(defn alter-env!
  "Alter the joodo environment by calling the given function on *env* with the given args."
  [f & args]
  (alter-var-root (var *env*)
    #(apply f % args)))

(defn set-env! [env]
  "Resets the value of joodo's environment."
  (alter-var-root (var *env*)
    (fn [_] env)))

(defn env
  "Retrieves an entry from the current environment. nil if not found"
  [key]
  (or
    (get *env* (keyword key))
    (get *env* (name key))
    (System/getProperty (name key))))

(defn development-env?
  "Returns true if the application is running in the
  development environment and false otherwise"
  []
  (= "development" (env :joodo-env)))

(defn production-env?
  "Returns true if the application is running in the
  production environment and false otherwise"
  []
  (= "production" (env :joodo-env)))

(defn- read-src [src-path src-content]
  (let [rdr (-> (java.io.StringReader. src-content) (clojure.lang.LineNumberingPushbackReader.))
        file (java.io.File. src-path)
        parent-path (.getParent file)
        src-filename (.getName file)]
    (clojure.lang.Compiler/load rdr parent-path src-filename)))

(defn load-config
  "Loads a joodo config file."
  [ns path]
  (let [src (.readTextFile (FileSystem/instance) path)]
    (binding [*ns* ns]
      (use 'clojure.core)
      (use 'joodo.env)
      (read-src path src))))

(defn load-configurations-unchecked
  "Loads joodo configuration without checking if it should (load-config?)."
  []
  (let [environment (System/getProperty "joodo.env")
        env-ns (create-ns (gensym (str "joodo.config-")))]
    (load-config env-ns "config/environment.clj")
    (load-config env-ns (format "config/%s.clj" environment))))

(defn load-config?
  "Checks the joodo.ignore.config System property for true or false"
  []
  (if-let [switch (System/getProperty "joodo.ignore.config")]
    (= "false" (.toLowerCase switch))
    true))

(defn load-configurations
  "Loads joodo configurations unless load-config? says not to."
  []
  (when (load-config?) (load-configurations-unchecked)))

