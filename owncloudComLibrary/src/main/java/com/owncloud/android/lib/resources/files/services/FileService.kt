/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.resources.files.services

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.Service
import com.owncloud.android.lib.resources.files.RemoteFile

interface FileService : Service {
    fun getUrlToOpenInWeb(openWebEndpoint: String, fileId: String): RemoteOperationResult<String>

    fun checkPathExistence(
        path: String,
        isUserLogged: Boolean
    ): RemoteOperationResult<Boolean>

    fun copyFile(
        sourceRemotePath: String,
        targetRemotePath: String,
    ): RemoteOperationResult<String>

    fun createFolder(
        remotePath: String,
        createFullPath: Boolean,
        isChunkFolder: Boolean = false
    ): RemoteOperationResult<Unit>

    fun downloadFile(
        remotePath: String,
        localTempPath: String
    ): RemoteOperationResult<Unit>

    fun moveFile(
        sourceRemotePath: String,
        targetRemotePath: String,
    ): RemoteOperationResult<Unit>

    fun refreshFolder(
        remotePath: String
    ): RemoteOperationResult<ArrayList<RemoteFile>>

    fun removeFile(
        remotePath: String
    ): RemoteOperationResult<Unit>

    fun renameFile(
        oldName: String,
        oldRemotePath: String,
        newName: String,
        isFolder: Boolean,
    ): RemoteOperationResult<Unit>
}