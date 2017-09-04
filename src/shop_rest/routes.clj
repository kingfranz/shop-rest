(ns shop-rest.routes
  	(:require 	(clojure 		 		[string   		:as str]
  								 		[pprint   		:as pp])
             	(clojure.tools.reader	[edn            :as edn])
            	(ring.util 		 		[response 		:as ring])
            	(compojure 		 		[core     		:as cc]
              					 		[route    		:as route])
            	(clojure.java 			[io         	:as io])
             	(cheshire				[core 			:refer :all])
				(cemerick 		 		[friend     	:as friend])
            	(cemerick.friend		[workflows 		:as workflows]
                             	 		[credentials	:as creds])
            	(shop-rest 				[recipes   		:refer :all]
              					 		[items    		:refer :all]
              					 		[menus    		:refer :all]
              					 		[tags     		:refer :all]
              					 		[lists    		:refer :all]
              					 		[projects 		:refer :all]
            					   		[users         	:refer :all]
            		 			 		[db       		:refer :all])))

;;-----------------------------------------------------------------------------

(defn- get-body
  	[req]
   	(cond
      	(= (:content-type req) "application/json")
      		(with-open [xin (io/reader (:body req))]
          		(parse-stream xin true))
       	(= (:content-type req) "application/edn")
          	(with-open [xin (io/reader (:body req))]
          		(edn/read xin))
        :else
           	(throw (ex-info "unknown application type" {:status 406}))))

