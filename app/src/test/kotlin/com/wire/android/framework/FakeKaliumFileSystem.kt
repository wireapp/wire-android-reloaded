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

package com.wire.android.framework

import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.data.asset.AssetsStorageFolder
import com.wire.kalium.logic.data.asset.CacheFolder
import com.wire.kalium.logic.data.asset.DBFolder
import com.wire.kalium.logic.data.asset.DataStoragePaths
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.use

class FakeKaliumFileSystem(
    private val dispatcher: TestDispatcherProvider = TestDispatcherProvider()
) : KaliumFileSystem {

    private val userHomePath = "/Users/me/testApp".toPath()

    private val rootFileSystemPath = AssetsStorageFolder("$userHomePath/files")

    private val rootCacheSystemPath = CacheFolder("$userHomePath/cache")

    private val rootDBSystemPath = DBFolder("$userHomePath/database")

    private val fakeFileSystem = FakeFileSystem()

    private val dataStoragePaths = DataStoragePaths(rootFileSystemPath, rootCacheSystemPath, rootDBSystemPath)

    init {
        fakeFileSystem.allowDeletingOpenFiles = true
        fakeFileSystem.allowReadsWhileWriting = true
        fakeFileSystem.createDirectories(userHomePath)
        fakeFileSystem.createDirectory(dataStoragePaths.cachePath.value.toPath())
        fakeFileSystem.createDirectory(dataStoragePaths.assetStoragePath.value.toPath())
    }

    override val rootCachePath: Path = dataStoragePaths.cachePath.value.toPath()

    override val rootDBPath: Path = dataStoragePaths.dbPath.value.toPath()

    override fun sink(outputPath: Path, mustCreate: Boolean): Sink = fakeFileSystem.sink(outputPath, mustCreate)

    override fun source(inputPath: Path): Source = fakeFileSystem.source(inputPath)

    override fun createDirectories(dir: Path) = fakeFileSystem.createDirectories(dir)

    override fun createDirectory(dir: Path, mustCreate: Boolean) = fakeFileSystem.createDirectory(dir, mustCreate)

    override fun delete(path: Path, mustExist: Boolean) = fakeFileSystem.delete(path, mustExist)

    override fun deleteContents(dir: Path, mustExist: Boolean) = fakeFileSystem.deleteRecursively(dir, mustExist)

    override fun exists(path: Path): Boolean = fakeFileSystem.exists(path)

    override fun copy(sourcePath: Path, targetPath: Path) = fakeFileSystem.copy(sourcePath, targetPath)

    override fun tempFilePath(pathString: String?): Path {
        val filePath = pathString ?: "temp_file_path"
        return "$rootCachePath/$filePath".toPath()
    }

    override fun providePersistentAssetPath(assetName: String): Path = "${dataStoragePaths.assetStoragePath.value}/$assetName".toPath()

    override suspend fun readByteArray(inputPath: Path): ByteArray = source(inputPath).use {
        withContext(dispatcher.io()) {
            it.buffer().use { bufferedFileSource ->
                bufferedFileSource.readByteArray()
            }
        }
    }

    override suspend fun writeData(outputSink: Sink, dataSource: Source): Long {
        var byteCount = 0L
        withContext(dispatcher.io()) {
            outputSink.buffer().use { bufferedFileSink ->
                byteCount = bufferedFileSink.writeAll(dataSource)
            }
        }
        return byteCount
    }

    override fun selfUserAvatarPath(): Path = providePersistentAssetPath("self_user_avatar.jpg")

    override suspend fun listDirectories(dir: Path): List<Path> = fakeFileSystem.list(dir)
    override fun fileSize(path: Path): Long = fakeFileSystem.metadata(path).size ?: 0

}
