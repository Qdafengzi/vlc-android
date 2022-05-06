/*
 * ************************************************************************
 *  PreferencesWidgets.kt
 * *************************************************************************
 * Copyright © 2022 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.preferences.widgets

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.SeekBarPreference
import com.google.android.material.color.DynamicColors
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.vlc.R
import org.videolan.vlc.gui.preferences.BasePreferenceFragment
import org.videolan.vlc.repository.WidgetRepository
import org.videolan.vlc.widget.WidgetViewModel

const val WIDGET_ID = "WIDGET_ID"

class PreferencesWidgets : BasePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    internal lateinit var model: WidgetViewModel
    private lateinit var backgroundPreference: ColorPreferenceCompat
    private lateinit var foregroundPreference: ColorPreferenceCompat
    private lateinit var lightThemePreference: CheckBoxPreference

    override fun getXml() = R.xml.preferences_widgets

    override fun getTitleId() = R.string.widget_preferences

    override fun onStart() {
        super.onStart()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backgroundPreference = findPreference("background_color")!!
        foregroundPreference = findPreference("foreground_color")!!
        lightThemePreference = findPreference("widget_light_theme")!!
        val themePreference = findPreference<ListPreference>("widget_theme")!!

        val id = (arguments?.getInt(WIDGET_ID) ?: -2)
        if (id == -2) throw IllegalStateException("Invalid widget id")
        model = ViewModelProvider(this, WidgetViewModel.Factory(requireActivity(), id))[WidgetViewModel::class.java]
        model.widget.observe(requireActivity()) { widget ->
            if (widget == null) return@observe
            if (!DynamicColors.isDynamicColorAvailable() && widget.theme == 0) {
                widget.theme = 1
                updateWidgetEntity()
            }
            themePreference.value = widget.theme.toString()
            backgroundPreference.isVisible = widget.theme == 2
            foregroundPreference.isVisible = widget.theme == 2
            backgroundPreference.saveValue(widget.backgroundColor)
            foregroundPreference.saveValue(widget.foregroundColor)
            findPreference<SeekBarPreference>("opacity")?.value = widget.opacity
            lightThemePreference.isChecked = widget.lightTheme
            lightThemePreference.isVisible = widget.theme != 2
        }

        if (!DynamicColors.isDynamicColorAvailable()) {
            themePreference.entryValues = themePreference.entryValues.filter { it != "0"}.toTypedArray()
            themePreference.entries = themePreference.entries.filter { it != getString(R.string.material_you) }.toTypedArray()
        }


    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "opacity" -> {
                model.widget.value?.opacity = sharedPreferences.getInt(key, 100)
            }
            "background_color" -> {
                model.widget.value?.backgroundColor = sharedPreferences.getInt(key, ContextCompat.getColor(requireActivity(), R.color.black))
            }
            "foreground_color" -> {
                model.widget.value?.foregroundColor = sharedPreferences.getInt(key, ContextCompat.getColor(requireActivity(), R.color.white))
            }
            "widget_theme" -> {
                val newValue = sharedPreferences.getString(key, "0")?.toInt() ?: 0
                model.widget.value?.theme = newValue
                backgroundPreference.isVisible = newValue == 2
                foregroundPreference.isVisible = newValue == 2
                lightThemePreference.isVisible = newValue != 2

            }
            "widget_light_theme" -> {
                val newValue = sharedPreferences.getBoolean(key, true)
                model.widget.value?.lightTheme = newValue

            }
            "widget_forward_delay" -> {
                val newValue = sharedPreferences.getInt(key, 10)
                model.widget.value?.forwardDelay = newValue

            }
            "widget_rewind_delay" -> {
                val newValue = sharedPreferences.getInt(key, 10)
                model.widget.value?.rewindDelay = newValue

            }
            "widget_show_configure" -> {
                val newValue = sharedPreferences.getBoolean(key, false)
                model.widget.value?.showConfigure = newValue

            }
        }
        updateWidgetEntity()
    }

    private fun updateWidgetEntity() {
        model.widget.value?.let { widget ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { WidgetRepository.getInstance(requireActivity()).updateWidget(widget) }
            }
        }
    }

}