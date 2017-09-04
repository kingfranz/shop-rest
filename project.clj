(defproject shop-rest "0.1.0"
  
	:description "Shopping REST API"
	:url "http://soahojen.se"
	:min-lein-version "2.0.0"
 
	:dependencies [
        [org.clojure/clojure "1.9.0-alpha17"]
        [ring "1.6.2"]
        [ring/ring-json "0.4.0"]
       	[org.clojure/spec.alpha "0.1.123"]
        [ring/ring-defaults "0.3.1"]
        [clj-time "0.14.0"]
        [environ "1.1.0"]
        [com.novemberain/monger "3.1.0"]
        [prone "1.1.4"]
        [cheshire "5.8.0"]
        [ring-logger "0.7.7"]
        [com.cemerick/friend "0.2.3"]
        [com.taoensso/timbre "4.10.0"]
        [compojure "1.6.0"]]
 
   	:jvm-opts ["-Dclojure.spec.compile-asserts=true"]

   	:plugins [
        [lein-ring "0.11.0"]
  		[lein-pprint "1.1.2"]
     	[lein-environ "1.1.0"]]
    
	:ring {:handler shop-rest.handler/app}
	:profiles
	{:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
	                    [ring/ring-mock "0.3.0"]]}})
