package com.example.dogalseslikitap.ui.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.dogalseslikitap.R
import com.example.dogalseslikitap.data.db.BookEntity
import com.example.dogalseslikitap.databinding.FragmentLibraryBinding
import com.example.dogalseslikitap.model.BookType
import kotlinx.coroutines.launch
import androidx.core.os.bundleOf

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LibraryViewModel by viewModels()
    private val adapter = BookAdapter { book ->
        findNavController().navigate(
            R.id.action_library_to_reader,
            bundleOf("bookId" to book.id)
        )
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerBooks.adapter = adapter
        binding.btnAdd.setOnClickListener {
            pickFileLauncher.launch(arrayOf("application/epub+zip", "application/pdf", "text/plain"))
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_library_to_settings)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.books.collect { list ->
                adapter.submitList(list)
                binding.txtEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun handleFile(uri: Uri) {
        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val title = queryDisplayName(uri) ?: uri.lastPathSegment ?: "Bilinmeyen"
        val type = BookType.fromPath(title).name
        val entity = BookEntity(title = title, path = uri.toString(), type = type)
        viewModel.addBook(entity)
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && it.moveToFirst()) {
                return it.getString(index)
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
