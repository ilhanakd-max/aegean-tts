package com.example.dogalseslikitap.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dogalseslikitap.R
import com.example.dogalseslikitap.databinding.FragmentSettingsBinding
import com.example.dogalseslikitap.tts.TtsManager
import com.example.dogalseslikitap.tts.TtsProvider
import com.example.dogalseslikitap.tts.TtsSettings
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var ttsManager: TtsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        ttsManager = TtsManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                renderSettings(settings)
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.save(buildSettingsFromUi())
            Toast.makeText(requireContext(), getString(R.string.saving), Toast.LENGTH_SHORT).show()
        }

        binding.btnTest.setOnClickListener {
            val settings = buildSettingsFromUi()
            ttsManager.speak(getString(R.string.sample_text), settings, {}, {
                Toast.makeText(requireContext(), getString(R.string.error_tts), Toast.LENGTH_LONG).show()
            })
        }
    }

    private fun renderSettings(settings: TtsSettings) {
        when (settings.provider) {
            TtsProvider.SYSTEM -> binding.groupProvider.check(R.id.radioSystem)
            TtsProvider.OPENAI -> binding.groupProvider.check(R.id.radioOpenAi)
            TtsProvider.AZURE -> binding.groupProvider.check(R.id.radioAzure)
        }
        binding.sliderSpeed.value = settings.speed
        binding.sliderPitch.value = settings.pitch
        binding.inputOpenAiKey.setText(settings.openAiKey)
        binding.inputOpenAiBase.setText(settings.openAiBase)
        binding.inputAzureKey.setText(settings.azureKey)
        binding.inputAzureRegion.setText(settings.azureRegion)
    }

    private fun buildSettingsFromUi(): TtsSettings {
        val provider = when (binding.groupProvider.checkedRadioButtonId) {
            R.id.radioOpenAi -> TtsProvider.OPENAI
            R.id.radioAzure -> TtsProvider.AZURE
            else -> TtsProvider.SYSTEM
        }
        return TtsSettings(
            provider = provider,
            speed = binding.sliderSpeed.value,
            pitch = binding.sliderPitch.value,
            voice = "", // Voice selection UI can be extended later.
            openAiKey = binding.inputOpenAiKey.text?.toString() ?: "",
            openAiBase = binding.inputOpenAiBase.text?.toString() ?: "https://api.openai.com/",
            azureKey = binding.inputAzureKey.text?.toString() ?: "",
            azureRegion = binding.inputAzureRegion.text?.toString()
                ?: "https://<region>.tts.speech.microsoft.com/"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
