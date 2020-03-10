package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.symbol.*
import org.mechdancer.symbol.core.Constant
import org.mechdancer.symbol.core.Expression
import org.mechdancer.symbol.core.Variable
import org.mechdancer.symbol.linear.ExpressionVector
import org.mechdancer.symbol.linear.VariableSpace
import org.mechdancer.symbol.optimize.fastestBatchGD
import org.mechdancer.symbol.optimize.optimize
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.random.Random.Default.nextDouble

class LocatingSystem {
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
//            .filterIndexed { i, p ->
//                relations[p]!!.containsAll(candidates.drop(i + 1))
//            }
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
        val error = information.map { (key, distance) ->
            val (a, b) = key
            targets += a
            targets += b
            val expression = lengthMemory.compute(key) { _, last ->
                last ?: (a.toVector() - b.toVector()).length()
            }!!
            (expression - distance `^` 2) / (2 * information.size)
        }
        // 构造初始值
        val init = targets.flatMap {
            val (i, t) = it
            it.toVector()
                .expressions
                .values
                .filterIsInstance<Variable>()
                .zip(positions[i, t]!!.toList().map(::Constant))
        }.toMap().let(::ExpressionVector)
        val space = VariableSpace(init.expressions.keys)
        // 优化
        val f = fastestBatchGD(error.sum(), space)
        val result = optimize(init, 1000, 1e-3, f)
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