(defn- auth
  	[lvl func & args]
   	(try
      	;(friend/authorize #{lvl} (apply func args))
        (apply func args)
       	(catch Throwable e
          	(if-let [info (ex-data e)]
             	{:status (:status info)}
              	{:status 500}))))

;;-----------------------------------------------------------------------------

(cc/defroutes api-routes
 	;---------- MENU --------------------
	(cc/GET "/menus" request
	    (auth :user get-menus (-> request :params :start-date)
               				  (-> request :params :end-date)))

	(cc/POST "/menus" request
	    (auth :user new-menu (get-body request)))

	(cc/PUT "/menus" request
	    (auth :user update-menu (get-body request)))

;	(cc/DELETE "/menus/:date" request
;	    (auth :user (delete-menu (-> request :params :date))))

	(cc/PUT "/menus/recipe" request
	    (auth :user add-recipe-to-menu (-> request :params :menu-date)
           							   (-> request :params :recipe-id)))

	(cc/DELETE "/menus/recipe" request
	    (auth :user remove-recipe-from-menu (-> request :params :menu-date)))

 	;---------- RECIPE --------------------
	(cc/GET "/recipes" request
	    (auth :user get-recipes))

	(cc/GET "/recipes/names" request
	    (auth :user get-recipe-names))

	(cc/GET "/recipes/:id" request
	    (auth :user get-recipe (-> request :params :id)))

	(cc/POST "/recipes" request
	    (auth :user new-recipe (get-body request)))

	(cc/PUT "/recipes" request
	    (auth :user update-recipe (get-body request)))

;	(cc/DELETE "/recipes/:id" request
;	    (auth :user (delete-recipe (-> request :params :id))))

 	;---------- PROJECTS --------------------
	(cc/GET "/projects" request
	    (auth :user get-projects))

	(cc/GET "/projects/active" request
	    (auth :user get-active-projects))

	(cc/GET "/projects/:id" request
	    (auth :user get-project (-> request :params :id)))

	(cc/POST "/projects" request
	    (auth :user new-project (get-body request)))

	(cc/PUT "/projects/finish/:id" request
	    (auth :user finish-project (-> request :params :id)))

	(cc/PUT "/projects/unfinish/:id" request
	    (auth :user unfinish-project (-> request :params :id)))

	(cc/PUT "/projects" request
	    (auth :user update-project (get-body request)))

;	(cc/DELETE "/projects/:id" request
;	    (auth :admin delete-project (-> request :params :id)))

	(cc/PUT "/projects/clear" request
	    (auth :user clear-projects))
	
 	;---------- ITEMS --------------------
	(cc/GET "/items" request
	    (auth :user get-items))

	(cc/GET "/items/names" request
	    (auth :user get-item-names))

	(cc/GET "/items/:id" request
	    (auth :user get-item (-> request :params :id)))

	(cc/POST "/items" request
	    (auth :user new-item (get-body request)))

	(cc/PUT "/items" request
	    (auth :admin update-item (get-body request)))

	(cc/DELETE "/items/:id" request
	    (auth :admin delete-item (-> request :params :id)))

 	;---------- LISTS --------------------
	(cc/GET "/lists" request
	    (auth :user get-lists))

	(cc/GET "/lists/names" request
	    (auth :user get-list-names))

	(cc/GET "/lists/counts" request
	    (auth :user get-lists-with-count))

	(cc/GET "/lists/:id" request
	    (auth :user get-list (-> request :params :id)))

	(cc/POST "/lists" request
	    (auth :admin new-list (get-body request)))

	(cc/PUT "/lists" request
	    (auth :admin update-list (get-body request)))

	(cc/DELETE "/lists/:id" request
	    (auth :admin delete-list (-> request :params :lid)))

 	;---------- LIST-ITEMS --------------------
	(cc/PUT "/lists/items/add" request
	    (auth :user item->list (-> request :params :listid)
				  	  		   (-> request :params :itemid)
              				   1))

	(cc/PUT "/lists/items/clean" request
	    (auth :user del-finished-list-items (-> request :params :listid)))

	(cc/PUT "/lists/items/up" request
	    (auth :user item->list (-> request :params :listid)
					 		   (-> request :params :itemid)
         					   1))

	(cc/PUT "/lists/items/down" request
	    (auth :user item->list (-> request :params :listid)
				   	   		   (-> request :params :itemid)
              				   -1))

	(cc/PUT "/lists/items/done" request
	    (auth :user finish-list-item (-> request :params :listid)
				   	   		  		 (-> request :params :itemid)))

	(cc/PUT "/lists/items/undo" request
	    (auth :user unfinish-list-item (-> request :params :listid)
				   	   		  		   (-> request :params :itemid)))

 	;---------- USERS --------------------
  	(cc/GET "/users" request
  		(auth :admin get-users))

  	(cc/GET "/users/:userid" request
  		(auth :admin get-user-by-id (-> request :params :userid)))

	(cc/POST "/users" request
	    (auth :admin create-user (get-body request)))

  	(cc/GET "/users/names/:username" request
  		(auth :admin get-user (-> request :params :username)))

;	(cc/DELETE "/users/:userid" request
;	    (auth :admin delete-user (-> request :params :userid)))

  	(cc/PUT "/users/passwd/:userid" request
  		(auth :admin set-user-password (-> request :params :userid)
                 		 		       (get-body request)))

  	(cc/PUT "/users/roles/:userid" request
  		(auth :admin set-user-roles (-> request :params :userid)
                 		 		    (get-body request)))

	(cc/PUT "/users/props/:userid" request
		(auth :user set-user-property (-> request :params :userid)
                		  		      (get-body request)))

 	;---------- TAGS --------------------
  	(cc/GET "/tags" request
  		(auth :user get-tags))

  	(cc/GET "/tags/names" request
  		(auth :user get-tag-names))

  	(cc/GET "/tags/:id" request
  		(auth :user get-tag (-> request :params :id)))

	(cc/DELETE "/tags/:id" request
	    (auth :admin delete-tag (-> request :params :id)))

	(cc/DELETE "/tags/all/:id" request
	    (auth :admin delete-tag-all (-> request :params :id)))

	(cc/POST "/tags" request
	    (auth :user new-tag (get-body request)))

	(cc/PUT "/tags/:id" request
	    (auth :admin update-tag (-> request :params :id)
              				    (get-body request))) ; new name
	)

;;-----------------------------------------------------------------------------
