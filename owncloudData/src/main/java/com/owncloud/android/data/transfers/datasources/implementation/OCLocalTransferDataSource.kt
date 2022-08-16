/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2022 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.data.transfers.datasources.implementation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.owncloud.android.data.transfers.datasources.LocalTransferDataSource
import com.owncloud.android.data.transfers.db.OCTransferEntity
import com.owncloud.android.data.transfers.db.TransferDao
import com.owncloud.android.domain.transfers.model.OCTransfer
import com.owncloud.android.domain.transfers.model.TransferResult
import com.owncloud.android.domain.transfers.model.TransferStatus

class OCLocalTransferDataSource(
    private val transferDao: TransferDao
) : LocalTransferDataSource {
    override fun storeTransfer(transfer: OCTransfer): Long {
        return transferDao.insert(transfer.toEntity())
    }

    override fun updateTransfer(transfer: OCTransfer) {
        transferDao.insert(transfer.toEntity())
    }

    override fun updateTransferStatusToInProgressById(id: Long) {
        transferDao.updateTransferStatusWithId(id, TransferStatus.TRANSFER_IN_PROGRESS.value)
    }

    override fun updateTransferStatusToEnqueuedById(id: Long) {
        transferDao.updateTransferStatusWithId(id, TransferStatus.TRANSFER_QUEUED.value)
    }

    override fun updateTransferWhenFinished(
        id: Long,
        status: TransferStatus,
        transferEndTimestamp: Long,
        lastResult: TransferResult
    ) {
        transferDao.updateTransferWhenFinished(id, status.value, transferEndTimestamp, lastResult.value)
    }

    override fun removeTransferById(id: Long) {
        transferDao.deleteTransferWithId(id)
    }

    override fun removeAllTransfersFromAccount(accountName: String) {
        transferDao.deleteTransfersWithAccountName(accountName)
    }

    override fun getTransferById(id: Long): OCTransfer? {
        return transferDao.getTransferWithId(id)?.toModel()
    }

    override fun getAllTransfersAsLiveData(): LiveData<List<OCTransfer>> {
        return Transformations.map(transferDao.getAllTransfersAsLiveData()) { transferEntitiesList ->
            val transfers = transferEntitiesList.map { transferEntity ->
                transferEntity.toModel()
            }
            val transfersGroupedByStatus = transfers.groupBy { it.status }
            val transfersGroupedByStatusOrdered = Array<List<OCTransfer>>(4) { emptyList() }
            val newTransfersList = mutableListOf<OCTransfer>()
            transfersGroupedByStatus.forEach { transferMap ->
                val order = when (transferMap.key) {
                    TransferStatus.TRANSFER_IN_PROGRESS -> 0
                    TransferStatus.TRANSFER_QUEUED -> 1
                    TransferStatus.TRANSFER_FAILED -> 2
                    TransferStatus.TRANSFER_SUCCEEDED -> 3
                }
                transfersGroupedByStatusOrdered[order] = transferMap.value
            }
            for (items in transfersGroupedByStatusOrdered) {
                newTransfersList.addAll(items)
            }
            newTransfersList
        }
    }

    override fun getLastTransferFor(remotePath: String, accountName: String): OCTransfer? {
        return transferDao.getLastTransferWithRemotePathAndAccountName(remotePath, accountName)?.toModel()
    }

    override fun getCurrentAndPendingTransfers(): List<OCTransfer> {
        return transferDao.getTransfersWithStatus(
            listOf(TransferStatus.TRANSFER_IN_PROGRESS.value, TransferStatus.TRANSFER_QUEUED.value)
        ).map { it.toModel() }
    }

    override fun getFailedTransfers(): List<OCTransfer> {
        return transferDao.getTransfersWithStatus(
            listOf(TransferStatus.TRANSFER_FAILED.value)
        ).map { it.toModel() }
    }

    override fun getFinishedTransfers(): List<OCTransfer> {
        return transferDao.getTransfersWithStatus(
            listOf(TransferStatus.TRANSFER_SUCCEEDED.value)
        ).map { it.toModel() }
    }

    override fun clearFailedTransfers() {
        transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_FAILED.value)
    }

    override fun clearSuccessfulTransfers() {
        transferDao.deleteTransfersWithStatus(TransferStatus.TRANSFER_SUCCEEDED.value)
    }

    private fun OCTransferEntity.toModel() = OCTransfer(
        id = id,
        localPath = localPath,
        remotePath = remotePath,
        accountName = accountName,
        fileSize = fileSize,
        status = TransferStatus.fromValue(status),
        localBehaviour = localBehaviour,
        forceOverwrite = forceOverwrite,
        transferEndTimestamp = transferEndTimestamp,
        lastResult = lastResult?.let { TransferResult.fromValue(it) },
        createdBy = createdBy,
        transferId = transferId
    )

    private fun OCTransfer.toEntity() = OCTransferEntity(
        localPath = localPath,
        remotePath = remotePath,
        accountName = accountName,
        fileSize = fileSize,
        status = status.value,
        localBehaviour = localBehaviour,
        forceOverwrite = forceOverwrite,
        transferEndTimestamp = transferEndTimestamp,
        lastResult = lastResult?.value,
        createdBy = createdBy,
        transferId = transferId
    ).apply { this@toEntity.id?.let { this.id = it } }

}