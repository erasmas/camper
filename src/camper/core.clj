(ns camper.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [etaoin.api :as e]
            [etaoin.keys :as k])
  (:gen-class))

(defn- download-url-to-file
  [url path]
  (with-open [in (io/input-stream url)
              out (io/output-stream path)]
    (io/copy in out)))

(defn- ensure-download-dir-created
  [path]
  (.mkdirs (io/file path)))

(defn login
  [driver {:keys [user password]}]
  (e/go driver "https://bandcamp.com/login")
  (e/wait-visible driver {:id :loginform})
  (e/fill driver {:tag :input :name :username-field} user)
  (e/fill driver {:tag :input :name :password-field} password)
  (e/fill driver {:tag :input :name :password-field} k/enter))

(defn query-download-urls
  [driver]
  (e/js-execute driver
    "return Array.from(document.querySelectorAll('span.redownload-item a')).map(el => el.href);"))

(defn query-collection-count
  [driver]
  (e/wait-visible driver {:tag :span :class :count} {:timeout 30})
  (-> (e/get-element-text driver {:tag :span :class "count"})
      (Integer/valueOf)))

(defn download-album
  [driver download-dir url]
  (try
    (let [download-query {:css "span.download-title > a.item-button"}]
    (e/go driver url)
    (e/wait-visible driver download-query {:timeout 90})
    (let [title (e/get-element-text driver {:tag :div :class :title})
          artist (-> (e/get-element-text driver {:tag :div :class :artist})
                     (str/replace "by " ""))
          url (e/get-element-attr driver download-query "href")
          download-dir (format "%s/%s" download-dir artist)
          download-path (format "%s/%s.zip" download-dir title)
          f (io/file download-path)]
      (if (.exists f)
        (log/infof "Skipping downloaded album at %s" download-path)
        (do
          (log/infof "Downloading '%s' by '%s'\n" title artist)
          (ensure-download-dir-created download-dir)
          (download-url-to-file url download-path)))))
    (catch Exception _
      (log/errorf "Failed to download %s" url))))

(defn get-album-urls
  [driver]
  (let [collection-count (query-collection-count driver)
        max-page 1000]
    (log/infof "Scrapping urls for %d albums\n" collection-count)
    (e/click driver {:tag :button :class :show-more})
    (loop [page 0
           collection-urls (query-download-urls driver)]
     (log/infof "Scrapped %d album urls ...\n" (count collection-urls))
     (if (or (>= (count collection-urls) collection-count)
             (== page max-page))
       collection-urls
       (do
         (e/scroll-down driver 1000)
         (e/wait 1)
         (recur (inc page) (query-download-urls driver)))))))

(def cli-options
  [["-u" "--user USER" "Username"]
   ["-p" "--password PASSWORD" "Password"]
   ["-d" "--download-dir DOWNLOAD_DIR" "Download directory"]])

(defn -main
  [& args]
  (let [{:keys [options] :as parsed} (parse-opts args cli-options)
        {:keys [download-dir]} options
        driver (e/firefox {:download-dir download-dir})]
    (try
      (login driver options)
      (let [urls (get-album-urls driver)]
        (log/infof "Found %d albums\n" (count urls))
        (doseq [url urls]
          (download-album driver download-dir url)))
      (finally
        (e/quit driver)))))

;; TODO Check downloaded archives for errors
;; TODO Select download format
