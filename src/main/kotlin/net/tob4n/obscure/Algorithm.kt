package net.tob4n.obscure

import kotlin.math.abs
import org.bukkit.Location

class Algorithm {

    private data class Axis(var start: Int, val end: Int) {
        val diff = abs(end - start)
        val step = if (end > start) 1 else -1
        var error = diff - abs(end - start)
    }

    companion object {
        fun bresenham3D(start: Location, end: Location): List<Location> {
            val points = mutableListOf<Location>()
            val (xAxis, yAxis, zAxis) =
                listOf(
                    start.blockX to end.blockX,
                    start.blockY to end.blockY,
                    start.blockZ to end.blockZ
                )
                    .map { (startValue, endValue) -> Axis(startValue, endValue) }

            val maxStep = maxOf(xAxis.diff, yAxis.diff, zAxis.diff)

            repeat(maxStep) {
                points +=
                    Location(
                        start.world,
                        xAxis.start.toDouble(),
                        yAxis.start.toDouble(),
                        zAxis.start.toDouble()
                    )
                listOf(xAxis, yAxis, zAxis).forEach { axis ->
                    axis.error -= axis.diff
                    if (axis.error < 0) {
                        axis.start += axis.step
                        axis.error += maxStep
                    }
                }
            }

            return points
        }
    }
}