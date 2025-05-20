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
@Table(name = "bar")
class Bar(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bar_seq")
    @SequenceGenerator(name = "bar_seq", sequenceName = "bar_seq", allocationSize = 1000)
    @Column(name = "bar_id", nullable = false)
    var barId: Long? = null,

    @Column(name = "bar1", nullable = false)
    val bar1: String,

    @Column(name = "bar2", nullable = false)
    val bar2: String,

    @Column(name = "bar3", nullable = false)
    val bar3: String,

    @OneToMany(mappedBy = "bar")
    val fooBars: Set<FooBar>
)

inline val Bar.id: Long
    get() = barId ?: error("Bar must be persisted before its id is used")