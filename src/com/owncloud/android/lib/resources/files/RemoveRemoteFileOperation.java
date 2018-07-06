/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2018 ownCloud GmbH.
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

package com.owncloud.android.lib.resources.files;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.http.HttpConstants;
import com.owncloud.android.lib.common.http.methods.nonwebdav.DeleteMethod;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.net.URL;

import static com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode.OK;

/**
 * Remote operation performing the removal of a remote file or folder in the ownCloud server.
 *
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 */
public class RemoveRemoteFileOperation extends RemoteOperation {
    private static final String TAG = RemoveRemoteFileOperation.class.getSimpleName();
    private String mRemotePath;

    protected boolean removeChunksFolder = false;

    /**
     * Constructor
     *
     * @param remotePath RemotePath of the remote file or folder to remove from the server
     */
    public RemoveRemoteFileOperation(String remotePath) {
        mRemotePath = remotePath;
    }

    /**
     * Performs the rename operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;

        try {
            Uri srcWebDavUri = removeChunksFolder ? client.getNewUploadsWebDavUri() : client.getNewFilesWebDavUri();

            DeleteMethod deleteMethod = new DeleteMethod(
                    new URL(srcWebDavUri + WebdavUtils.encodePath(mRemotePath)));

            int status = client.executeHttpMethod(deleteMethod);

            result = isSuccess(status) ?
                    new RemoteOperationResult(OK) :
                    new RemoteOperationResult(deleteMethod);

            Log_OC.i(TAG, "Remove " + mRemotePath + ": " + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Remove " + mRemotePath + ": " + result.getLogMessage(), e);
        }

        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpConstants.HTTP_OK || status == HttpConstants.HTTP_NO_CONTENT;
    }
}