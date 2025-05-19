package com.example.inserts.sequencebased

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "foo")
class Foo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "foo_seq")
    @SequenceGenerator(name = "foo_seq", sequenceName = "foo_seq", allocationSize = 10000)
    @Column(name = "foo_id", nullable = false)
    var fooId: Long? = null,

    @Column(name = "foo1", nullable = false)
    val foo1: String,

    @Column(name = "foo2", nullable = false)
    val foo2: String,

    @Column(name = "foo3", nullable = false)
    val foo3: String,

    @OneToMany(mappedBy = "foo")
    val fooBars: Set<FooBar>
)