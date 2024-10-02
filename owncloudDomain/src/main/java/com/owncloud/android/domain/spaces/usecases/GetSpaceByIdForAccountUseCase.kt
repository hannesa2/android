/**
 * ownCloud Android client application
 *
 * @author Aitor Ballesteros Pavón
 *
 * Copyright (C) 2023 ownCloud GmbH.
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
package com.owncloud.android.domain.spaces.usecases

import com.owncloud.android.domain.BaseUseCase
import com.owncloud.android.domain.spaces.SpacesRepository
import com.owncloud.android.domain.spaces.model.OCSpace

class GetSpaceByIdForAccountUseCase(
    private val spacesRepository: SpacesRepository
) : BaseUseCase<OCSpace?, GetSpaceByIdForAccountUseCase.Params>() {

    override fun run(params: Params) = spacesRepository.getSpaceByIdForAccount(
        spaceId = params.spaceId,
        accountName = params.accountName
    )

    data class Params(
        val accountName: String,
        val spaceId: String?,
    )
}
