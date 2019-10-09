package io.github.cnaos.example.bledevicescan.ui.main

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.cnaos.example.bledevicescan.databinding.RecyclerviewItemBinding

class RecyclerViewListAdapter(
    context: Context
) : ListAdapter<BluetoothDevice, RecyclerViewListAdapter.BindingHolder>(ITEM_CALLBACK) {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    class BindingHolder(
        val binding: RecyclerviewItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    companion object {
        val ITEM_CALLBACK = object : DiffUtil.ItemCallback<BluetoothDevice>() {
            override fun areItemsTheSame(
                oldItem: BluetoothDevice,
                newItem: BluetoothDevice
            ): Boolean =
                oldItem.address == newItem.address

            override fun areContentsTheSame(
                oldItem: BluetoothDevice,
                newItem: BluetoothDevice
            ): Boolean = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder {
        val binding = RecyclerviewItemBinding.inflate(mInflater, parent, false)
        return BindingHolder(binding)
    }

    override fun onBindViewHolder(bindingHolder: BindingHolder, position: Int) {
        val current = getItem(position)
        bindingHolder.binding.bluetoothDevice = current
    }


}