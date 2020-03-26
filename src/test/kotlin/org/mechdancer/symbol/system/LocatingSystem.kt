package org.mechdancer.symbol.system

import org.mechdancer.algebra.function.vector.euclid
import org.mechdancer.algebra.function.vector.times
import org.mechdancer.algebra.implement.vector.Vector3D
import org.mechdancer.algebra.implement.vector.to3D
import org.mechdancer.algebra.implement.vector.vector3DOfZero
import org.mechdancer.symbol.core.VariableSpace.Companion.variables
import org.mechdancer.symbol.minus
import org.mechdancer.symbol.optimize.*
import org.mechdancer.symbol.sum
import org.mechdancer.symbol.system.LocatingSystem.Companion.get
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.random.Random.Default.nextDouble

/** 由最大可测 [maxMeasure] 长度的标签组成的定位系统 */
class LocatingSystem(private val maxMeasure: Double) {
    // 画图子程序
    var painter: (Map<Beacon, Vector3D>) -> Unit = {}

    // 长度存储
    private val measures =
        hashMapOf<Pair<Position, Position>, MutableList<Double>>()

    // 坐标存储
    private val positions =
        hashMapOf<Beacon, SortedMap<Long, Vector3D>>()

    // 关系缓存，用于加速查询
    private val relationMemory =
        hashMapOf<Position, SortedSet<Position>>()

    /** 存储 [a] 到 [b] 在 [t] 时刻的测距 [l] */
    operator fun set(a: Position, b: Position, t: Long, l: Double) {
        // 对于一个新的位置点，复制同一个标签已知的最新坐标，或随机产生一个坐标
        fun Position.copyLastOrRandom() {
            positions.update(
                beacon,
                { map -> map.computeIfAbsent(time) { map[map.lastKey()]!! } },
                { sortedMapOf(time to Vector3D(nextDouble(), nextDouble(), nextDouble()) * 1e-4) })
        }

        require(a != b)
        a.copyLastOrRandom()
        b.copyLastOrRandom()
        measures.update(sortedPairOf(a, b), { it += l }, { mutableListOf(l) })
        relationMemory.update(a, { it += b }, { sortedSetOf(a, b) })
        relationMemory.update(b, { it += a }, { sortedSetOf(b, a) })
    }

    /** 存储 [t] 时刻的一组测距 [measures] */
    operator fun set(t: Long, measures: Map<Pair<Position, Position>, Double>) {
        for ((pair, l) in measures) {
            val (a, b) = pair
            this[a, b, t] = l
        }
    }

    /** 使用所有已知的测量数据，优化所有坐标 */
    fun optimize() {
        calculate(positions.flatMap { (beacon, set) -> set.keys.map(beacon::move) }.toSortedSet())
    }

    /** 优化某标签和与此标签直接测距的所有标签 */
    operator fun get(beacon: Beacon): Map<Beacon, Vector3D> {
        val p = positions[beacon]?.lastKey()?.let(beacon::move)
                ?: return mapOf(beacon to vector3DOfZero())
        return relationMemory[p]!!.let {
            positions[p] = calculate(p, it - p)
            calculate(it).mapKeys { (key, _) -> key.beacon }
        }
    }

    /** 获得一份深拷贝的完整位置点表 */
    fun copy() = positions.mapValues { (_, map) -> map.toSortedMap() }

    /** 获得每个标签最新位置列表 */
    fun newest() = positions.mapValues { (_, map) -> map[map.lastKey()]!! }

    // 使用关心的部分关系更新坐标
    private fun calculate(targets: SortedSet<Position>)
        : Map<Position, Vector3D> {
        // 收集优化条件
        val (errors, domain, init) = conditions {
            // 狭义上三角遍历
            val list = targets.toList()
            for (i in list.indices) for (j in i + 1 until list.size) {
                val a = list[i]
                val b = list[j]
                // 若两点间有测距数据，对测距取平均，添加到方程组
                measures[a to b]?.average()?.let { l -> this += (a euclid b) - l }
                // 否则若其中一个是固定标签，认为两个标签之间距离大于可测极限长度，添加到不等式约束
                ?: if (a.isStatic() || b.isStatic())
                    this[domain(maxMeasure - (a euclid b))] = maxMeasure - (positions[a]!! euclid positions[b]!!)
            }
            // 补充初始值
            for (target in targets)
                this[target.space] = positions[target]!!
        }
        // 确定未知数空间
        val space = variables(init.expressions.keys)
        // 构造优化步骤函数
        val f = when {
            domain.isEmpty() -> batchGD(errors.sum(), space, *domain, controller = NagMethod(space.dim, 1.0, .99))
            space.dim <= 20  -> dampingNewton(errors.sum(), space, *domain)
            else             -> fastestBatchGD(errors.sum(), space, *domain)
        }
        // 优化迭代
        val result = recurrence(init to .0) { (p, _) -> f(p) }
            .onEach { (p, _) ->
                targets.associate { b -> b.beacon to p.toVector(b.space).to3D() }.let(painter)
            }
            .take(1000)
            .firstOrLast { (_, step) -> step < 5e-4 }
            .first
        // 优化解整理为坐标
        return targets.associateWith { p ->
            result.toVector(p.space).to3D().also { positions[p] = it }
        }
    }

    // 锁定其他点，定位一个位置点
    private fun calculate(p: Position, beacons: Set<Position>): Vector3D {
        // 收集优化条件
        val (errors, domains, init) = conditions {
            for (b in beacons) this += (p euclid positions[b]!!) - measures[sortedPairOf(p, b)]!!.average()
            this[p.space] = positions[p]!!
        }
        // 构造优化步骤函数
        val f = dampingNewton(errors.sum(), variables(init.expressions.keys), *domains)
        val result = optimize(init, 1000, 2e-4, f)
        return result.toVector(p.space).to3D()
    }

    private companion object {
        // 修改哈希映射
        fun <TK, TV> HashMap<TK, TV>.update(key: TK, block: (TV) -> Unit, default: () -> TV) =
            compute(key) { _, last -> last?.also(block) ?: default() }

        // 查找二维表
        operator fun <T> Map<Beacon, Map<Long, T>>.get(p: Position) =
            get(p.beacon)?.get(p.time)

        // 修改二维表
        operator fun <T> HashMap<Beacon, SortedMap<Long, T>>.set(p: Position, t: T) =
            update(p.beacon, { it[p.time] = t }, { sortedMapOf(p.time to t) })
    }
}
