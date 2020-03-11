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
        hashMapOf<Pair<Position, Position>, SortedMap<Long, Double>>()

    operator fun set(a: Position, b: Position, t: Long, d: Double) {
        fun random(time: Long) = Vector3D(nextDouble(), nextDouble(), nextDouble())
        require(a != b)
        val u: Position
        val v: Position
        if (a < b) {
            u = a; v = b
        } else {
            u = b; v = a
        }
        positions.update(a.beacon, { it += a.time }, { sortedMapOf(a.time to random(a.time)) })
        positions.update(b.beacon, { it += b.time }, { sortedMapOf(b.time to random(b.time)) })
        relations.update(u, { it += v }, { sortedSetOf(v) })
        relations.update(v, { it += u }, { sortedSetOf(u) })
        measures.update(u to v, { it[t] = d }, { sortedMapOf(t to d) })
    }

    /** 使用所有已知的测量数据，优化所有坐标 */
    fun optimize() {
        calculate(measures.mapValues { (_, list) -> list.average() })
    }

    /** 获得一个全联通子图的全部定位 */
    operator fun get(beacon: Beacon): Map<Beacon, Vector3D> {
        val lastTime = positions[beacon]?.lastKey() ?: return mapOf(beacon to vector3DOfZero())
        val position = beacon.move(lastTime)
        val candidates = relations[position]!!
        return candidates
            // 最小全联通域
            // .filterIndexed { i, p ->
            //     relations[p]!!.containsAll(candidates.drop(i + 1))
            // }
            .toSortedSet()
            .apply { add(position) }
            .toList()
            .run {
                sequence {
                    for (i in indices) for (j in i + 1 until size)
                        yield(get(i) to get(j))
                }
            }
            .mapNotNull { pair -> measures[pair]?.average()?.let { pair to it } }
            .toMap()
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
    private fun calculate(information: Map<Pair<Position, Position>, Double>)
        : Map<Position, Vector3D> {
        // 构造变量空间和损失函数
        val targets = sortedSetOf<Position>()
        for ((key, _) in information) {
            val (a, b) = key
            targets += a
            targets += b
        }
        val collector = ConditionCollector()
        val list = targets.toList()
        for (i in list.indices) for (j in i + 1 until list.size) {
            val a = list[i]
            val b = list[j]
            val pair = a to b
            val e = lengthMemory.compute(pair) { _, last ->
                last ?: (a.toVector() - b.toVector()).length()
            }!!
            information[pair]?.let { d -> collector += e - d }
            ?: collector.domain(maxMeasure - e,
                                maxMeasure - (positions[a.beacon, a.time]!! euclid positions[b.beacon, b.time]!!))
        }
        val (error, domain, lambda) = collector.build()
        println("points: ${targets.size}")
        // 构造初始值
        val init = targets.flatMap {
                val (i, t) = it
                it.toVector()
                    .expressions
                    .values
                    .filterIsInstance<Variable>()
                    .zip(positions[i, t]!!.toList().map(::Constant))
            }.toMap().toMutableMap()
            .also { for ((v, value) in lambda) it[v] = Constant(value!!) }
            .let(::ExpressionVector)
        val space = VariableSpace(init.expressions.keys + lambda.keys)
        // 优化
        val f = fastestBatchGD(error.sum(), space, *domain)
//        val result = optimize(init, 500, 1e-4, f)
        val result = recurrence(init to .0) { (p, _) -> f(p) }
            .onEach { (p, s) ->
                targets.map { b ->
                    b.beacon to b.toVector().expressions.values
                        .toList()
                        .map { p[it as Variable]!!.toDouble() }
                        .to3D()
                }.toMap().let(painter)
            }
            .take(500)
//            .firstOrLast { (_, step) -> step < 1e-3 }
            .last()
            .first
        return targets.associateWith { p ->
            p.toVector().expressions.values
                .toList()
                .map { result[it as Variable]!!.toDouble() }
                .to3D()
                .also { positions[p.beacon, p.time] = it }
        }
    }

    private companion object {
        operator fun <T, U> SortedMap<T, U>.plusAssign(key: T) = set(key, get(lastKey()))
        fun SortedMap<Long, Double>.average() = values.sum() / size
        fun <TK, TV> HashMap<TK, TV>.update(
            key: TK,
            block: (TV) -> Unit,
            default: () -> TV
        ) = compute(key) { _, last ->
            last?.also(block) ?: default()
        }

        operator fun <T, U, V> HashMap<T, SortedMap<U, V>>.get(t: T, u: U) = get(t)?.get(u)
        operator fun <T, U, V> HashMap<T, SortedMap<U, V>>.set(t: T, u: U, v: V) = get(t)!!.set(u, v)
        fun List<Double>.to3D() = Vector3D(get(0), get(1), get(2))
    }
}
