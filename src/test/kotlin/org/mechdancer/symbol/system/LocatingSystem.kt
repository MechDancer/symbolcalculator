package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import java.util.*
import kotlin.random.Random

class LocatingSystem {
    private val positions =
        hashMapOf<Beacon, SortedMap<Long, Vector3D>>()

    private val relations =
        hashMapOf<Position, SortedSet<Position>>()

    private val measures =
        hashMapOf<Pair<Position, Position>, SortedMap<Long, Double>>()

    operator fun set(a: Position, b: Position, t: Long, d: Double) {
        require(a != b)
        val u: Position
        val v: Position
        if (a < b) {
            u = a; v = b
        } else {
            u = b; v = a
        }
        positions.update(a.beacon, { it += a.time }, { sortedMapOf(a.time to random3D) })
        positions.update(b.beacon, { it += b.time }, { sortedMapOf(b.time to random3D) })
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
            .filterIndexed { i, p ->
                relations[p]!!.containsAll(candidates.drop(i + 1))
            }
            .toSortedSet()
            .apply { add(position) }
            .toList()
            .run {
                sequence {
                    for (i in indices) for (j in i + 1 until size)
                        yield(get(i) to get(j))
                }
            }
            .associateWith { measures[it]!!.average() }
            .let(::calculate)
            .mapKeys { (key, _) -> key.beacon }
    }

    /** 获得一份深拷贝的坐标系 */
    fun copy() = positions.mapValues { (_, map) -> map.toSortedMap() }

    /** 使用关心的部分关系更新坐标 */
    private fun calculate(infomation: Map<Pair<Position, Position>, Double>)
        : Map<Position, Vector3D> = TODO()

    private companion object {
        operator fun <T, U> SortedMap<T, U>.plusAssign(key: T) = set(key, get(lastKey()))
        val random3D get() = Vector3D(Random.nextDouble(), Random.nextDouble(), Random.nextDouble())
        fun SortedMap<Long, Double>.average() = values.sum() / size
        fun <TK, TV> HashMap<TK, TV>.update(
            key: TK,
            block: (TV) -> Unit,
            default: () -> TV
        ) = compute(key) { _, last ->
            last?.also(block) ?: default()
        }
    }
}
