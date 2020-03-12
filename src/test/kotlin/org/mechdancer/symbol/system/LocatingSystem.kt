package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.minus
import org.mechdancer.symbol.optimize.ConditionCollector
import org.mechdancer.symbol.optimize.fastestBatchGD
import org.mechdancer.symbol.optimize.firstOrLast
import org.mechdancer.symbol.optimize.recurrence
import org.mechdancer.symbol.sum
import org.mechdancer.symbol.toDouble
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.random.Random.Default.nextDouble

class LocatingSystem(val maxMeasure: Double) {
    var painter: (Map<Beacon, Vector3D>) -> Unit = {}

    private val positions =
        hashMapOf<Beacon, SortedMap<Long, Vector3D>>()

    private val relations =
        hashMapOf<Position, SortedSet<Position>>()

    private val measures =
        hashMapOf<Pair<Position, Position>, MutableList<Double>>()

    operator fun set(a: Position, b: Position, t: Long, d: Double) {
        require(a != b)
        positions.update(a) { Vector3D(nextDouble(), nextDouble(), nextDouble()) }
        positions.update(b) { Vector3D(nextDouble(), nextDouble(), nextDouble()) }
        relations.update(a, { it += b }, { sortedSetOf(b) })
        relations.update(b, { it += a }, { sortedSetOf(a) })
        measures.update(if (a < b) a to b else b to a, { it += d }, { mutableListOf(d) })
    }

    /** 使用所有已知的测量数据，优化所有坐标 */
    fun optimize() {
        calculate(positions.flatMap { (beacon, set) -> set.keys.map(beacon::move) }.toSortedSet())
    }

    /** 获得一个全联通子图的全部定位 */
    operator fun get(beacon: Beacon): Map<Beacon, Vector3D> {
        val lastTime = positions[beacon]?.lastKey() ?: return mapOf(beacon to vector3DOfZero())
        val position = beacon.move(lastTime)
        return relations[position]!!
            .toSortedSet()
            .apply { add(position) }
            .let(::calculate)
            .mapKeys { (key, _) -> key.beacon }
    }

    /** 获得一份深拷贝的坐标系 */
    fun copy() = positions.mapValues { (_, map) -> map.toSortedMap() }

    /** 获得每个标签最新位置列表 */
    fun newest() = positions.mapValues { (_, map) -> map[map.lastKey()]!! }

    private val lengthMemory =
        hashMapOf<Pair<Position, Position>, Expression>()

    /** 使用关心的部分关系更新坐标 */
    private fun calculate(targets: SortedSet<Position>)
        : Map<Position, Vector3D> {
        // 构造变量空间和损失函数
        val collector = ConditionCollector()
        val list = targets.toList()
        for (i in list.indices) for (j in i + 1 until list.size) {
            val a = list[i]
            val b = list[j]
            val pair = a to b
            val e = lengthMemory.computeIfAbsent(pair) { (a.toVector() - b.toVector()).length() }
            measures[pair]?.run {
                val l = average()
                collector += e - l
            } ?: if (a.isStatic() || b.isStatic()) {
                val l = (positions[a]!! euclid positions[b]!!)
                collector[maxMeasure - e] = maxMeasure - l
            }
        }
        val (error, domain, lambda) = collector.build()
        // 构造初始值
        val init = targets.flatMap {
                it.toVector()
                    .expressions
                    .values
                    .filterIsInstance<Variable>()
                    .zip(positions[it]!!.toList().map(::Constant))
            }.toMap().toMutableMap()
            .also { for ((v, value) in lambda) it[v] = Constant(value!!) }
            .let(::ExpressionVector)
        val space = VariableSpace(init.expressions.keys + lambda.keys)
        // 优化
        val f = fastestBatchGD(error.sum(), space, *domain)
        // val result = optimize(init, 500, 1e-4, f)
        val result = recurrence(init to .0) { (p, _) -> f(p) }
            .onEach { (p, _) ->
                targets.map { b ->
                    b.beacon to b.toVector().expressions.values
                        .toList()
                        .map { p[it as Variable]!!.toDouble() }
                        .to3D()
                }.toMap().let(painter)
            }
            .take(1000)
            .firstOrLast { (_, step) -> step < 2e-3 }
            .first
        return targets.associateWith { p ->
            p.toVector().expressions.values
                .toList()
                .map { result[it as Variable]!!.toDouble() }
                .to3D()
                .also { positions[p] = it }
        }
    }

    private companion object {
        fun <TK, TV> HashMap<TK, TV>.update(key: TK, block: (TV) -> Unit, default: () -> TV) =
            compute(key) { _, last -> last?.also(block) ?: default() }

        operator fun <T> HashMap<Beacon, SortedMap<Long, T>>.get(p: Position) =
            get(p.beacon)?.get(p.time)

        operator fun <T> HashMap<Beacon, SortedMap<Long, T>>.set(p: Position, t: T) =
            update(p.beacon, { it[p.time] = t }, { sortedMapOf(p.time to t) })

        fun <T> HashMap<Beacon, SortedMap<Long, T>>.update(p: Position, block: () -> T) =
            update(p.beacon, { it[p.time] = it[it.lastKey()] }, { sortedMapOf(p.time to block()) })

        fun List<Double>.to3D() = Vector3D(get(0), get(1), get(2))
    }
}
