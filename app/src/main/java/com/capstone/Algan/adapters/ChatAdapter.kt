package com.capstone.Algan.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.R
import com.capstone.Algan.models.Message

class ChatAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.tvMessageContent.text = message.content
        holder.tvMessageTimestamp.text = message.timestamp
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        val tvMessageTimestamp: TextView = itemView.findViewById(R.id.tvMessageTimestamp)
    }
}
