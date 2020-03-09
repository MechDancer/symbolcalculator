package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D
import java.util.*
import kotlin.random.Random

class LocatingSystem {
    private val positions =
        hashMapOf<Beacon, SortedMap<Long, Vector3D>>()

    private val relations =
        hashMapOf<Position, HashSet<Position>>()

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
        relations.update(u, { it += v }, { hashSetOf(v) })
        relations.update(v, { it += u }, { hashSetOf(u) })
        measures.update(u to v, { it[t] = d }, { sortedMapOf(t to d) })
    }

    /** 使用所有已知的测量数据，优化所有坐标 */
    fun optimize() {
        TODO()
    }

    /** 获得一个全联通子图的全部定位 */
    operator fun get(beacon: Beacon): Map<Beacon, Vector3D> {
        TODO()
    }

    /** 获得一份深拷贝的坐标系 */
    fun copy() = positions.mapValues { (_, map) -> map.toSortedMap() }

    companion object {
        private operator fun <T, U> SortedMap<T, U>.plusAssign(key: T) = set(key, get(lastKey()))
        private val random3D get() = Vector3D(Random.nextDouble(), Random.nextDouble(), Random.nextDouble())
        private fun <TK, TV> HashMap<TK, TV>.update(
            key: TK,
            block: (TV) -> Unit,
            default: () -> TV
        ) = compute(key) { _, last ->
            last?.also(block) ?: default()
        }
    }
}
