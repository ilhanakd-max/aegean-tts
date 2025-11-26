package com.example.dogalseslikitap.ui.settings

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private var selectedEngine: String = TextToSpeech.Engine.DEFAULT_ENGINE
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.engines.collect {
                val rhvoice = viewModel.rhvoicePackage()
                if (selectedEngine.isBlank()) selectedEngine = TextToSpeech.Engine.DEFAULT_ENGINE
                if (selectedEngine == rhvoice) {
                    binding.groupEngine.check(R.id.btnEngineRhvoice)
                } else {
                    binding.groupEngine.check(R.id.btnEngineSystem)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.voices.collect { voices ->
                val labels = voices.map { it.label }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels.ifEmpty { listOf(getString(R.string.voice_none)) })
                binding.dropdownVoice.setAdapter(adapter)
                val currentIndex = voices.indexOfFirst { it.name == selectedVoice }
                if (currentIndex >= 0) {
                    binding.dropdownVoice.setText(labels[currentIndex], false)
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
                selectedEngine = settings.enginePackage
                selectedVoice = settings.voiceName
                renderSettings(settings)
                viewModel.refreshVoices(selectedEngine)
            }
        }

        binding.groupEngine.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnEngineRhvoice -> {
                    val rhvoice = viewModel.rhvoicePackage()
                    if (rhvoice == null) {
                        Toast.makeText(requireContext(), getString(R.string.engine_not_installed), Toast.LENGTH_LONG).show()
                        binding.groupEngine.check(R.id.btnEngineSystem)
                        selectedEngine = TextToSpeech.Engine.DEFAULT_ENGINE
                    } else {
                        selectedEngine = rhvoice
                    }
                }
                else -> selectedEngine = TextToSpeech.Engine.DEFAULT_ENGINE
            }
            viewModel.refreshVoices(selectedEngine)
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
                ttsManager.speak(
                    getString(R.string.sample_text),
                    settings,
                    onDone = {},
                    onError = {
                        Toast.makeText(requireContext(), getString(R.string.error_tts), Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun renderSettings(settings: TtsSettings) {
        binding.sliderSpeed.value = settings.speed
        binding.sliderPitch.value = settings.pitch
        selectedVoice = settings.voiceName
        selectedEngine = settings.enginePackage
        val rhvoice = viewModel.rhvoicePackage()
        if (settings.enginePackage == rhvoice && rhvoice != null) {
            binding.groupEngine.check(R.id.btnEngineRhvoice)
        } else {
            binding.groupEngine.check(R.id.btnEngineSystem)
        }
    }

    private fun buildSettingsFromUi(): TtsSettings =
        TtsSettings(
            enginePackage = selectedEngine,
            voiceName = selectedVoice,
            speed = binding.sliderSpeed.value,
            pitch = binding.sliderPitch.value
        )

    override fun onDestroyView() {
        super.onDestroyView()
        ttsManager.shutdown()
        _binding = null
    }
}
