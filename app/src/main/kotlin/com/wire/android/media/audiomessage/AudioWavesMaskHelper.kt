/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.media.audiomessage

import linc.com.amplituda.Amplituda
import linc.com.amplituda.Cache
import okio.Path
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

class AudioWavesMaskHelper @Inject constructor(
    private val amplituda: Amplituda
) {

    companion object {
        private const val WAVES_AMOUNT = 75
        private const val WAVE_MAX = 32
    }

    fun getWaveMask(decodedAssetPath: Path): List<Int> = getWaveMask(File(decodedAssetPath.toString()))

    fun getWaveMask(file: File): List<Int> = amplituda
        .processAudio(file, Cache.withParams(Cache.REUSE))
        .get()
        .amplitudesAsList()
        .averageWavesMask()
        .equalizeWavesMask()

    private fun List<Double>.equalizeWavesMask(): List<Int> {
        if (this.isEmpty()) return listOf()

        val divider = max() / (WAVE_MAX - 1)
        return map { (it / divider).roundToInt() + 1 }
    }

    private fun List<Int>.averageWavesMask(): List<Double> {
        val wavesSize = size
        val sectionSize = (wavesSize.toFloat() / WAVES_AMOUNT).roundToInt()

        if (wavesSize < WAVES_AMOUNT || sectionSize == 1) return map { it.toDouble() }

        val averagedWaves = mutableListOf<Double>()
        for (i in 0..<(wavesSize / sectionSize)) {
            val startIndex = (i * sectionSize)
            if (startIndex >= wavesSize) continue
            val endIndex = (startIndex + sectionSize).coerceAtMost(wavesSize - 1)
            averagedWaves.add(subList(startIndex, endIndex).averageInt())
        }
        return averagedWaves
    }

    private fun List<Int>.averageInt(): Double {
        if (isEmpty()) return 0.0
        return sum().toDouble() / size
    }

    fun clear() {
        amplituda.clearCache()
    }
}
