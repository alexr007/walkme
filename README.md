### short description

`HTTP GET` requests to `http://localhost:8080/get-fortune` being forwarded to:
  - [`http://localhost:9551/get-fortune`](http://localhost:9551/get-fortune)
  - [`http://localhost:9552/get-fortune`](http://localhost:9552/get-fortune)
  - [`http://localhost:9553/get-fortune`](http://localhost:9553/get-fortune)

depending on their availability.
Dependent services availability being tracked internally with the help of service implemented.
All requests can't be served immediately are buffered.
A global timeout is 10 seconds, can be adjusted in `resources/application.conf` file.

### lo run load balancer

```shell script
sbt run
```

### prerequisites

services at `9551`, `9552`, `9553` must be started beforehand according to requirements provided [here](req/README.md). 

### technical description
- [`BalancerApp`](src/main/scala/BalancerApp.scala) - application starter (contains `main` function)
- [`JokeRoutes`](src/main/scala/JokeRoutes.scala) - mapping `/` to our service
- [`LoadBalancer`](src/main/scala/LoadBalancer.scala) - framework-agnostic implementation.
There is only one limitation: requestMaker is a function: `A => Future[B]`.
responseHandler is a function: `B => Any`
- [`ForwardLogic`](src/main/scala/ForwardLogic.scala) - wiring `LoadBalancer` to `JokeRoutes` according to Akka-Http Actor Implementation
- [`build.sbt`](build.sbt) - Scala project configuration 
