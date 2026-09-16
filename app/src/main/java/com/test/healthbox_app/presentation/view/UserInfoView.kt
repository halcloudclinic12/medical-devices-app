package com.test.healthbox_app.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.presentation.dashboard.DashboardViewModel
import com.test.healthbox_app.presentation.util.DatePickerUtil

class UserInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val avatarFrame: android.widget.FrameLayout
    private val ivUserIcon: ImageView
    private val tvPatientNameAge: TextView
    private val tvPatientGender: TextView

    /*private val binding: LayoutUserInfoBinding =
        LayoutUserInfoBinding.inflate(LayoutInflater.from(context), this, true)*/

    /*private val binding: LayoutUserInfoBinding
        get() = LayoutUserInfoBinding.bind(this)*/

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_user_info, this, true)
        avatarFrame = findViewById(R.id.avatar_frame)
        ivUserIcon = findViewById(R.id.iv_user_icon)
        tvPatientNameAge = findViewById(R.id.tv_patient_name_age)
        tvPatientGender = findViewById(R.id.tv_patient_gender)
    }

    fun bind(viewModel: DashboardViewModel) {
//        binding.viewModel = viewModel
//        binding.executePendingBindings()
    }

    fun loadPatientFromPref() {
        val patient = PatientPref.patient
        patient?.let {
            val age = DatePickerUtil.getAgeFromDob(patient.dateOfBirth.toString())
            val gender = patient.gender ?: ""

            tvPatientNameAge.text = "${patient.name} | $age Years"

            tvPatientGender.text = "$gender"

            // Case-insensitive: RegisterPatientViewModel.updatePatient() sends
            // gender.lowercase() to the server, so a patient re-fetched after an edit can come
            // back as "male"/"female" instead of the "Male"/"Female" the registration dropdown
            // originally wrote — an exact-match check here silently mis-shows the female icon
            // for a male patient in that case.
            if (gender.equals("Male", ignoreCase = true)) {
                ivUserIcon.setImageResource(R.drawable.male_icon)
                avatarFrame.setBackgroundResource(R.drawable.bg_avatar_ring_male)
            } else if (gender.equals("Female", ignoreCase = true)) {
                ivUserIcon.setImageResource(R.drawable.female_icon)
                avatarFrame.setBackgroundResource(R.drawable.bg_avatar_ring_female)
            }

        }
    }
}