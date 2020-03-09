package org.mechdancer.symbol.system

import org.mechdancer.algebra.implement.vector.Vector3D
import java.util.*
import kotlin.random.Random

class LocateSystemContext {
    /**
     * ∵ 移动 <=> 位置变化
     * ∴ 需要记录每个标签最后一次运动的时刻，把运动前后的位置视作两个无关的变量
     */

    // 某标签 -> 某时刻运动后 -> 计算出的位置
    private val beacons =
        hashMapOf<BeaconIndex, SortedMap<Long, Vector3D>>()

    // 某时刻运动后的某标签 和 某时刻运动后的某标签 -> 某时刻 -> 测得的距离
    private val measures =
        hashMapOf<Pair<BeaconData, BeaconData>, SortedSet<Stamped<Double>>>()

    /** 添加一条新的测距记录 */
    operator fun set(a: BeaconData, b: BeaconData, measure: Stamped<Double>) {
        require(a.data != b.data)
        update(a, b, measure)
        update(a)
        update(b)
    }

    // 更新测距数据
    private fun update(a: BeaconData, b: BeaconData, measure: Stamped<Double>) {
        measures.compute(if (a < b) a to b else b to a)
        { _, last ->
            (last ?: sortedSetOf()).apply { add(measure) }
        }
    }

    // 更新标签坐标
    private fun update(beacon: BeaconData) {
        val (t, i) = beacon
        beacons.compute(i) { _, last ->
            // 若标签之前出现过，检查是否发生新的移动，复制上一次计算的位置做为新的初始值
            last?.apply { lastKey().takeIf { it < t }?.let { this[t] = this[it]!! } }
            // 若标签第一次出现，初始化一个容器并放入一个随机的位置作为初始值
            ?: sortedMapOf(t to Vector3D(Random.nextDouble(), Random.nextDouble(), Random.nextDouble()))
        }
    }
}
