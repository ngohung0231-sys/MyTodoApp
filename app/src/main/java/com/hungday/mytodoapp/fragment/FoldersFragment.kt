package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderGridAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import kotlinx.coroutines.launch

class FoldersFragment : Fragment(R.layout.fragment_folders) {
    // Database & Repository
    private lateinit var repository: TodoRepository
    
    // Adapters
    private lateinit var folderGridAdapter: FolderGridAdapter

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var rvFoldersGrid: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //------------------------------------ Init Database ------------------------------------//
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())
        //---------------------------------------------------------------------------------------//

        //------------------------------------ Init Views ------------------------------------//
        btnBack = view.findViewById(R.id.btnBack)
        rvFoldersGrid = view.findViewById(R.id.rvFoldersGrid)
        //------------------------------------------------------------------------------------//

        //------------------------------------ Setup Adapters ------------------------------------//
        folderGridAdapter = FolderGridAdapter(emptyList(), {
            // Click "New Folder"
            // TODO: Mở Dialog hoặc Fragment thêm folder
        }, { folder ->
            // Click vào Folder cụ thể
            // TODO: Xem chi tiết task trong folder này
        })

        rvFoldersGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        rvFoldersGrid.adapter = folderGridAdapter
        //----------------------------------------------------------------------------------------//

        //------------------------------------ Setup Data Loading (Room) ------------------------------------//
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allFolders.collect { folders ->
                folderGridAdapter.updateData(folders)
            }
        }
        //--------------------------------------------------------------------------------------------------//

        //------------------------------------ Setup Listeners ------------------------------------//
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        //-----------------------------------------------------------------------------------------//
    }
}
