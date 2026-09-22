package com.test.healthbox_app.data.model.mapper

import com.test.healthbox_app.data.model.ParameterRanges
import com.test.healthbox_app.data.model.Parameters
import com.test.healthbox_app.data.model.response.Hba1cTestData

/** Same shape as BodyCheckupPref.toHba1cParameterList() - a single-row list, since an
 *  HbA1c record only ever carries the one reading. */
fun Hba1cTestData.toParametersList(): List<Parameters> = listOfNotNull(
    hba1c?.let {
        Parameters(
            parameterName = "HbA1c",
            value = it,
            result = hba1cResult,
            range = ParameterRanges.HBA1C["Normal"]
        )
    }
)
