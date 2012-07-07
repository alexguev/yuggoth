(ns yuggoth.views.common
  (:use hiccup.element hiccup.form
        [noir.core]
        [noir.validation :as vali]        
        [hiccup.page :only [include-css include-js html5]])
  (:require [yuggoth.views.util :as util]
            [yuggoth.models.db :as db]
            [noir.session :as session]))

(defn header []
  [:div.header [:h1 [:div.site-title  (:title (db/get-admin))]]])

(defn menu []
  [:div.menu 
   (into
     (if (session/get :admin) 
       [:ul.menu-items          
        [:li (link-to "/logout" "logout")]
        [:li (link-to "/profile" "profile")]
        [:li (link-to "/upload" "upload")]
        [:li#new-post (link-to "/make-post" "New post")]]
       [:ul.menu-items
        [:li (link-to "/login" "login")]])     
     [[:li#about (link-to "/about" "About")]
      [:li#archives (link-to "/archives" "Archives")]
      [:li#home (link-to "/" "Home")]])])

(defn sidebar []
  [:div.sidebar
   [:h2 "Recent posts"]
   (into [:ul]
         (for [{:keys [id time title]} (reverse (sort-by :time (db/get-posts 10)))]
           [:li 
            (link-to (str "/blog/" id)
                     title
                     [:div.date (util/format-time time)])]))])

(defn footer []
  [:div.footer
   [:p (str "Copyright (C) 2012 " (:handle (db/get-admin)) " - Powered by: ") 
    (link-to "http://github.com/yogthos/yuggoth" "Yuggoth")]])

(defpartial layout [title & content]
  (let [html-title (if (string? title) title (:title title))
        title-elements (when (map? title) (:elements title))]    
    (html5
      [:head
       [:title html-title]
       (include-css (util/get-css)
                    "/css/jquery.alerts.css")
       (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"
                   "/js/jquery.alerts.js"
                   "/js/site.js")]      
      [:body
       (hidden-field "selected" 
                     (condp = html-title
                       "Archives" "#archives"
                       "Login" "#login"
                       "New post" "#new-post"
                       "#home"))
       [:div.container
        (header)
        (menu)
        [:div.contents 
         [:div.post
          [:div.entry-title [:h2 html-title title-elements]]
          [:div.entry-content content]]
         (sidebar)]          
        (footer)]])))
