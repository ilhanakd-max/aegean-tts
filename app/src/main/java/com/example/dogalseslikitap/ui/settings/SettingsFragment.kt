package com.example.dogalseslikitap.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dogalseslikitap.R
import com.example.dogalseslikitap.databinding.FragmentSettingsBinding
import com.example.dogalseslikitap.tts.TtsManager
import com.example.dogalseslikitap.tts.TtsSettings
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var ttsManager: TtsManager
    private var selectedVoice: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        ttsManager = TtsManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideLegacyEngineOptions()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.voices.collect { voices ->
                val labels = voices.map { it.label }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels.ifEmpty { listOf(getString(R.string.voice_none)) })
                binding.dropdownVoice.setAdapter(adapter)
                val currentIndex = voices.indexOfFirst { it.name == selectedVoice }
                if (currentIndex >= 0) {
                    binding.dropdownVoice.setText(labels[currentIndex], false)
                } else if (voices.isNotEmpty()) {
                    binding.dropdownVoice.setText(labels.first(), false)
                    selectedVoice = voices.first().name
                } else {
                    binding.dropdownVoice.setText("", false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadingVoices.collect { loading ->
                binding.dropdownVoice.isEnabled = !loading
                if (loading) {
                    binding.dropdownVoice.setText(getString(R.string.voice_loading), false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                renderSettings(settings)
            }
        }

        binding.dropdownVoice.setOnItemClickListener { _, _, position, _ ->
            val voices = viewModel.voices.value
            if (position in voices.indices) {
                selectedVoice = voices[position].name
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.save(buildSettingsFromUi())
            Toast.makeText(requireContext(), getString(R.string.saving), Toast.LENGTH_SHORT).show()
        }

        binding.btnTest.setOnClickListener {
            val settings = buildSettingsFromUi()
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.testSpeech(
                    text = getString(R.string.sample_text),
                    settings = settings,
                    onError = {
                        Toast.makeText(requireContext(), getString(R.string.error_tts), Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        viewModel.refreshVoices()
    }

    private fun renderSettings(settings: TtsSettings) {
        binding.sliderSpeed.value = settings.speed
        binding.sliderPitch.value = settings.pitch
        selectedVoice = settings.voiceName
    }

    private fun buildSettingsFromUi(): TtsSettings =
        TtsSettings(
            voiceName = selectedVoice,
            speed = binding.sliderSpeed.value,
            pitch = binding.sliderPitch.value
        )

    private fun hideLegacyEngineOptions() {
        binding.groupEngine.isVisible = false
        binding.btnEngineRhvoice.isVisible = false
        binding.btnEngineSystem.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.shutdown()
        _binding = null
    }
}
