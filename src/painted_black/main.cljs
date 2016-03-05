(ns painted-black.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [<!]]
            [cljs-http.client :as http]
            [clojure.string :as string]
            [clojurewerkz.balagan.core :as balagan]
            [dommy.core :as dommy]))

(def settlement
  (str "疲れからか、不幸にも黒塗りの高級車に追突してしまう。"
       "後輩をかばいすべての責任を負った三浦に対し、車の主、"
       "暴力団員谷岡に言い渡された示談の条件とは・・・。"))

(defn fetch [params xform]
  (http/jsonp "https://ja.wikipedia.org/w/api.php"
              {:channel (async/chan 1 xform)
               :query-params (assoc params :format "json")}))

(defn search-titles [query]
  (fetch {:action "query", :list "search", :srsearch query}
         (map #(balagan/select % [:body :query :search :* :title]))))

(defn fetch-article [title]
  (fetch {:action "query", :prop "revisions", :rvprop "content", :titles title}
         (map #(nth (balagan/select % [:body :query :pages :* :revisions 0 :*]) 2))))

(defn summary-of [source]
  (some-> (re-find #"(?:あらすじ|ストーリー)\s?={2,}([\s\S]+?)==" source)
          second
          (string/replace #"\[\[(.+?)\|.+?\]\]" "$1")
          (string/replace #"\{\{.+\}\}|<.+/>" "")
          (string/replace #"'{2,}|\[\[|\]\]|#|\s" "")))

(defn generate [source]
  (let [result (re-find #"^(?:.+?。){0,2}.+?[ぁ-ん]、" source)]
    (str result settlement)))

(go
  (dommy/listen! (dommy/sel1 :#query) :keydown
    #(when (= (.-keyCode %) 13)
      (.assign js/location (str "?q=" (dommy/value (.-target %))))))

  (let [query (->> (.-href js/location) js/decodeURIComponent (re-find #"\?q=(.+)"))]
    (when (not-empty query)
      (if-let [title (-> (<! (search-titles query)) first)]
        (do
          (dommy/set-value! (dommy/sel1 :#query) title)
          (if-let [content (summary-of (<! (fetch-article title)))]
            (dommy/set-text! (dommy/sel1 :#content) (generate content))
            (dommy/set-text! (dommy/sel1 :#content) "（あらすじが見つから）ないです")))
        (dommy/set-text! (dommy/sel1 :#content) "（作品が見つから）ないです")))))
