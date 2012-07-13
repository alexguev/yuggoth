(ns yuggoth.models.db
  (:require [clojure.java.jdbc :as sql])
  (:import java.sql.Timestamp java.util.Date))

(def ^{:private true} db 
  {:classname "org.hsqldb.jdbcDriver"
   :subprotocol "hsqldb"
   :subname "yog.db"
   :create true})

(defn drop-table
  "drops the supplied table from the DB, table name must be a keyword
eg: (drop-table :users)"
  [table]
  (try
   (sql/with-connection db (sql/drop-table table))
   (catch Exception _)))

(defn db-read
  "returns the result of running the supplied SQL query"
  [query & args]
  (sql/with-connection 
    db
    (sql/with-query-results res (vec (cons query args)) (doall res))))

(defn transaction
  "runs a function with the supplied arguments in an SQL transaction
eg: (transaction add-user email firstname lastname password)"
  [f & args]
  (sql/with-connection db
    (sql/transaction
      (apply f args))))

;file management

(defn create-file-table []
  (sql/create-table
    :file
    [:type "varchar(20)"]
    [:name "varchar(50)"]
    [:data "LONGVARBINARY"]))

(defn to-byte-array [x]  
  (with-open [input (new java.io.FileInputStream x)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))

(defn fix-file-name [filename]
  (.replaceAll filename "[^a-zA-Z0-9-\\.]" ""))

(defn store-file [{:keys [tempfile filename content-type]}]
  (sql/with-connection 
    db
    (sql/update-or-insert-values
      :file
      ["name=?" filename]
      {:type content-type :name (fix-file-name filename) :data (to-byte-array tempfile)})))

(defn list-files []
  (map :name (db-read "select name from file")))

(defn delete-file [name]
  (sql/with-connection db (sql/delete-rows :file ["name=?" name])))

(defn get-file [name]
  (first (db-read "select * from file where name=?" name)))

;;blog table management
(defn create-blog-table []
  (sql/create-table
    :blog
    [:id :int "GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) not null"]
    [:time :timestamp]
    [:title "varchar(100)"]
    [:content "LONGVARCHAR"]
    [:author "varchar(100)"]))

(defn update-post [id title content]
  (sql/with-connection
    db
    (sql/update-values
      :blog
      ["id=?" id]
      {:id id :title title :content content})))

(defn get-posts [& [limit]]  
  (db-read (str "select " (if limit (str "limit 0 " limit)) " id, time, title from blog")))

(defn get-post [id]  
  (first (db-read "select * from blog where id=?" id)))

(defn store-post [title content author]
  (sql/with-connection 
    db
    (sql/insert-values
      :blog
      [:time :title :content :author]
      [(new Timestamp (.getTime (new Date))) title content author])))

(defn delete-post [id]
  (sql/with-connection 
    db
    (sql/delete-rows :blog ["id=?" id])))

(defn get-last-post [] 
  (first (db-read "select * from blog where id = (Select max(id) from blog)")))

(defn create-comments-table []
  (sql/create-table
    :comment
    [:blogid :int]
    [:time :timestamp]    
    [:content "LONGVARCHAR"]
    [:author "varchar(100)"]))

(defn add-comment [blog-id content author]
  (sql/with-connection 
    db
    (sql/insert-values
      :comment
      [:blogid :time :content :author]
      [blog-id (new Timestamp (.getTime (new Date))) content author])))

(defn get-comments [blog-id]
  (db-read "select * from comment where blogid=?" blog-id))

;;admin table management
(defn create-admin-table []
  (sql/create-table
    :admin   
    [:title "varchar(100)"]
    [:style "varchar(50)"]
    [:about "LONGVARCHAR"]
    [:handle "varchar(100)"]
    [:pass   "varchar(20)"]
    [:email  "varchar(50)"]))

(defn set-admin [admin]
  (sql/with-connection db (sql/insert-record :admin admin)))

(defn update-admin [admin]
  (sql/with-connection 
    db
    (sql/update-values :admin ["handle=?" (:handle admin)] admin)))

(defn reset-blog []  
  (sql/with-connection 
    db    
    (drop-table :admin)
    (drop-table :blog)
    (drop-table :comments)
    (drop-table :file)
    (create-admin-table)
    (create-blog-table)
    (create-comments-table)
    (create-file-table)
    nil))

(defn get-admin []
  (try (first (db-read "select * from admin"))
    (catch java.sql.SQLException ex
      (when (.contains (.getMessage ex) "Table not found")
        (reset-blog)))))

(defn export []
  {:admin (dissoc (get-admin) :pass)
   :posts
   (vec (for [blog (db-read "select * from blog")]
          (assoc blog :comments (vec (db-read "select * from comment where blogid = ?" (:id blog))))))})
