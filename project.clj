(defproject yuggoth "0.1.0-SNAPSHOT"
            :description "personal blog engine"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3" :exclude [org.clojure/clojure]]
                           [markdown-clj "0.8"]
                           [org.clojure/java.jdbc "0.2.3"]
                           [net.sf.jlue/jlue-core "1.3"]
                           [hsqldb/hsqldb "1.8.0.10"]]
	    :dev-dependencies [[lein-ring "0.7.1"]]
            :ring {:handler yuggoth.server/handler}
            :main yuggoth.server)

