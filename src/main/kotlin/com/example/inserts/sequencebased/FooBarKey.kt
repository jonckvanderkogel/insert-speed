package com.example.inserts.sequencebased

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class FooBarKey(
    @Column(name = "foo_id")
    val fooId: Long,

    @Column(name = "bar_id")
    val barId: Long
)