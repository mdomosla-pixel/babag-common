(ns babag-common.database
  (:require
    [clojure.java.jdbc :as jdbc]
    [again.core :as again]
    [clojure.java.jdbc :as jdbc]))

(def backoff-ms 1000)
(def retry-count 10)
(def default-retry-strategy
  (again/max-retries
    retry-count
    (again/constant-strategy backoff-ms)))


(defn put-status [datasource record status
                  & {:keys [retry-strategy priority] :or {retry-strategy default-retry-strategy
                                                          priority (:priority record 1)}}]
  (again/with-retries retry-strategy
    (jdbc/with-db-connection [conn {:datasource datasource}]
      (jdbc/execute! conn
                     [(str "INSERT INTO babag_statuses("
                           "id, sms_user, from_name, to_number, "
                           "provider, status, insert_date, priority"
                           ") VALUES ("
                           "?,?,?,?,?,?,CURRENT_TIMESTAMP,?)"
                           " ON CONFLICT (id) DO UPDATE "
                           " SET sms_user=?, from_name=?, to_number=?,"
                           " provider=?, status=?, insert_date=CURRENT_TIMESTAMP,"
                           " priority=?")
                      (:id record) (:user record) (:from record) (:to record)
                      (name (:provider record)) (name status) priority
                      ; upsert update
                      (:user record) (:from record) (:to record)
                      (name (:provider record)) (name status) priority]))))

(defn get-status [datasource id
                  & {:keys [retry-strategy]
                     :or {retry-strategy default-retry-strategy}}]
  (jdbc/with-db-connection [conn {:datasource datasource}]
    (:status (first (jdbc/query conn
      [(str "SELECT status FROM babag_statuses"
            " WHERE id=?") id])))))

(defn update-status! [datasource id status
                      & {:keys [retry-strategy]
                         :or {retry-strategy default-retry-strategy}}]
  (again/with-retries retry-strategy
    (jdbc/with-db-connection [conn {:datasource datasource}]
      (jdbc/execute! conn ["SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;"])
      (jdbc/execute! conn
                     [(str "INSERT INTO babag_statuses(status,id)"
                           " VALUES(?,?) "
                           " ON CONFLICT (id) DO UPDATE "
                           " SET status=?")
                      (name status) id (name status)]))))

(defn health-check [datasource]
  (jdbc/with-db-connection [conn {:datasource datasource}]
    (= 1 (:item (first (jdbc/query conn ["SELECT 1 AS item"]))))))
