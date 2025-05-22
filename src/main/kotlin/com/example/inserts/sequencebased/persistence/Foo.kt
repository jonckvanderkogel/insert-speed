package com.example.inserts.sequencebased.persistence

import com.example.inserts.saveAndReturn
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

@Entity
@Table(name = "foo")
class Foo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "foo_seq")
    @SequenceGenerator(name = "foo_seq", sequenceName = "foo_seq", allocationSize = 1000)
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

inline val Foo.id: Long
    get() = fooId ?: error("Foo must be persisted before its id is used")

interface FooRepository : JpaRepository<Foo, Long>

@Service
class FooService(
    private val repository: FooRepository
) {
    fun batchInsert(records: List<Foo>): Pair<Long, List<Foo>> = saveAndReturn(
        records,
        repository::saveAll
    )
}