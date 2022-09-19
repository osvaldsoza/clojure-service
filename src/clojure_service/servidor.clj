(ns clojure-service.servidor
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [clojure-service.database :as db]))


(defn inject-store [context]
  (update context :request assoc :store db/store))

(def db-interceptor {
                     :name :db-interceptor
                     :enter inject-store
                     })

(defn criar-tarefa-mapa [uuid nome status]
  {:id uuid :nome nome :status status})

(defn funcao-hello [request]
  {:status 200 :body (str "Hello world " (get-in request [:query-params :name] "every body!"))})

(defn listar-tarefas [request]
  {:status 200 :body @(:store request)})

(defn criar-tarefa [request]
  (let [uuid (java.util.UUID/randomUUID)
        nome (get-in request [:query-params :nome])
        status (get-in request [:query-params :status])
        tarefa (criar-tarefa-mapa uuid nome status)
        store (:store request)]
    (swap! store assoc uuid tarefa)
    {:status 201 :body {:mensagem "Tarefa cadastrada com sucesso"
                        :tarefa   tarefa}}))

(def routes (route/expand-routes
              #{["/hello" :get funcao-hello :route-name :hello-world]
                ["/tarefa" :post [db-interceptor criar-tarefa] :route-name :criar-tarefa]
                ["/tarefa" :get [db-interceptor listar-tarefas] :route-name :listar-tarefas]}))

(def service-map {::http/routes routes
                  ::http/port   9999
                  ::http/type   :jetty
                  ::http/join?  false})
(def server (atom nil))

(defn start-server []
  (reset! server (http/start (http/create-server service-map))))

(defn test-request [verb url]
  (test/response-for (::http/service-fn @server) verb url))

(defn stop-server []
      (http/stop @server))

(defn restart-server []
  (stop-server)
  (start-server))

(start-server)
;(restart-server)
;(println (test-request :get "/hello?name=Osvaldo"))
;(println (test-request :get "/hello"))
(println (test-request :post "/tarefa?nome=Correr Clojure&status=feito"))
(println (test-request :post "/tarefa?nome=Aprender Clojure&status=pendente"))
(println (test-request :post "/tarefa?nome=Lavar o carro&status=pendente"))

(println "Listando tarefas...")
(clojure.edn/read-string (:body (test-request :get "/tarefa")))



