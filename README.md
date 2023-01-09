# specomatic

[![cljdoc badge](https://cljdoc.org/badge/com.primeteach/specomatic)](https://cljdoc.org/d/com.primeteach/specomatic)

A Clojure(script) library to define entities and their relationships via clojure.spec, and to ask questions about them.

See [specomatic-db](https://github.com/primeteach/specomatic-db) database tooling for a concrete library building on this.

## Introduction

The core concept in specomatic is the schema, a nested map that contains all the information about the structure of your data. The functions in the `specomatic.core`, `specomatic.etype-def` and `specomatic.field-def` namespaces are pure functions that take the schema or parts of it as a first argument and answer questions about it.

The functions in the `specomatic.registry` namespace query the clojure.spec registry to generate the schema. They can work with entities and relationships specified using clojure.spec in different ways.
Any part of the schema can be overridden.
It would also be possible to bypass these functions - or even clojure.spec itself - completely and to create the schema yourself. Supporting this is however not a goal of specomatic.

## Entity types

An entity type represents a domain concept. It specifies a map by a set of keys (fields), among them an identity field.

You can define your entity types in different ways:

### Using the `defent` macro

The `defent` macro takes the same arguments as `s/keys` and
* `:id` to specify the id field for the entity type (optional, default is `:entity-type/id`)
* `:field-ns` for a namespace to use key specs from (optional, default is current namespace)

The name of the entity type is added as a namespace to any unqualified keywords.
If no spec exists for a field, but there is one for a keyword in the current namespace (overridden by `:field-ns`)
and with the same name as the field, it is used for the field.

In addition, if the spec for a field is a keyword, the field is `derive`d from the spec to facilitate multimethod dispatch.

```clojure
(require '[clojure.spec.alpha :as s])
(require '[specomatic.registry :as sr])

(s/def :potato/harvest-date #(instance? java.time.LocalDateTime %))
(s/def ::weight integer?)
(s/def ::id integer?)

(sr/defent ::potato
  :req [:harvest-date]
  :opt [:weight])
```

### Using `s/keys`

```clojure
(require '[clojure.spec.alpha :as s])

(s/def :potato/harvest-date #(instance? java.time.LocalDateTime %))
(s/def :potato/weight integer?)
(s/def :potato/id integer?)

(s/keys ::potato
  :req [:potato/harvest-date]
  :opt [:potato/id :potato/weight])
```

This will yield an equivalent schema to the one above defined using `defent`.

### Using other spec predicate functions

To use the spec predicate function of your choice for defining entity types, extend the following multimethods for it:

`specomatic.registry/etype-spec-fn?`

`specomatic.registry/etype-spec-form-fields`

`specomatic.registry/etype-spec-form-required-fields`

## Relationships

Relationships represent connections between domain concepts. They are specified using relational fields: reference and reference collection fields.

Reference fields can contain either the id of the target entity or (part of) the target entity itself.

* 1:n relationships are usually specified by defining a reference field on the "n" side of the relationship. An inverse reference collection field will be derived on the "1" side of the relationship.
* 1:1 relationships are usually specified by defining a reference field on one side of the relationship and overriding the inverse field to be a reference field (instead of a reference collection field).
* m:n relationships are usually specified by defining a reference collection field on the owning side of the relationship. An inverse reference collection field of reference type `:has-many-through` will be derived on the opposite side.

### Defining reference fields

#### Using the `reference` macro

```clojure
(require '[clojure.spec.alpha :as s])
(require '[specomatic.spec :as sp])

(s/def :review/movie (sp/reference ::movie))
```

#### Using `s/or`

```clojure
(require '[clojure.spec.alpha :as s])

(s/def :review/movie (s/or :id integer? :entity ::movie)
```
This is what the `reference` macro expands to.

To use keys of your choice for the `s/or` spec, extend the following multimethods to them:

`specomatic.registry/reference-id-key?`

`specomatic.registry/reference-entity-key?`

#### Using other spec predicate functions

To use the spec predicate function of your choice for defining reference fields, extend the following multimethods for them:

`specomatic.registry/reference-spec-form?`

`specomatic.registry/reference-spec-form-referenced-etype`

### Defining reference collection fields

#### Using the `references` macro

```clojure
(require '[clojure.spec.alpha :as s])
(require '[specomatic.spec :as sp])

(s/def :movie/directors (sp/references ::director))
```

#### Using `s/coll-of` with a reference field spec

```clojure
(require '[clojure.spec.alpha :as s])

(s/def :movie/directors (s/coll-of (s/or :id integer? :entity ::director)))
```
This is what the `references` macro expands to.

#### Using other spec predicate functions

To use the spec predicate function of your choice for defining reference collection fields, extend the following multimethods for them:

`specomatic.registry/reference-coll-spec-form?`

`specomatic.registry/reference-coll-spec-form-referenced-etype`

### Inverse relational fields

For a reference field, the default inverse field is a reference collection field of reference type `:has-many`.

For a reference collection field, the default inverse field is a reference collection field of reference type `:has-many-through`.

These can be overridden as described in the next section.

## Getting the schema

Generate the raw schema (only containing what is explicitely defined by the specs) by passing the namespaces with your specs to the `schema` function:

```clojure
(require '[specomatic.registry :as sr])

(sr/schema ['all 'the 'name 'spaces])
```

Generate the full schema containing defaults and inverse relational fields by passing the namespaces with your specs to the `full-schema` function:

```clojure
(require '[specomatic.registry :as sr])

(sr/full-schema ['all 'the 'name 'spaces]))
```

To override defaults and inverse fields, pass your overrides to the full-schema function as a second argument:

```clojure
(require '[specomatic.registry :as sr])

(sr/full-schema ['all 'the 'name 'spaces] overrides)
```

## Schema structure

The schema is a map of entity types to entity type definitions:

```clojure
{::actor    ...
 ::director ...
 ::movie    ...
 ::review   ...
 ::user     ...}
```

### Entity type definitions

Entity type definitions have the following shape:

```clojure
{;; set of fields (keywords) that are part of the display name of the entity type.
 :display-name-fields #{:movie/title}}
 ;; field definitions, see below
 :field-defs           ...
 :id-field            :movie/id
 :required-fields     #{:movie/title :movie/release-year}}
```

### Field type definitions

Simple (non-relational) field type definitions have the following shape:

```clojure
{:kind      ::sf/simple
 ;; the spec itself if it is a keyword, a description (from `s/describe`) if it is not
 :dispatch  'string? }
```

Relational fields have the following shape:

```clojure
{;; the inverse field in the schema (only defined for the entity type that does not own the relationship)
 :inverse-of     :review/movie
 ;; the kind of the field, ::sf/reference or ::sf/reference-coll (extensible)
 :kind           ::sf/reference-coll
 ;; the reference type of the field, can be :has-one, :has-many, :has-many-through
 :reference-type :has-many
 ;; the target entity type of the reference
 :target         :schema/review
 ;; the reference field on the opposite side of the relationship, if available.
 :via            :review/movie}
```

## Thoughts and plans

* Support non-integer ids
* Maybe use spec.tools/visitor to generate the schema
