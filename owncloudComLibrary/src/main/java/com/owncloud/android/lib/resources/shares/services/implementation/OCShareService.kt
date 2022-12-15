/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2022 ownCloud GmbH.
 *
 *   @author David González Verdugo
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

package com.owncloud.android.lib.resources.shares.services.implementation

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation
import com.owncloud.android.lib.resources.shares.RemoveRemoteShareOperation
import com.owncloud.android.lib.resources.shares.ShareResponse
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.UpdateRemoteShareOperation
import com.owncloud.android.lib.resources.shares.services.ShareService

class OCShareService(override val client: OwnCloudClient) : ShareService {
    override fun getShares(
        remoteFilePath: String,
        reshares: Boolean,
        subfiles: Boolean
    ): RemoteOperationResult<ShareResponse> = GetRemoteSharesForFileOperation(
        remoteFilePath,
        reshares,
        subfiles
    ).execute(client)

    override fun insertShare(
        remoteFilePath: String,
        shareType: ShareType,
        shareWith: String,
        permissions: Int,
        name: String,
        password: String,
        expirationDate: Long,
        publicUpload: Boolean
    ): RemoteOperationResult<ShareResponse> =
        CreateRemoteShareOperation(
            remoteFilePath,
            shareType,
            shareWith,
            permissions
        ).apply {
            this.name = name
            this.password = password
            this.expirationDateInMillis = expirationDate
            this.publicUpload = publicUpload
            this.retrieveShareDetails = true
        }.execute(client)

    override fun updateShare(
        remoteId: String,
        name: String,
        password: String?,
        expirationDate: Long,
        permissions: Int,
        publicUpload: Boolean
    ): RemoteOperationResult<ShareResponse> =
        UpdateRemoteShareOperation(
            remoteId
        ).apply {
            this.name = name
            this.password = password
            this.expirationDateInMillis = expirationDate
            this.permissions = permissions
            this.publicUpload = publicUpload
            this.retrieveShareDetails = true
        }.execute(client)

    override fun deleteShare(remoteId: String): RemoteOperationResult<Unit> =
        RemoveRemoteShareOperation(
            remoteId
        ).execute(client)
}
