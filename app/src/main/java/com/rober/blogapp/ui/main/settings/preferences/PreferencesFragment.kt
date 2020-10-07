package com.rober.blogapp.ui.main.settings.preferences

import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import com.rober.blogapp.R
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.base.BasePreferenceFragment
import com.rober.blogapp.ui.main.settings.preferences.utils.Keys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_preferences.*
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject


@AndroidEntryPoint
class PreferencesFragment :
    BasePreferenceFragment<PreferenceState, PreferenceFragmentEvent, PreferenceViewModel>(R.xml.settings_preference) {

    override val viewModel: PreferenceViewModel by viewModels()

    @Inject
    lateinit var keys: Keys

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var sharedPreferencesEdit: SharedPreferences.Editor

    var materialToolbar: MaterialToolbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        /* Solution to add toolbar on preferences fragment!
        /  Loads custom layout with only a toolbar as a ViewGroup
        /  Then custom layout adds the actual view of preferences as a child
         */
        val defaultView = super.onCreateView(inflater, container, savedInstanceState)
        val customLayout = inflater.inflate(R.layout.fragment_preferences, container, false) as ViewGroup
        customLayout.addView(defaultView)
        return customLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        materialToolbar = view.findViewById<MaterialToolbar>(R.id.preferences_material_toolbar)

        materialToolbar?.apply {
            val backArrow = resources.getDrawable(R.drawable.ic_arrow_left, null)
            backArrow.colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(requireContext(), R.color.blueTwitter),
                PorterDuff.Mode.SRC_ATOP
            )

            navigationIcon = backArrow
        } ?: kotlin.run {
            Toast.makeText(
                requireContext(),
                "Failed to load preferences, try loading again the app",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }
    }

    override fun setupPreferenceListener() {
        val sharedPrefListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                when (key) {
                    keys.PREFERENCE_DARK_THEME -> {
                        viewModel.setIntention(
                            PreferenceFragmentEvent.TouchDarkThemeOption(
                                key,
                                sharedPreferences.getBoolean(key, true)
                            )
                        )
                    }
                }
            }
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefListener)

        materialToolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun render(viewState: PreferenceState) {
        when (viewState) {
            is PreferenceState.ConfigureTheme -> displayDarkTheme(viewState.value)
        }
    }

    private fun displayDarkTheme(display: Boolean) {
        if (display) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            sharedPreferencesEdit.putBoolean(keys.PREFERENCE_DARK_THEME, true)
            sharedPreferencesEdit.apply()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            sharedPreferencesEdit.putBoolean(keys.PREFERENCE_DARK_THEME, false)
            sharedPreferencesEdit.apply()
        }
    }

}

sealed class PreferenceFragmentEvent() {
    data class TouchDarkThemeOption(val key: String, val value: Boolean) : PreferenceFragmentEvent()
}