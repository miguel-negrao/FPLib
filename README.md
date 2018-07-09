FPLib
=====

Functional Programming library for SuperCollider

Requirements
===========

* SuperCollider 3.9.3
* JITLibExtensions quarks.
* [Modality Library](https://github.com/ModalityTeam/Modality-toolkit)

Instalation
==========

Move this folder to the SuperCollider 'Extensions' folder.

Overview
========

Making full use of this library requires knowledge of modern Functional Programming concepts as implemented for instance in Haskell, specially familiarity with functors, applicative functors, monoids and monads.

I recommend reading a book such as ["Learn you a Haskell for greater good"](http://http://learnyouahaskell.com/).

  The Library implements an experimental type class system, where the functions of the type classes are defined in Object and to "implement" a type class a SuperCollider class has to overload the corresponding functions (i.e. >>= or collect). This doesn't allow for more advanced uses of type classes but allows defining functions that are available to a specific type class, by defining a corresponding method in Object (i.e. sequence depends on traverse, >>=| depends on >>=). If one calls >>=| on a class that is not a monad (doesn't implement >>= ) an error is thrown.
  Some methods such as sequence cannot be used like in Haskell because SuperCollider doesn't have the sophisticated type system to infere the right class to use in some circunstances. In those cases o type hint must be given:

    [].sequence(Option)

Without this type hint, there would be no way to know in what class to wrap the empty array.

# Type Classes:

## Functor


  * needs: collect

## Applicative Functor (AF):

  * needs: ```<*>```, ```*makePure```
  * makes available: ```<*```, ```*>```, ```<%>```, ```sequence```, ```pure```

## Monad


  * needs: ```>>=```, ```*makePure``` (we don't distinguish between AF's pure and Monad's return)
  * makes available: ```>>=|```, ```sequenceM```, ```pure```

## Monoid

  * needs: ```|+|```, ```*zero```

## Traverse

  * needs: ```traverse```
  * makes available: ```collectTraverse```, ```disperse```

# Instances of Type Classes:

* IO
* Option
* Validation
* Promise
* Reader
* ST
* WriterReader
* RWST

# New collections:

* LazyList
* Tuple(n)

# [Functional Reactive Programming](http://en.wikipedia.org/wiki/Functional_reactive_programming)

Classes:
```FPSignal```, ```EventStream``` and their children.

These classes are based on [scala](http://reactive-web.tk/) and [haskell](http://www.haskell.org/haskellwiki/Reactive-banana) implementations of FRP.

A monad for constructing the event processors graph is available which is compiled into a ```EventNetwork``` which can be started and stopped at will. The network description itself has this pseudo type:

```
//networkDescription : Writer( Unit, Tuple3([IO(IO())], [EventStream[IO[Unit]]], [IO] ) )
  //                                     eventHandlers         reactimates        IOLater
```

#More info

See the help files, including the reference page, the examples directory and also there are some blog posts relating to FP Lib at http://www.friendlyvirus.org/miguelnegrao/category/code/.




