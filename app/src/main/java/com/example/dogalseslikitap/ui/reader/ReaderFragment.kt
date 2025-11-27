package com.example.dogalseslikitap.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dogalseslikitap.R
import com.example.dogalseslikitap.databinding.FragmentReaderBinding
import kotlinx.coroutines.launch

class ReaderFragment : Fragment() {
    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bookId = arguments?.getLong("bookId") ?: 0L
        viewModel.loadBook(bookId)

        viewLifecycleOwner.lifecycleScope.launch {
            ttsManager.initialize(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookTitle.collect { binding.txtTitle.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.content.collect { binding.txtContent.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.progressText.collect { binding.txtProgress.text = it }
        }

        binding.btnPlay.setOnClickListener { speakCurrent(false) }
        binding.btnPause.setOnClickListener { viewModel.pauseSpeech() }
        binding.btnStop.setOnClickListener { viewModel.stopSpeech() }
        binding.btnNext.setOnClickListener {
            viewModel.nextSentence()
            speakCurrent(true)
        }
        binding.btnPrev.setOnClickListener {
            viewModel.previousSentence()
            speakCurrent(true)
        }
    }

    private fun speakCurrent(auto: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.speakCurrentSentence(
                autoAdvance = auto,
                onError = {
                    Toast.makeText(requireContext(), getString(R.string.error_tts), Toast.LENGTH_LONG).show()
                },
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
