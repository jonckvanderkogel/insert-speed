package com.example.inserts.sequencebased

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "foo_x_bar")
data class FooBar(
    @EmbeddedId
    val fooBarId: FooBarKey,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("fooId")
    @JoinColumn(name = "foo_id")
    val foo: Foo,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("barId")
    @JoinColumn(name = "bar_id")
    val bar: Bar
)