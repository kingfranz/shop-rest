(ns shop-rest.handler
  	(:require 	(compojure 					[core       	:refer [defroutes]]
  											[route      	:as route]
            								[handler    	:as handler])
            	(shop-rest					[db        		:as db]
                    						[logfile        :refer :all]
            								[routes    		:as sr])
            	(taoensso 					[timbre     	:as log])
            	(taoensso.timbre.appenders 	[core 			:as appenders])
            	(cemerick 					[friend     	:as friend])
            	(cemerick.friend 			[workflows 		:as workflows]
                             				[credentials 	:as creds])
            	(environ 					[core 			:refer [env]])
            	(clj-time 	 				[core        	:as t]
            				 				[local       	:as l]
            				 				[coerce      	:as c]
            				 				[format      	:as f]
            				 				[periodic    	:as p])
            	(ring 						[logger			:as logger])
            	(ring.middleware 			[defaults   	:refer :all]
                              				[json           :refer :all]
            								[reload     	:as rmr]
            								[stacktrace 	:as rmst])
            	(ring.middleware.session 	[store  		:as store]
            								[cookie 		:as cookie]
            								[memory			:as mem])
            	(ring.util 					[response   	:as response])
            	(ring.adapter 				[jetty			:as ring]))
  	(:gen-class))

(defroutes app-routes
  	sr/api-routes
  	(route/not-found "No matching route found"))

(def my-defaults
  	(-> api-defaults
        (assoc-in [:security :anti-forgery] false)))

(defn dirty-fix
	[x]
	(setup-log)
	x)

(def app
  	(-> app-routes
        (dirty-fix)
        (wrap-json-body {:keywords? true})
        (wrap-json-response)
        (wrap-defaults my-defaults)
        ))
