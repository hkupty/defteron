# Defteron

> constituting number two in a sequence; coming after the first in time or order; 2nd.

## What is this library?

It's an abstraction on top of Google protobuf so java Protobuf objects can be interpreted as Clojure maps.

## How to use this library?

So, given the following Protobuf file:

```proto

enum Enum {
  VALUE = 0;
}

message Object {
  string key = 1;
  Enum other_key = 2;
}
```

Applying `defteron.core/proto->map` on it will transform it to Clojure data:

```clj
(require [defteron.core :as d])

(d/proto->map your-proto-obj) ;; => {:key "value" :other-key :your.namespaced.enum/value}
```


## How stable is this library?

It's alpha quality yet. Help wanted :)
