package io.github.cnaos.example.bledevicescan.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.cnaos.example.bledevicescan.databinding.MainFragmentBinding

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: MainFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        // ボタンを押した時の動作設定
        binding.button.setOnClickListener {
            if (viewModel.scanning.value!!) {
                viewModel.stopDeviceScan()
            } else {
                viewModel.startDeviceScan()
            }
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val adapter = RecyclerViewListAdapter(context!!)

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(context!!)
        val itemDecoration = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        viewModel.bleDeviceDataList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

    }

}
